/*
 * Copyright (c) 2021-2022 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.alloc.admin;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequestUri;
import static uk.ac.manchester.spinnaker.alloc.db.Row.bool;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.error;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.uri;
import static uk.ac.manchester.spinnaker.alloc.web.SystemController.USER_MAY_CHANGE_PASSWORD;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.Machine;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;
import uk.ac.manchester.spinnaker.alloc.allocator.QuotaManager;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.model.BoardRecord;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.MachineTagging;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.web.Action;
import uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.ViewFactory;
import uk.ac.manchester.spinnaker.alloc.web.SystemController;

/**
 * Implements the logic supporting the JSP views and maps them into URL space.
 *
 * @author Donal Fellows
 */
@Controller("mvc.adminUI")
@PreAuthorize(IS_ADMIN)
public class AdminControllerImpl extends DatabaseAwareBean
		implements AdminController {
	private static final Logger log = getLogger(AdminControllerImpl.class);

	/** One board-hour in board-seconds. */
	private static final int BOARD_HOUR = 3600;

	// These are paths below src/main/webapp/WEB-INF/views
	private static final ViewFactory MAIN_VIEW = new ViewFactory("admin/index");

	private static final ViewFactory USER_LIST_VIEW =
			new ViewFactory("admin/listusers");

	private static final ViewFactory USER_DETAILS_VIEW =
			new ViewFactory("admin/userdetails");

	private static final ViewFactory CREATE_USER_VIEW =
			new ViewFactory("admin/createuser");

	private static final ViewFactory GROUP_LIST_VIEW =
			// FIXME define view
			new ViewFactory("admin/listgroups");

	private static final ViewFactory GROUP_DETAILS_VIEW =
			// FIXME define view
			new ViewFactory("admin/groupdetails");

	private static final ViewFactory BOARD_VIEW =
			new ViewFactory("admin/board");

	private static final ViewFactory MACHINE_VIEW =
			new ViewFactory("admin/machine");

	private static final String USER_LIST_OBJ = "userlist";

	private static final String USER_OBJ = "user";

	private static final String GROUP_LIST_OBJ = "grouplist";

	private static final String GROUP_OBJ = "group";

	private static final String BOARD_OBJ = "board";

	private static final String MACHINE_LIST_OBJ = "machineNames";

	private static final String DEFINED_MACHINES_OBJ = "definedMachines";

	private static final String MACHINE_TAGGING_OBJ = "machineTagging";

	private static final String DEFAULT_TAGGING_COUNT = "defaultCount";

	private static final String MACHINE_REPORTS_OBJ = "machineReports";

	private static final String BASE_URI = "baseuri";

	private static final String TRUST_LEVELS = "trustLevels";

	private static final String USERS_URI = "usersUri";

	private static final String CREATE_USER_URI = "createUserUri";

	private static final String GROUPS_URI = "groupsUri";

	private static final String BOARDS_URI = "boardsUri";

	private static final String MACHINE_URI = "machineUri";

	@Autowired
	private UserControl userController;

	@Autowired
	private MachineStateControl machineController;

	@Autowired
	private MachineDefinitionLoader machineDefiner;

	@Autowired
	private SpallocAPI spalloc;

	@Autowired
	private QuotaManager quotaManager;

	private Map<String, Boolean> getMachineNames(boolean allowOutOfService) {
		try (Connection conn = getConnection();
				Query listMachines = conn.query(LIST_MACHINE_NAMES)) {
			return conn.transaction(false,
					() -> listMachines.call(allowOutOfService)
							.toMap(string("machine_name"), bool("in_service")));
		} catch (DataAccessException e) {
			log.warn("problem when listing machines", e);
			return emptyMap();
		}
	}

	private static AdminController admin() {
		// Do not refactor to a constant; request-aware!
		return on(AdminController.class);
	}

	private static SystemController system() {
		// Do not refactor to a constant; request-aware!
		return on(SystemController.class);
	}

	/**
	 * All models should contain a common set of attributes that describe where
	 * the view is rendering and where other parts of the admin interface are.
	 *
	 * @param mav
	 *            The model-and-view.
	 * @param attrs
	 *            The redirect attributes, or {@code null} if this is not a
	 *            redirect.
	 * @return The enhanced model-and-view.
	 */
	private static ModelAndView addStandardContext(ModelAndView mav,
			RedirectAttributes attrs) {
		Authentication auth =
				SecurityContextHolder.getContext().getAuthentication();
		boolean mayChangePassword =
				auth instanceof UsernamePasswordAuthenticationToken;

		// Real implementation of these is always a ModelMap
		ModelMap model;
		if (attrs != null) {
			model = (ModelMap) attrs.getFlashAttributes();
		} else {
			model = (ModelMap) mav.getModel();
		}

		model.addAttribute(BASE_URI, fromCurrentRequestUri().toUriString());
		model.addAttribute(TRUST_LEVELS, TrustLevel.values());
		model.addAttribute(USERS_URI, uri(admin().listUsers()));
		model.addAttribute(CREATE_USER_URI, uri(admin().getUserCreationForm()));
		model.addAttribute(GROUPS_URI, uri(admin().listGroups()));
		model.addAttribute(BOARDS_URI, uri(admin().boards()));
		model.addAttribute(MACHINE_URI, uri(admin().machineManagement()));
		model.addAttribute(USER_MAY_CHANGE_PASSWORD, mayChangePassword);
		return mav;
	}

	/**
	 * All models should contain a common set of attributes that describe where
	 * the view is rendering and where other parts of the admin interface are.
	 *
	 * @param mav
	 *            The model-and-view.
	 * @param attrs
	 *            The redirect attributes, or {@code null} if this is not a
	 *            redirect.
	 * @return The enhanced model-and-view.
	 */
	private static ModelAndView addStandardContext(ModelAndView mav) {
		return addStandardContext(mav, null);
	}

	/**
	 * Construct a model-and-view which will redirect to a target URL. All
	 * models should contain a common set of attributes that describe where the
	 * view is rendering and where other parts of the admin interface are.
	 *
	 * @param uri
	 *            The URI to redirect to. Only the path is currently used.
	 * @return The model-and-view.
	 */
	private static ModelAndView redirectTo(URI uri, RedirectAttributes attrs) {
		return addStandardContext(new ModelAndView("redirect:" + uri.getPath()),
				attrs);
	}

	private static ModelAndView errors(String message) {
		return addStandardContext(error(message), null);
	}

	private static ModelAndView errors(DataAccessException exception) {
		return addStandardContext(error("database access failed: "
				+ exception.getMostSpecificCause().getMessage()), null);
	}

	@ExceptionHandler(BindException.class)
	ModelAndView validationError(BindingResult result) {
		if (result.hasGlobalErrors()) {
			return errors(result.getGlobalError().toString());
		}
		if (result.hasFieldErrors()) {
			return errors(result.getFieldError().toString());
		}
		return errors("unknown error");
	}

	/**
	 * Convert thrown exceptions from the DB layer to views so the rest of the
	 * code doesn't have to.
	 *
	 * @param e
	 *            A database access exception.
	 * @param hm
	 *            What method generated the problem? Used to look up what was
	 *            supposed to happen.
	 * @return The view to render.
	 */
	@ExceptionHandler(DataAccessException.class)
	ModelAndView dbException(DataAccessException e, HandlerMethod hm) {
		Action a = hm.getMethodAnnotation(Action.class);
		if (nonNull(a)) {
			log.warn("database access issue when {}", a.value(), e);
		} else {
			log.warn("database access issue", e);
		}
		return errors(e);
	}

	private static class AdminException extends RuntimeException {
		private static final long serialVersionUID = 8401068773689159840L;

		AdminException(String message) {
			super(message);
		}
	}

	/**
	 * Convert thrown admin issues to views so the rest of the code doesn't have
	 * to.
	 *
	 * @param e
	 *            An admin exception.
	 * @param hm
	 *            What method generated the problem? Used to look up what was
	 *            supposed to happen.
	 * @return The view to render.
	 */
	@ExceptionHandler(AdminException.class)
	ModelAndView adminException(AdminException e, HandlerMethod hm) {
		Action a = hm.getMethodAnnotation(Action.class);
		if (nonNull(a)) {
			log.warn("general issue when {}", a.value(), e);
		} else {
			log.warn("general issue", e);
		}
		return errors(e.getMessage());
	}

	@Override
	@Action("getting the main admin UI")
	public ModelAndView mainUI() {
		return addStandardContext(MAIN_VIEW.view());
	}

	@Override
	@Action("listing the users")
	public ModelAndView listUsers() {
		return addStandardContext(USER_LIST_VIEW.view(USER_LIST_OBJ,
				unmodifiableMap(userController.listUsers(
						user -> uri(admin().showUserForm(user.getUserId()))))));
	}

	@Override
	@Action("getting the user-creation UI")
	public ModelAndView getUserCreationForm() {
		return addStandardContext(
				CREATE_USER_VIEW.view(USER_OBJ, new UserRecord()));
	}

	@Override
	@Action("creating a user")
	public ModelAndView createUser(UserRecord user, ModelMap model,
			RedirectAttributes attrs) {
		user.initCreationDefaults();
		UserRecord realUser = userController.createUser(user)
				.orElseThrow(() -> new AdminException(
						"user creation failed (duplicate username?)"));
		int id = realUser.getUserId();
		log.info("created user ID={} username={}", id, realUser.getUserName());
		return redirectTo(uri(admin().showUserForm(id)), attrs);
	}

	@Override
	@Action("getting info about a user")
	public ModelAndView showUserForm(int id) {
		ModelAndView mav = USER_DETAILS_VIEW.view();
		UserRecord user = userController.getUser(id)
				.orElseThrow(() -> new AdminException("no such user"));
		mav.addObject(USER_OBJ, user.sanitise());
		assert mav.getModel().get(USER_OBJ) instanceof UserRecord;
		mav.addObject("deleteUri", uri(admin().deleteUser(id, null, null)));
		mav.addObject("addQuotaUri",
				uri(admin().adjustUserQuota(id, "", 0, null)));
		return addStandardContext(mav);
	}

	@Override
	@Action("updating a user's details")
	public ModelAndView submitUserForm(int id, UserRecord user, ModelMap model,
			Principal principal) {
		String adminUser = principal.getName();
		user.setUserId(null);
		log.info("updating user ID={}", id);
		UserRecord updatedUser = userController.updateUser(id, user, adminUser)
				.orElseThrow(() -> new AdminException("no such user"));
		ModelAndView mav = USER_DETAILS_VIEW.view(model);
		mav.addObject(USER_OBJ, updatedUser.sanitise());
		return addStandardContext(mav);
	}

	@Override
	@Action("deleting a user")
	public ModelAndView deleteUser(int id, Principal principal,
			RedirectAttributes attrs) {
		String adminUser = principal.getName();
		String deletedUsername =
				userController.deleteUser(id, adminUser).orElseThrow(
						() -> new AdminException("could not delete that user"));
		log.info("deleted user ID={} username={}", id, deletedUsername);
		// Not sure that these are the correct place
		ModelAndView mav = redirectTo(uri(admin().listUsers()), attrs);
		attrs.addFlashAttribute("notice", "deleted " + deletedUsername);
		attrs.addFlashAttribute(USER_OBJ, new UserRecord());
		return mav;
	}

	@Override
	@Action("adjusting a user's quota")
	public ModelAndView adjustUserQuota(int id, String machine, int delta,
			RedirectAttributes attrs) {
		// FIXME: this is now based on group, not on user; doesn't use machine
		if (quotaManager.addQuota(id, delta * BOARD_HOUR) > 0) {
			log.info("adjusted quota for user ID={} delta={}", id, delta);
		}
		return redirectTo(uri(admin().showUserForm(id)), attrs);
	}

	@Override
	@Action("listing the groups")
	public ModelAndView listGroups() {
		return addStandardContext(GROUP_LIST_VIEW.view(GROUP_LIST_OBJ,
				unmodifiableMap(userController.listGroups(group -> uri(
						admin().showGroupInfo(group.getGroupId()))))));
	}

	@Override
	@Action("getting info about a group")
	public ModelAndView showGroupInfo(int id) {
		// TODO add mapper for membership URLs?
		ModelAndView mav = GROUP_DETAILS_VIEW.view();
		mav.addObject(GROUP_OBJ, userController.getGroup(id, null)
				.orElseThrow(() -> new AdminException("no such group")));
		assert mav.getModel().get(GROUP_OBJ) instanceof GroupRecord;
		mav.addObject("deleteUri", uri(admin().deleteGroup(id, null)));
		mav.addObject("addQuotaUri",
				uri(admin().adjustGroupQuota(id, 0, null)));
		return addStandardContext(mav);
	}

	@Override
	@Action("adjusting a group's quota")
	public ModelAndView adjustGroupQuota(int id, int delta,
			RedirectAttributes attrs) {
		// TODO make this query return the group name for logging?
		if (quotaManager.addQuota(id, delta * BOARD_HOUR) > 0) {
			log.info("adjusted quota for group ID={} delta={}", id, delta);
		}
		return redirectTo(uri(admin().showGroupInfo(id)), attrs);
	}

	@Override
	@Action("deleting a group")
	public ModelAndView deleteGroup(int id, RedirectAttributes attrs) {
		// FIXME implement this
		ModelAndView mav = redirectTo(uri(admin().listGroups()), attrs);
		attrs.addFlashAttribute("notice", "not yet implemented");
		return mav;
	}

	@Override
	@Action("getting the UI for finding boards")
	public ModelAndView boards() {
		ModelAndView mav = BOARD_VIEW.view();
		mav.addObject(BOARD_OBJ, new BoardRecord());
		mav.addObject(MACHINE_LIST_OBJ, getMachineNames(true));
		return addStandardContext(mav);
	}

	private Optional<BoardState> getBoardState(BoardRecord board) {
		if (nonNull(board.getId())) {
			return machineController.findId(board.getId());
		} else if (board.isTriadCoordPresent()) {
			return machineController.findTriad(board.getMachineName(),
					board.getX(), board.getY(), board.getZ());
		} else if (board.isPhysicalCoordPresent()) {
			return machineController.findPhysical(board.getMachineName(),
					board.getCabinet(), board.getFrame(), board.getBoard());
		} else if (board.isAddressPresent()) {
			return machineController.findIP(board.getMachineName(),
					board.getIpAddress());
		} else {
			// unreachable because of validation
			return Optional.empty();
		}
	}

	// TODO should we refactor into multiple methods?
	@Override
	@Action("processing changes to a board's configuration")
	public ModelAndView board(BoardRecord board, ModelMap model) {
		ModelAndView mav = BOARD_VIEW.view(model);
		BoardState bs = getBoardState(board)
				.orElseThrow(() -> new AdminException("no such board"));

		// Inflate the coordinates
		board.setId(bs.id);
		board.setX(bs.x);
		board.setY(bs.y);
		board.setZ(bs.z);
		board.setCabinet(bs.cabinet);
		board.setFrame(bs.frame);
		board.setBoard(bs.board);
		board.setIpAddress(bs.address);
		board.setMachineName(bs.machineName);

		// Inflate the other properties
		board.setPowered(bs.getPower());
		board.setLastPowerOff(bs.getPowerOffTime().orElse(null));
		board.setLastPowerOn(bs.getPowerOnTime().orElse(null));
		board.setJobId(bs.getAllocatedJob().orElse(null));
		board.setReports(bs.getReports());

		// Get or set
		if (!board.isEnabledDefined()) {
			board.setEnabled(bs.getState());
		} else {
			log.info(
					"setting board-allocatable state for board "
							+ "({},{},{}) to {}",
					bs.x, bs.y, bs.z, board.isEnabled());
			bs.setState(board.isEnabled());
			spalloc.purgeDownCache();
		}
		model.put(BOARD_OBJ, bs);
		model.put(MACHINE_LIST_OBJ, getMachineNames(true));
		return addStandardContext(mav);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Implementation note:</strong> This is the baseline information
	 * that {@code admin/machine.jsp} needs.
	 */
	@Override
	@Action("getting a machine's configuration")
	public ModelAndView machineManagement() {
		ModelAndView mav =
				MACHINE_VIEW.view(MACHINE_LIST_OBJ, getMachineNames(true));
		List<MachineTagging> tagging = machineController.getMachineTagging();
		tagging.forEach(
				t -> t.setUrl(uri(system().getMachineInfo(t.getName()))));
		mav.addObject(MACHINE_TAGGING_OBJ, tagging);
		mav.addObject(DEFAULT_TAGGING_COUNT, tagging.stream()
				.filter(MachineTagging::isTaggedAsDefault).count());
		mav.addObject(MACHINE_REPORTS_OBJ,
				machineController.getMachineReports());
		return addStandardContext(mav);
	}

	@Override
	@Action("retagging a machine")
	public ModelAndView retagMachine(String machineName, String newTags) {
		Set<String> tags =
				stream(newTags.split(",")).map(String::trim).collect(toSet());
		machineController.updateTags(machineName, tags);
		log.info("retagged {} to have tags {}", machineName, tags);
		return machineManagement();
	}

	@Override
	@Action("disabling a machine")
	public ModelAndView disableMachine(String machineName) {
		machineController.setMachineState(machineName, false);
		log.info("marked {} as out of service", machineName);
		return machineManagement();
	}

	@Override
	@Action("enabling a machine")
	public ModelAndView enableMachine(String machineName) {
		machineController.setMachineState(machineName, true);
		log.info("marked {} as in service", machineName);
		return machineManagement();
	}

	@Override
	@Action("defining a machine")
	public ModelAndView defineMachine(MultipartFile file) {
		List<Machine> machines = extractMachineDefinitions(file);
		for (Machine m : machines) {
			machineDefiner.loadMachineDefinition(m);
			log.info("defined machine {}", m.getName());
		}
		ModelAndView mav = machineManagement();
		// Tailor with extra objects here
		mav.addObject(DEFINED_MACHINES_OBJ, machines);
		return mav;
	}

	private List<Machine> extractMachineDefinitions(MultipartFile file) {
		try (InputStream input = file.getInputStream()) {
			return machineDefiner.readMachineDefinitions(input);
		} catch (IOException e) {
			throw new AdminException(
					"problem with processing file: " + e.getMessage());
		}
	}
}
