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

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toSet;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequestUri;
import static uk.ac.manchester.spinnaker.alloc.db.Row.bool;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;
import static uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType.COLLABRATORY;
import static uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType.INTERNAL;
import static uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType.ORGANISATION;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.error;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.errorMessage;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.uri;
import static uk.ac.manchester.spinnaker.alloc.web.SystemController.USER_MAY_CHANGE_PASSWORD;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import uk.ac.manchester.spinnaker.alloc.admin.AdminController.BlacklistData;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.Machine;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;
import uk.ac.manchester.spinnaker.alloc.allocator.QuotaManager;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.bmp.BlacklistIO;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.model.BoardIssueReport;
import uk.ac.manchester.spinnaker.alloc.model.BoardRecord;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType;
import uk.ac.manchester.spinnaker.alloc.model.MachineTagging;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.web.Action;
import uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.ViewFactory;
import uk.ac.manchester.spinnaker.alloc.web.SystemController;
import uk.ac.manchester.spinnaker.messages.bmp.Blacklist;

/**
 * Implements the logic supporting the JSP views and maps them into URL space.
 *
 * @author Donal Fellows
 */
@Controller("mvc.adminUI")
@PreAuthorize(IS_ADMIN)
public class AdminControllerImpl extends DatabaseAwareBean
		implements AdminController, AdminControllerConstants {
	private static final Logger log = getLogger(AdminControllerImpl.class);

	/** One board-hour in board-seconds. */
	private static final int BOARD_HOUR = 3600;

	@Autowired
	private UserControl userManager;

	@Autowired
	private MachineStateControl machineController;

	@Autowired
	private MachineDefinitionLoader machineDefiner;

	@Autowired
	private SpallocAPI spalloc;

	@Autowired
	private QuotaManager quotaManager;

	@Autowired
	private BlacklistIO blio;

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
		Authentication auth = getContext().getAuthentication();
		boolean mayChangePassword =
				auth instanceof UsernamePasswordAuthenticationToken;

		// Real implementation of these is always a ModelMap
		ModelMap model = (ModelMap) (nonNull(attrs) ? attrs.getFlashAttributes()
				: mav.getModel());

		model.addAttribute(BASE_URI, fromCurrentRequestUri().toUriString());
		model.addAttribute(TRUST_LEVELS, TrustLevel.values());
		model.addAttribute(USERS_URI, uri(admin().listUsers()));
		model.addAttribute(CREATE_USER_URI, uri(admin().getUserCreationForm()));
		model.addAttribute(CREATE_GROUP_URI,
				uri(admin().getGroupCreationForm()));
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
	ModelAndView validationError(BindException result) {
		if (result.hasGlobalErrors()) {
			// I don't believe this is really reachable code
			log.debug("binding problem", result);
			return errors(errorMessage(result.getGlobalError()));
		} else if (result.hasFieldErrors()) {
			log.debug("binding problem", result);
			return errors(errorMessage(result.getFieldError()));
		} else {
			// This should definitely be unreachable
			log.error("unknown binding error", result);
			return errors("unknown error");
		}
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

	private static final class NoUser extends AdminException {
		private static final long serialVersionUID = 6430674580385445089L;

		private NoUser() {
			super("no such user");
		}
	}

	private static final class NoGroup extends AdminException {
		private static final long serialVersionUID = -4593707687103047377L;

		private NoGroup() {
			super("no such group");
		}
	}

	private static final class NoBoard extends AdminException {
		private static final long serialVersionUID = -4017368969526085002L;

		private NoBoard() {
			super("no such board");
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
		ModelAndView mav = USER_LIST_VIEW.view();
		// Share this function
		Function<UserRecord, URI> urlMaker =
				user -> uri(admin().showUserForm(user.getUserId()));
		mav.addObject(LOCAL_USER_LIST_OBJ,
				unmodifiableMap(userManager.listUsers(true, urlMaker)));
		mav.addObject(OPENID_USER_LIST_OBJ,
				unmodifiableMap(userManager.listUsers(false, urlMaker)));
		return addStandardContext(mav);
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
		UserRecord realUser = userManager.createUser(user)
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
		UserRecord user = userManager
				.getUser(id, m -> uri(admin().showGroupInfo(m.getGroupId())))
				.orElseThrow(NoUser::new);
		mav.addObject(USER_OBJ, user.sanitise());
		assert mav.getModel().get(USER_OBJ) instanceof UserRecord;
		mav.addObject("deleteUri", uri(admin().deleteUser(id, null, null)));
		return addStandardContext(mav);
	}

	@Override
	@Action("updating a user's details")
	public ModelAndView submitUserForm(int id, UserRecord user, ModelMap model,
			Principal principal) {
		String adminUser = principal.getName();
		user.setUserId(null);
		log.info("updating user ID={}", id);
		UserRecord updatedUser = userManager
				.updateUser(id, user, adminUser,
						m -> uri(admin().showGroupInfo(m.getGroupId())))
				.orElseThrow(NoUser::new);
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
				userManager.deleteUser(id, adminUser).orElseThrow(
						() -> new AdminException("could not delete that user"));
		log.info("deleted user ID={} username={}", id, deletedUsername);
		// Not sure that these are the correct place
		ModelAndView mav = redirectTo(uri(admin().listUsers()), attrs);
		attrs.addFlashAttribute("notice", "deleted " + deletedUsername);
		attrs.addFlashAttribute(USER_OBJ, new UserRecord());
		return mav;
	}

	@Override
	@Action("listing the groups")
	public ModelAndView listGroups() {
		ModelAndView mav = GROUP_LIST_VIEW.view();
		// Share this function
		Function<GroupRecord, URI> urlMaker =
				group -> uri(admin().showGroupInfo(group.getGroupId()));
		mav.addObject(LOCAL_GROUP_LIST_OBJ,
				unmodifiableMap(userManager.listGroups(INTERNAL, urlMaker)));
		mav.addObject(ORG_GROUP_LIST_OBJ, unmodifiableMap(
				userManager.listGroups(ORGANISATION, urlMaker)));
		mav.addObject(COLLAB_GROUP_LIST_OBJ, unmodifiableMap(
				userManager.listGroups(COLLABRATORY, urlMaker)));
		return addStandardContext(mav);
	}

	@Override
	@Action("getting info about a group")
	public ModelAndView showGroupInfo(int id) {
		ModelAndView mav = GROUP_DETAILS_VIEW.view();
		Map<String, URI> userLocations = new HashMap<>();
		mav.addObject(GROUP_OBJ, userManager.getGroup(id, m -> {
			userLocations.put(m.getUserName(),
					uri(admin().showUserForm(m.getUserId())));
			return uri(admin().removeUserFromGroup(id, m.getUserId(), null));
		}).orElseThrow(NoGroup::new));
		assert mav.getModel().get(GROUP_OBJ) instanceof GroupRecord;
		mav.addObject(USER_LIST_OBJ, userLocations);
		mav.addObject("deleteUri", uri(admin().deleteGroup(id, null)));
		mav.addObject("addUserUri",
				uri(admin().addUserToGroup(id, null, null)));
		mav.addObject("addQuotaUri",
				uri(admin().adjustGroupQuota(id, 0, null)));
		return addStandardContext(mav);
	}

	@Override
	@Action("getting the group-creation UI")
	public ModelAndView getGroupCreationForm() {
		return addStandardContext(
				CREATE_GROUP_VIEW.view(GROUP_OBJ, new CreateGroupModel()));
	}

	@Override
	@Action("creating a group")
	public ModelAndView createGroup(CreateGroupModel groupRequest,
			RedirectAttributes attrs) {
		GroupRecord realGroup =
				userManager.createGroup(groupRequest.toGroupRecord(), INTERNAL)
						.orElseThrow(() -> new AdminException(
								"group creation failed (duplicate name?)"));
		int id = realGroup.getGroupId();
		log.info("created group ID={} name={}", id, realGroup.getGroupName());
		return redirectTo(uri(admin().showGroupInfo(id)), attrs);
	}

	@Override
	@Action("adding a user to a group")
	public ModelAndView addUserToGroup(int id, String user,
			RedirectAttributes attrs) {
		GroupRecord g =
				userManager.getGroup(id, null).orElseThrow(NoGroup::new);
		UserRecord u = userManager.getUser(user, null).orElseThrow(NoUser::new);
		String notice;
		if (userManager.addUserToGroup(u, g).isPresent()) {
			log.info("added user {} to group {}", u.getUserName(),
					g.getGroupName());
			notice = format("added user %s to group %s", u.getUserName(),
					g.getGroupName());
		} else {
			notice = format("user %s is already a member of group %s",
					u.getUserName(), g.getGroupName());
		}
		attrs.addFlashAttribute("notice", notice);
		return redirectTo(uri(admin().showGroupInfo(id)), attrs);
	}

	@Override
	@Action("removing a user from a group")
	public ModelAndView removeUserFromGroup(int id, int userid,
			RedirectAttributes attrs) {
		GroupRecord g =
				userManager.getGroup(id, null).orElseThrow(NoGroup::new);
		UserRecord u =
				userManager.getUser(userid, null).orElseThrow(NoUser::new);
		String notice;
		if (userManager.removeUserFromGroup(u, g)) {
			log.info("removed user {} from group {}", u.getUserName(),
					g.getGroupName());
			notice = format("removed user %s from group %s", u.getUserName(),
					g.getGroupName());
		} else {
			notice = format("user %s is already not a member of group %s",
					u.getUserName(), g.getGroupName());
		}
		attrs.addFlashAttribute("notice", notice);
		return redirectTo(uri(admin().showGroupInfo(id)), attrs);
	}

	@Override
	@Action("adjusting a group's quota")
	public ModelAndView adjustGroupQuota(int id, int delta,
			RedirectAttributes attrs) {
		quotaManager.addQuota(id, delta * BOARD_HOUR).ifPresent(aq -> {
			log.info("adjusted quota for group {} to {}", aq.getName(),
					aq.getQuota());
			// attrs.addFlashAttribute("notice", "quota updated");
		});
		return redirectTo(uri(admin().showGroupInfo(id)), attrs);
	}

	@Override
	@Action("deleting a group")
	public ModelAndView deleteGroup(int id, RedirectAttributes attrs) {
		String deletedGroupName = userManager.deleteGroup(id)
				.orElseThrow(NoGroup::new);
		log.info("deleted group ID={} groupname={}", id, deletedGroupName);
		ModelAndView mav = redirectTo(uri(admin().listGroups()), attrs);
		attrs.addFlashAttribute("notice", "deleted " + deletedGroupName);
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
		BoardState bs = getBoardState(board).orElseThrow(NoBoard::new);

		inflateBoardRecord(board, bs);

		if (board.isEnabledDefined()) {
			// We're doing a set
			log.info("setting board-allocatable state for board {} to {}", bs,
					board.isEnabled());
			bs.setState(board.isEnabled());
			spalloc.purgeDownCache();
		}

		board.setEnabled(bs.getState());
		addBlacklistData(bs, model);
		model.put(BOARD_OBJ, bs); // TODO is this right?
		model.put(MACHINE_LIST_OBJ, getMachineNames(true));
		return addStandardContext(mav);
	}

	@Override
	@Action("processing changes to a blacklist")
	public ModelAndView blacklistHandling(BlacklistData bldata,
			ModelMap model) {
		ModelAndView mav = BOARD_VIEW.view(model);
		BoardState bs = readAndRememberBoardState(model);

		// FIXME add in blacklist manipulators here
		addBlacklistData(bs, model);

		model.put(MACHINE_LIST_OBJ, getMachineNames(true));
		return addStandardContext(mav);
	}

	@Override
	@Action("fetching a live blacklist from the machine")
	public CompletableFuture<ModelAndView> blacklistFetch(BlacklistData bldata,
			ModelMap model) {
		ModelAndView mav = BOARD_VIEW.view(model);
		BoardState bs = readAndRememberBoardState(model);

		log.info("pulling blacklist from board {}", bs);
		machineController.pullBlacklist(bs);
		addBlacklistData(bs, model);

		model.put(MACHINE_LIST_OBJ, getMachineNames(true));
		return completedFuture(addStandardContext(mav));
	}

	@Override
	@Action("pushing a blacklist to the machine")
	public CompletableFuture<ModelAndView> blacklistPush(BlacklistData bldata,
			ModelMap model) {
		ModelAndView mav = BOARD_VIEW.view(model);
		BoardState bs = readAndRememberBoardState(model);

		log.info("pushing blacklist to board {}", bs);
		machineController.pushBlacklist(bs);
		addBlacklistData(bs, model);

		model.put(MACHINE_LIST_OBJ, getMachineNames(true));
		return completedFuture(addStandardContext(mav));
	}

	private BoardState readAndRememberBoardState(ModelMap model) {
		BoardRecord board = (BoardRecord) model.get(BOARD_OBJ);
		BoardState bs = getBoardState(board).orElseThrow(NoBoard::new);
		inflateBoardRecord(board, bs);
		board.setEnabled(bs.getState());
		// Replace the state in the model with the current values
		model.put(BOARD_OBJ, bs);
		return bs;
	}

	private void addBlacklistData(BoardState board, ModelMap model) {
		BlacklistData bldata = new BlacklistData();
		bldata.setId(board.id);
		Optional<Blacklist> bl = machineController.readBlacklistFromDB(board);
		bldata.setPresent(bl.isPresent());
		bl.map(blio::toString).ifPresent(bldata::setBlacklist);

		model.addAttribute(BLACKLIST_URI,
				uri(admin().blacklistHandling(null, model)));
		model.addAttribute(BLACKLIST_DATA_OBJ, bldata);
	}

	/**
	 * Copy settings from the record out of the DB.
	 *
	 * @param board
	 *            Where the values are copied to. A partial state.
	 * @param bs
	 *            Where the values are copied from. A complete state from the
	 *            DB.
	 */
	private void inflateBoardRecord(BoardRecord board, BoardState bs) {
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

	@SuppressWarnings("unused")
	private abstract static class Use {
		Use(GroupType t, BoardIssueReport b) {
		}
	}
}

interface AdminControllerConstants {
	// These are paths below src/main/webapp/WEB-INF/views
	/** View: {@code admin/index.jsp}. */
	ViewFactory MAIN_VIEW = new ViewFactory("admin/index");

	/** View: {@code admin/listusers.jsp}. */
	ViewFactory USER_LIST_VIEW = new ViewFactory("admin/listusers");

	/** View: {@code admin/userdetails.jsp}. */
	ViewFactory USER_DETAILS_VIEW = new ViewFactory("admin/userdetails");

	/** View: {@code admin/createuser.jsp}. */
	ViewFactory CREATE_USER_VIEW = new ViewFactory("admin/createuser");

	/** View: {@code admin/listgroups.jsp}. */
	ViewFactory GROUP_LIST_VIEW = new ViewFactory("admin/listgroups");

	/** View: {@code admin/groupdetails.jsp}. */
	ViewFactory GROUP_DETAILS_VIEW = new ViewFactory("admin/groupdetails");

	/** View: {@code admin/creategroup.jsp}. */
	ViewFactory CREATE_GROUP_VIEW = new ViewFactory("admin/creategroup");

	/** View: {@code admin/board.jsp}. */
	ViewFactory BOARD_VIEW = new ViewFactory("admin/board");

	/** View: {@code admin/machine.jsp}. */
	ViewFactory MACHINE_VIEW = new ViewFactory("admin/machine");

	/** User list in {@link #USER_LIST_VIEW}. All users. */
	String USER_LIST_OBJ = "userlist";

	/** User list in {@link #USER_LIST_VIEW}. Local users only. */
	String LOCAL_USER_LIST_OBJ = "localusers";

	/** User list in {@link #USER_LIST_VIEW}. OpenID users only. */
	String OPENID_USER_LIST_OBJ = "openidusers";

	/** User details in {@link #USER_DETAILS_VIEW}. */
	String USER_OBJ = "user";

	/**
	 * Group list (type: {@link GroupType#INTERNAL}) in
	 * {@link #GROUP_LIST_VIEW}.
	 */
	String LOCAL_GROUP_LIST_OBJ = "localgroups";

	/**
	 * Group list (type: {@link GroupType#ORGANISATION}) in
	 * {@link #GROUP_LIST_VIEW}.
	 */
	String ORG_GROUP_LIST_OBJ = "orggroups";

	/**
	 * Group list (type: {@link GroupType#COLLABRATORY}) in
	 * {@link #GROUP_LIST_VIEW}.
	 */
	String COLLAB_GROUP_LIST_OBJ = "collabgroups";

	/**
	 * Group details in {@link #GROUP_DETAILS_VIEW}. Group creation info in
	 * {@link #CREATE_GROUP_VIEW}.
	 */
	String GROUP_OBJ = "group";

	/** State in {@link #BOARD_VIEW}. {@link BoardRecord}. */
	String BOARD_OBJ = "board";

	/**
	 * Blacklist data in {@link #BOARD_VIEW}. {@link BlacklistData}.
	 */
	String BLACKLIST_DATA_OBJ = "bldata";

	/**
	 * Blacklist contents in {@link #BOARD_VIEW}. String serialization of
	 * {@link Blacklist}, if present.
	 */
	String BLACKLIST_OBJ = "blacklist";

	/** Blacklist-related state in {@link #BOARD_VIEW}. Boolean. */
	String HAVE_BLACKLIST = "haveBlacklist";

	/**
	 * Mapping from machine names to whether they're in service, in
	 * {@link #MACHINE_VIEW}.
	 * {@link Map}{@code <}{@link String}{@code ,}{@link Boolean}{@code >}
	 */
	String MACHINE_LIST_OBJ = "machineNames";

	/**
	 * Machines that have been defined, in {@link #MACHINE_VIEW}.
	 *
	 * @see Machine
	 */
	String DEFINED_MACHINES_OBJ = "definedMachines";

	/**
	 * Result of {@link MachineStateControl#getMachineTagging()}, in
	 * {@link #MACHINE_VIEW}.
	 *
	 * @see MachineTagging
	 */
	String MACHINE_TAGGING_OBJ = "machineTagging";

	/**
	 * Number of {@code default} tags in {@link #MACHINE_TAGGING_OBJ}, in
	 * {@link #MACHINE_VIEW}.
	 */
	String DEFAULT_TAGGING_COUNT = "defaultCount";

	/**
	 * Result of {@link MachineStateControl#getMachineReports()}, in
	 * {@link #MACHINE_VIEW}.
	 *
	 * @see BoardIssueReport
	 */
	String MACHINE_REPORTS_OBJ = "machineReports";

	/** The base URI for the current request. In all views. */
	String BASE_URI = "baseuri";

	/** The members of {@link TrustLevel}. In all views. */
	String TRUST_LEVELS = "trustLevels";

	/** How to call {@link AdminController#listUsers()}. In all views. */
	String USERS_URI = "usersUri";

	/**
	 * How to call {@link AdminController#getUserCreationForm()}. In all views.
	 */
	String CREATE_USER_URI = "createUserUri";

	/** How to call {@link AdminController#listGroups()}. In all views. */
	String GROUPS_URI = "groupsUri";

	/**
	 * How to call {@link AdminController#getGroupCreationForm()}. In all views.
	 */
	String CREATE_GROUP_URI = "createGroupUri";

	/** How to call {@link AdminController#boards()}. In all views. */
	String BOARDS_URI = "boardsUri";

	/**
	 * How to call {@link AdminController#blacklistHandling()}. In
	 * {@link #BOARD_VIEW}.
	 */
	String BLACKLIST_URI = "blacklistControlUri";

	/**
	 * How to call {@link AdminController#machineManagement()}. In all views.
	 */
	String MACHINE_URI = "machineUri";
}
