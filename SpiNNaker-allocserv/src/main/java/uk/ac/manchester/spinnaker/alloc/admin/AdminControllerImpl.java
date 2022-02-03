/*
 * Copyright (c) 2021 The University of Manchester
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

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequestUri;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.error;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.uri;
import static uk.ac.manchester.spinnaker.alloc.web.SystemController.USER_MAY_CHANGE_PASSWORD;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;

/**
 * Implements the logic supporting the JSP views and maps them into URL space.
 *
 * @author Donal Fellows
 */
@Controller("mvc.adminController")
@RequestMapping(AdminController.BASE_PATH)
@PreAuthorize(IS_ADMIN)
public class AdminControllerImpl extends DatabaseAwareBean
		implements AdminController {
	private static final Logger log = getLogger(AdminControllerImpl.class);

	/** One board-hour in board-seconds. */
	private static final int BOARD_HOUR = 3600;

	// These are paths below src/main/webapp/WEB-INF/views
	private static final String MAIN_VIEW = "admin/index";

	private static final String USER_LIST_VIEW = "admin/listusers";

	private static final String USER_DETAILS_VIEW = "admin/userdetails";

	private static final String CREATE_USER_VIEW = "admin/createuser";

	private static final String BOARD_VIEW = "admin/board";

	private static final String MACHINE_VIEW = "admin/machine";

	private static final String USER_OBJ = "user";

	private static final String BOARD_OBJ = "board";

	private static final String MACHINE_LIST_OBJ = "machineNames";

	private static final String DEFINED_MACHINES_OBJ = "definedMachines";

	private static final String MACHINE_TAGGING_OBJ = "machineTagging";

	private static final String DEFAULT_TAGGING_COUNT = "defaultCount";

	private static final String MACHINE_REPORTS_OBJ = "machineReports";

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

	private List<String> getMachineNames(boolean allowOutOfService) {
		try (Connection conn = getConnection();
				Query listMachines = conn.query(LIST_MACHINE_NAMES)) {
			return conn.transaction(false,
					() -> listMachines.call(allowOutOfService)
							.map(string("machine_name")).toList());
		} catch (DataAccessException e) {
			log.warn("problem when listing machines", e);
			return emptyList();
		}
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
		if (attrs != null) {
			attrs.addFlashAttribute("baseuri",
					fromCurrentRequestUri().toUriString());
			attrs.addFlashAttribute("trustLevels", TrustLevel.values());
			attrs.addFlashAttribute("usersUri",
					uri(on(AdminController.class).listUsers()));
			attrs.addFlashAttribute("createUserUri",
					uri(on(AdminController.class).getUserCreationForm()));
			attrs.addFlashAttribute("boardsUri",
					uri(on(AdminController.class).boards()));
			attrs.addFlashAttribute("machineUri",
					uri(on(AdminController.class).machineManagement()));
			Authentication auth =
					SecurityContextHolder.getContext().getAuthentication();
			attrs.addFlashAttribute(USER_MAY_CHANGE_PASSWORD,
					auth instanceof UsernamePasswordAuthenticationToken);
		} else {
			mav.addObject("baseuri", fromCurrentRequestUri().toUriString());
			mav.addObject("trustLevels", TrustLevel.values());
			mav.addObject("usersUri",
					uri(on(AdminController.class).listUsers()));
			mav.addObject("createUserUri",
					uri(on(AdminController.class).getUserCreationForm()));
			mav.addObject("boardsUri", uri(on(AdminController.class).boards()));
			mav.addObject("machineUri",
					uri(on(AdminController.class).machineManagement()));
			Authentication auth =
					SecurityContextHolder.getContext().getAuthentication();
			mav.addObject(USER_MAY_CHANGE_PASSWORD,
					auth instanceof UsernamePasswordAuthenticationToken);
		}
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
	 * All models should contain a common set of attributes that describe where
	 * the view is rendering and where other parts of the admin interface are.
	 *
	 * @param viewName
	 *            The name of the view; the model will be a basic model with
	 *            just the standard attributes.
	 * @return The model-and-view.
	 */
	private static ModelAndView addStandardContext(String viewName) {
		return addStandardContext(new ModelAndView(viewName), null);
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

	private static ModelAndView errors(BindingResult result) {
		if (result.hasGlobalErrors()) {
			return errors(result.getGlobalError().toString());
		}
		if (result.hasFieldErrors()) {
			return errors(result.getFieldError().toString());
		}
		return errors("unknown error");
	}

	@Override
	public ModelAndView mainUI() {
		return addStandardContext(MAIN_VIEW);
	}

	@Override
	@GetMapping(USERS_PATH)
	public ModelAndView listUsers() {
		Map<String, URI> result = new TreeMap<>();
		try {
			userController.listUsers()
					.forEach(user -> result.put(user.getUserName(),
							uri(on(AdminController.class)
									.showUserForm(user.getUserId()))));
		} catch (DataAccessException e) {
			return errors(e);
		}

		return addStandardContext(new ModelAndView(USER_LIST_VIEW, "userlist",
				unmodifiableMap(result)));
	}

	@Override
	@GetMapping(CREATE_USER_PATH)
	public ModelAndView getUserCreationForm() {
		return addStandardContext(
				new ModelAndView(CREATE_USER_VIEW, USER_OBJ, new UserRecord()));
	}

	@Override
	@PostMapping(CREATE_USER_PATH)
	public ModelAndView createUser(
			@Valid @ModelAttribute(USER_OBJ) UserRecord user,
			BindingResult result, ModelMap model, RedirectAttributes attrs) {
		if (result.hasErrors()) {
			return errors(result);
		}
		user.initCreationDefaults();
		Optional<UserRecord> realUser;
		try {
			realUser = userController.createUser(user);
		} catch (DataAccessException e) {
			return errors(e);
		}
		if (!realUser.isPresent()) {
			return errors("user creation failed (duplicate username?)");
		}
		int id = realUser.get().getUserId();
		log.info("created user ID={} username={}", id,
				realUser.get().getUserName());
		return redirectTo(uri(on(AdminController.class).showUserForm(id)),
				attrs);
	}

	@Override
	@GetMapping(USER_PATH)
	public ModelAndView showUserForm(@PathVariable("id") int id) {
		ModelAndView mav = new ModelAndView(USER_DETAILS_VIEW);
		Optional<UserRecord> user;
		try {
			user = userController.getUser(id);
			if (!user.isPresent()) {
				return errors("no such user");
			}
		} catch (DataAccessException e) {
			return errors(e);
		}
		mav.addObject(USER_OBJ, user.get().sanitise());
		mav.addObject("deleteUri",
				uri(on(AdminController.class).deleteUser(id, null, null)));
		mav.addObject("addQuotaUri",
				uri(on(AdminController.class).adjustQuota(id, "", 0, null)));
		return addStandardContext(mav);
	}

	@Override
	@PostMapping(USER_PATH)
	public ModelAndView submitUserForm(@PathVariable("id") int id,
			@Valid @ModelAttribute(USER_OBJ) UserRecord user,
			BindingResult result, ModelMap model, Principal principal) {
		if (result.hasErrors()) {
			return errors(result);
		}
		String adminUser = principal.getName();
		user.setUserId(null);
		Optional<UserRecord> updatedUser;
		try {
			log.info("updating user ID={}", id);
			updatedUser = userController.updateUser(id, user, adminUser);
		} catch (DataAccessException e) {
			return errors(e);
		}
		if (!updatedUser.isPresent()) {
			return errors("no such user");
		}
		ModelAndView mav = new ModelAndView(USER_DETAILS_VIEW, model);
		mav.addObject(USER_OBJ, updatedUser.get().sanitise());
		return addStandardContext(mav);
	}

	@Override
	@PostMapping(USER_DELETE_PATH)
	public ModelAndView deleteUser(@PathVariable("id") int id,
			Principal principal, RedirectAttributes attrs) {
		ModelAndView mav =
				redirectTo(uri(on(AdminController.class).listUsers()), attrs);
		String adminUser = principal.getName();
		try {
			Optional<String> deletedUsername =
					userController.deleteUser(id, adminUser);
			if (!deletedUsername.isPresent()) {
				return errors("could not delete that user");
			}
			log.info("deleted user ID={} username={}", id,
					deletedUsername.get());
			// Not sure that these are the correct place
			attrs.addFlashAttribute("notice",
					"deleted " + deletedUsername.get());
		} catch (DataAccessException e) {
			return errors(e);
		}
		attrs.addFlashAttribute(USER_OBJ, new UserRecord());
		return mav;
	}

	@Override
	@PostMapping(USER_QUOTA_PATH)
	public ModelAndView adjustQuota(@PathVariable("id") int id,
			@RequestParam("machine") String machine,
			@RequestParam("delta") int delta, RedirectAttributes attrs) {
		if (isNull(machine)) {
			return errors("machine must be specified");
		}
		try {
			quotaManager.addQuota(id, machine, delta * BOARD_HOUR);
			log.info("adjusted quota for user ID={} machine={} delta={}", id,
					machine, delta);
			return redirectTo(uri(on(AdminController.class).showUserForm(id)),
					attrs);
		} catch (DataAccessException e) {
			return errors(e);
		}
	}

	@Override
	@GetMapping(BOARDS_PATH)
	public ModelAndView boards() {
		ModelAndView mav = new ModelAndView(BOARD_VIEW);
		mav.addObject(BOARD_OBJ, new BoardRecord());
		mav.addObject(MACHINE_LIST_OBJ, getMachineNames(true));
		return addStandardContext(mav);
	}

	// TODO refactor into multiple methods
	@Override
	@PostMapping(BOARDS_PATH)
	public ModelAndView board(
			@Valid @ModelAttribute(BOARD_OBJ) BoardRecord board,
			BindingResult result, ModelMap model) {
		ModelAndView mav = new ModelAndView(BOARD_VIEW, model);
		if (result.hasErrors()) {
			return errors(result);
		}
		BoardState bs = null;
		try {
			if (nonNull(board.getId())) {
				bs = machineController.findId(board.getId()).orElse(null);
			} else if (board.isTriadCoordPresent()) {
				bs = machineController.findTriad(board.getMachineName(),
						board.getX(), board.getY(), board.getZ()).orElse(null);
			} else if (board.isPhysicalCoordPresent()) {
				bs = machineController.findPhysical(board.getMachineName(),
						board.getCabinet(), board.getFrame(), board.getBoard())
						.orElse(null);
			} else if (board.isAddressPresent()) {
				bs = machineController
						.findIP(board.getMachineName(), board.getIpAddress())
						.orElse(null);
			} else {
				// unreachable because of validation
				throw new UnsupportedOperationException("bad address");
			}
		} catch (DataAccessException e) {
			return errors(e);
		}
		if (isNull(bs)) {
			return errors("no such board");
		}

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
		try {
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
		} catch (DataAccessException e) {
			return errors(e);
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
	@GetMapping(MACHINE_PATH)
	public ModelAndView machineManagement() {
		ModelAndView mav = new ModelAndView(MACHINE_VIEW, MACHINE_LIST_OBJ,
				getMachineNames(true));
		List<MachineTagging> tagging = machineController.getMachineTagging();
		mav.addObject(MACHINE_TAGGING_OBJ, tagging);
		mav.addObject(DEFAULT_TAGGING_COUNT, tagging.stream()
				.filter(MachineTagging::isTaggedAsDefault).count());
		mav.addObject(MACHINE_REPORTS_OBJ,
				machineController.getMachineReports());
		return addStandardContext(mav);
	}

	@Override
	@PostMapping(path = MACHINE_PATH, params = MACHINE_RETAG_PARAM)
	public ModelAndView retagMachine(@ModelAttribute("machine") int machineId,
			@ModelAttribute(MACHINE_RETAG_PARAM) String newTags,
			ModelMap modelMap) {
		Set<String> tags = new HashSet<>();
		for (String tag : newTags.split(",")) {
			tag = tag.trim();
			if (tag.matches("\\w+")) {
				tags.add(tag);
			} else {
				return errors("tag \"" + tag + "\" is illegal");
			}
		}
		try {
			machineController.updateTags(machineId, tags);
		} catch (DataAccessException e) {
			return errors(e);
		}
		return machineManagement();
	}

	@Override
	@PostMapping(value = MACHINE_PATH, params = MACHINE_FILE_PARAM)
	public ModelAndView defineMachine(
			@RequestParam(MACHINE_FILE_PARAM) MultipartFile file,
			ModelMap modelMap) {
		List<Machine> machines;
		try (InputStream input = file.getInputStream()) {
			machines = machineDefiner.readMachineDefinitions(input);
			for (Machine m : machines) {
				machineDefiner.loadMachineDefinition(m);
				log.info("defined machine {}", m.getName());
			}
		} catch (DataAccessException e) {
			return errors(e);
		} catch (IOException e) {
			return errors("problem with processing file: " + e.getMessage());
		}
		ModelAndView mav = machineManagement();
		// Tailor with extra objects here
		mav.addObject(DEFINED_MACHINES_OBJ, machines);
		return mav;
	}
}
