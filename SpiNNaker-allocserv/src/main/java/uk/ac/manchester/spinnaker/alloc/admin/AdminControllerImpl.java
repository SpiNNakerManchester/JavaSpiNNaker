/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.admin;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.IOUtils.buffer;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequestUri;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.BASE_URI;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.BLACKLIST_URI;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.BOARDS_URI;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.BOARD_OBJ;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.BOARD_VIEW;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.CREATE_GROUP_URI;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.CREATE_GROUP_VIEW;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.CREATE_USER_URI;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.CREATE_USER_VIEW;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.DEFINED_MACHINES_OBJ;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.GROUPS_URI;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.GROUP_DETAILS_VIEW;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.GROUP_LIST_VIEW;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.GROUP_OBJ;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.MACHINE_URI;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.MACHINE_VIEW;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.MAIN_VIEW;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.TEMP_URI;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.TEMP_VIEW;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.TRUST_LEVELS;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.USERS_URI;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.USER_DETAILS_VIEW;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.USER_LIST_VIEW;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.USER_OBJ;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addBlacklist;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addBoard;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addCollabratoryList;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addGroup;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addLocalGroupList;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addLocalUserList;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addMachineList;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addMachineReports;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addMachineTagging;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addNotice;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addOrganisationList;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addRemoteUserList;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addTemperatureData;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addUrl;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addUser;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminControllerSupport.addUserList;
import static uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType.COLLABRATORY;
import static uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType.INTERNAL;
import static uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType.ORGANISATION;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.error;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.errorMessage;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.uri;
import static uk.ac.manchester.spinnaker.alloc.web.SystemController.USER_MAY_CHANGE_PASSWORD;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindException;
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
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.model.BoardRecord;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.MemberRecord;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.web.Action;
import uk.ac.manchester.spinnaker.alloc.web.SystemController;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;

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

	private class MachineName {
		String name;

		boolean inService;

		MachineName(Row row) {
			name = row.getString("machine_name");
			inService = row.getBoolean("in_service");
		}
	}

	private Map<String, Boolean> getMachineNames(boolean allowOutOfService) {
		try (var conn = getConnection();
				var listMachines = conn.query(LIST_MACHINE_NAMES)) {
			return conn.transaction(false,
					() -> Row.stream(listMachines.call(MachineName::new,
							allowOutOfService))
					.toMap(m -> m.name, m -> m.inService));
		} catch (DataAccessException e) {
			log.warn("problem when listing machines", e);
			return Map.of();
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
	 * Only call from
	 * {@link #addStandardContext(ModelAndView, RedirectAttributes)}.
	 *
	 * @param model
	 *            The base model to add to. This may be the real model or the
	 *            flash attributes.
	 */
	private static void addStandardContextAttrs(Map<String, Object> model) {
		var auth = getContext().getAuthentication();
		boolean mayChangePassword =
				auth instanceof UsernamePasswordAuthenticationToken;

		model.put(BASE_URI, fromCurrentRequestUri().toUriString());
		model.put(TRUST_LEVELS, TrustLevel.values());
		model.put(USERS_URI, uri(admin().listUsers()));
		model.put(CREATE_USER_URI, uri(admin().getUserCreationForm()));
		model.put(CREATE_GROUP_URI, uri(admin().getGroupCreationForm()));
		model.put(GROUPS_URI, uri(admin().listGroups()));
		model.put(BOARDS_URI, uri(admin().boards()));
		model.put(MACHINE_URI, uri(admin().machineManagement()));
		model.put(USER_MAY_CHANGE_PASSWORD, mayChangePassword);
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
		addStandardContextAttrs(nonNull(attrs)
				// Real implementation of flash attrs is always a ModelMap
				? (ModelMap) attrs.getFlashAttributes()
				: mav.getModel());
		return mav;
	}

	/**
	 * All models should contain a common set of attributes that describe where
	 * the view is rendering and where other parts of the admin interface are.
	 *
	 * @param mav
	 *            The model-and-view.
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
	 * @param attrs
	 *            The redirect attributes, or {@code null} if this is not a
	 *            redirect.
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
		var a = hm.getMethodAnnotation(Action.class);
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
		var a = hm.getMethodAnnotation(Action.class);
		if (nonNull(a)) {
			log.warn("general issue when {}", a.value(), e);
		} else {
			log.warn("general issue", e);
		}
		return errors(e.getMessage());
	}

	// The actual controller methods

	@Override
	@Action("getting the main admin UI")
	public ModelAndView mainUI() {
		return addStandardContext(MAIN_VIEW.view());
	}

	@Override
	@Action("listing the users")
	public ModelAndView listUsers() {
		var mav = USER_LIST_VIEW.view();
		addLocalUserList(mav,
				userManager.listUsers(true, this::showUserFormUrl));
		addRemoteUserList(mav,
				userManager.listUsers(false, this::showUserFormUrl));
		return addStandardContext(mav);
	}

	@Override
	@Action("getting the user-creation UI")
	public ModelAndView getUserCreationForm() {
		var userForm = new UserRecord();
		userForm.setInternal(true);
		return addStandardContext(
				CREATE_USER_VIEW.view(USER_OBJ, userForm));
	}

	@Override
	@Action("creating a user")
	public ModelAndView createUser(UserRecord user, ModelMap model,
			RedirectAttributes attrs) {
		user.initCreationDefaults();
		var realUser = userManager.createUser(user)
				.orElseThrow(() -> new AdminException(
						"user creation failed (duplicate username?)"));
		int id = realUser.getUserId();
		log.info("created user ID={} username={}", id, realUser.getUserName());
		addNotice(attrs, "created " + realUser.getUserName());
		return redirectTo(showUserFormUrl(id), attrs);
	}

	@Override
	@Action("getting info about a user")
	public ModelAndView showUserForm(int id) {
		var mav = USER_DETAILS_VIEW.view();
		var user = userManager.getUser(id, this::showGroupInfoUrl)
				.orElseThrow(NoUser::new);
		addUser(mav, user);
		addUrl(mav, "deleteUri", deleteUserUrl(id));
		return addStandardContext(mav);
	}

	/**
	 * Get URL for calling {@link #showUserForm(int) showUserForm()} later.
	 *
	 * @param id
	 *            User ID
	 * @return URL
	 */
	private URI showUserFormUrl(int id) {
		return uri(admin().showUserForm(id));
	}

	/**
	 * Get URL for calling {@link #showUserForm(int) showUserForm()} later.
	 *
	 * @param member
	 *            Record referring to user
	 * @return URL
	 */
	private URI showUserFormUrl(MemberRecord member) {
		return uri(admin().showUserForm(member.getUserId()));
	}

	/**
	 * Get URL for calling {@link #showUserForm(int) showUserForm()} later.
	 *
	 * @param user
	 *            Record referring to user
	 * @return URL
	 */
	private URI showUserFormUrl(UserRecord user) {
		return uri(admin().showUserForm(user.getUserId()));
	}

	@Override
	@Action("updating a user's details")
	public ModelAndView submitUserForm(int id, UserRecord user, ModelMap model,
			Principal principal) {
		var adminUser = principal.getName();
		user.setUserId(null);
		log.info("updating user ID={}", id);
		var mav = USER_DETAILS_VIEW.view(model);
		addUser(mav,
				userManager
						.updateUser(id, user, adminUser, this::showGroupInfoUrl)
						.orElseThrow(NoUser::new));
		return addStandardContext(mav);
	}

	@Override
	@Action("deleting a user")
	public ModelAndView deleteUser(int id, Principal principal,
			RedirectAttributes attrs) {
		var adminUser = principal.getName();
		var deletedUsername = userManager.deleteUser(id, adminUser).orElseThrow(
				() -> new AdminException("could not delete that user"));
		log.info("deleted user ID={} username={}", id, deletedUsername);
		// Not sure that these are the correct place
		var mav = redirectTo(uri(admin().listUsers()), attrs);
		addNotice(attrs, "deleted " + deletedUsername);
		addUser(attrs, new UserRecord());
		return mav;
	}

	/**
	 * Get URL for calling
	 * {@link #deleteUser(int, Principal, RedirectAttributes) deleteUser()}
	 * later.
	 *
	 * @param id
	 *            User ID
	 * @return URL
	 */
	private URI deleteUserUrl(int id) {
		return uri(admin().deleteUser(id, null, null));
	}

	@Override
	@Action("listing the groups")
	public ModelAndView listGroups() {
		var mav = GROUP_LIST_VIEW.view();
		addLocalGroupList(mav,
				userManager.listGroups(INTERNAL, this::showGroupInfoUrl));
		addOrganisationList(mav,
				userManager.listGroups(ORGANISATION, this::showGroupInfoUrl));
		addCollabratoryList(mav,
				userManager.listGroups(COLLABRATORY, this::showGroupInfoUrl));
		return addStandardContext(mav);
	}

	@Override
	@Action("getting info about a group")
	public ModelAndView showGroupInfo(int id) {
		var mav = GROUP_DETAILS_VIEW.view();
		var userLocations = new HashMap<String, URI>();
		addGroup(mav, userManager.getGroup(id, m -> {
			userLocations.put(m.getUserName(), showUserFormUrl(m));
			return uri(admin().removeUserFromGroup(id, m.getUserId(), null));
		}).orElseThrow(NoGroup::new));
		addUserList(mav, userLocations);
		addUrl(mav, "deleteUri", uri(admin().deleteGroup(id, null)));
		addUrl(mav, "addUserUri", uri(admin().addUserToGroup(id, null, null)));
		addUrl(mav, "addQuotaUri", uri(admin().adjustGroupQuota(id, 0, null)));
		return addStandardContext(mav);
	}

	/**
	 * Get URL for calling {@link #showGroupInfo(int) showGroupInfo()} later.
	 *
	 * @param id
	 *            Group ID
	 * @return URL
	 */
	private URI showGroupInfoUrl(int id) {
		return uri(admin().showGroupInfo(id));
	}

	/**
	 * Get URL for calling {@link #showGroupInfo(int) showGroupInfo()} later.
	 *
	 * @param membership
	 *            Record referring to group
	 * @return URL
	 */
	private URI showGroupInfoUrl(MemberRecord membership) {
		return uri(admin().showGroupInfo(membership.getGroupId()));
	}

	/**
	 * Get URL for calling {@link #showGroupInfo(int) showGroupInfo()} later.
	 *
	 * @param group
	 *            Record referring to group
	 * @return URL
	 */
	private URI showGroupInfoUrl(GroupRecord group) {
		return uri(admin().showGroupInfo(group.getGroupId()));
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
		var realGroup =
				userManager.createGroup(groupRequest.toGroupRecord(), INTERNAL)
						.orElseThrow(() -> new AdminException(
								"group creation failed (duplicate name?)"));
		int id = realGroup.getGroupId();
		log.info("created group ID={} name={}", id, realGroup.getGroupName());
		addNotice(attrs, "created " + realGroup.getGroupName());
		return redirectTo(showGroupInfoUrl(id), attrs);
	}

	@Override
	@Action("adding a user to a group")
	public ModelAndView addUserToGroup(int id, String user,
			RedirectAttributes attrs) {
		var g = userManager.getGroup(id, null).orElseThrow(NoGroup::new);
		var u = userManager.getUser(user, null).orElseThrow(NoUser::new);
		if (userManager.addUserToGroup(u, g).isPresent()) {
			log.info("added user {} to group {}", u.getUserName(),
					g.getGroupName());
			addNotice(attrs, format("added user %s to group %s",
					u.getUserName(), g.getGroupName()));
		} else {
			addNotice(attrs, format("user %s is already a member of group %s",
					u.getUserName(), g.getGroupName()));
		}
		return redirectTo(showGroupInfoUrl(id), attrs);
	}

	@Override
	@Action("removing a user from a group")
	public ModelAndView removeUserFromGroup(int id, int userid,
			RedirectAttributes attrs) {
		var g = userManager.getGroup(id, null).orElseThrow(NoGroup::new);
		var u = userManager.getUser(userid, null).orElseThrow(NoUser::new);
		if (userManager.removeUserFromGroup(u, g)) {
			log.info("removed user {} from group {}", u.getUserName(),
					g.getGroupName());
			addNotice(attrs, format("removed user %s from group %s",
					u.getUserName(), g.getGroupName()));
		} else {
			addNotice(attrs,
					format("user %s is already not a member of group %s",
							u.getUserName(), g.getGroupName()));
		}
		return redirectTo(showGroupInfoUrl(id), attrs);
	}

	@Override
	@Action("adjusting a group's quota")
	public ModelAndView adjustGroupQuota(int id, int delta,
			RedirectAttributes attrs) {
		quotaManager.addQuota(id, delta * BOARD_HOUR).ifPresent(aq -> {
			log.info("adjusted quota for group {} to {}", aq.getName(),
					aq.getQuota());
			// addNotice(attrs, "quota updated");
		});
		return redirectTo(showGroupInfoUrl(id), attrs);
	}

	@Override
	@Action("deleting a group")
	public ModelAndView deleteGroup(int id, RedirectAttributes attrs) {
		var deletedGroupName =
				userManager.deleteGroup(id).orElseThrow(NoGroup::new);
		log.info("deleted group ID={} groupname={}", id, deletedGroupName);
		addNotice(attrs, "deleted " + deletedGroupName);
		return redirectTo(uri(admin().listGroups()), attrs);
	}

	@Override
	@Action("getting the UI for finding boards")
	public ModelAndView boards() {
		var mav = BOARD_VIEW.view();
		addBoard(mav, new BoardRecord());
		addMachineList(mav, getMachineNames(true));
		return addStandardContext(mav);
	}

	private Optional<BoardState> getBoardState(BoardRecord board) {
		if (nonNull(board.getId())) {
			return machineController.findId(board.getId());
		} else if (board.isTriadCoordPresent()) {
			return machineController.findTriad(board.getMachineName(),
					new TriadCoords(board.getX(), board.getY(), board.getZ()));
		} else if (board.isPhysicalCoordPresent()) {
			return machineController.findPhysical(board.getMachineName(),
					new PhysicalCoords(board.getCabinet(), board.getFrame(),
							board.getBoard()));
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
		var bs = getBoardState(board).orElseThrow(NoBoard::new);

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
		inflateBoardRecord(board, bs);
		addBoard(model, board);
		addMachineList(model, getMachineNames(true));
		addUrl(model, TEMP_URI,
				uri(admin().getTemperatures(board.getId(), model)));
		return addStandardContext(BOARD_VIEW.view(model));
	}

	@Override
	@Action("saving changes to a board blacklist")
	public ModelAndView blacklistSave(BlacklistData bldata, ModelMap model) {
		var bs = readAndRememberBoardState(model);

		if (bldata.isPresent()) {
			machineController.writeBlacklistToDB(bs,
					bldata.getParsedBlacklist());
		}
		addBlacklistData(bs, model);

		addMachineList(model, getMachineNames(true));
		return addStandardContext(BOARD_VIEW.view(model));
	}

	@Override
	@Async
	@Action("fetching a live board blacklist from the machine")
	public CompletableFuture<ModelAndView> blacklistFetch(BlacklistData bldata,
			ModelMap model) {
		var bs = readAndRememberBoardState(model);

		log.info("pulling blacklist from board {}", bs);
		machineController.pullBlacklist(bs);
		addBlacklistData(bs, model);

		addMachineList(model, getMachineNames(true));
		return completedFuture(addStandardContext(BOARD_VIEW.view(model)));
	}

	@Override
	@Async
	@Action("pushing a board blacklist to the machine")
	public CompletableFuture<ModelAndView> blacklistPush(BlacklistData bldata,
			ModelMap model) {
		var bs = readAndRememberBoardState(model);

		log.info("pushing blacklist to board {}", bs);
		machineController.pushBlacklist(bs);
		addBlacklistData(bs, model);

		addMachineList(model, getMachineNames(true));
		return completedFuture(addStandardContext(BOARD_VIEW.view(model)));
	}

	@Override
	@Async
	@Action("getting board temperature data from the machine")
	public CompletableFuture<ModelAndView> getTemperatures(
			int boardId, ModelMap model) {
		try {
			var temps = machineController.readTemperatureFromMachine(boardId)
					.orElse(null);
			addTemperatureData(model, temps);
			return completedFuture(addStandardContext(TEMP_VIEW.view(model)));
		} catch (InterruptedException e) {
			return failedFuture(e);
		}
	}

	/**
	 * Get the board record from the model and inflate a board state control
	 * object.
	 *
	 * @param model
	 *            The model.
	 * @return The board state control object. <em>This object should not be put
	 *         in the model as it is DB-aware!</em>
	 */
	private BoardState readAndRememberBoardState(ModelMap model) {
		var br = (BoardRecord) model.get(BOARD_OBJ);
		var bs = getBoardState(br).orElseThrow(NoBoard::new);
		inflateBoardRecord(br, bs);
		br.setEnabled(bs.getState());
		// Replace the state in the model with the current values
		addBoard(model, br);
		return bs;
	}

	/**
	 * Add current blacklist data to model.
	 *
	 * @param board
	 *            Which board's blacklist data to add.
	 * @param model
	 *            The model to add it to.
	 */
	private void addBlacklistData(BoardState board, ModelMap model) {
		var bldata = new BlacklistData();
		bldata.setId(board.id);
		var bl = machineController.readBlacklistFromDB(board);
		bldata.setPresent(bl.isPresent());
		bl.map(Blacklist::render).ifPresent(bldata::setBlacklist);
		bldata.setSynched(machineController.isBlacklistSynched(board));

		addBlacklist(model, bldata);
		addUrl(model, BLACKLIST_URI, uri(admin().blacklistSave(null, model)));
	}

	/**
	 * Copy settings from the record out of the DB.
	 *
	 * @param br
	 *            Where the values are copied to. A partial state.
	 * @param bs
	 *            Where the values are copied from. A complete state from the
	 *            DB.
	 */
	private void inflateBoardRecord(BoardRecord br, BoardState bs) {
		// Inflate the coordinates
		br.setId(bs.id);
		br.setX(bs.x);
		br.setY(bs.y);
		br.setZ(bs.z);
		br.setCabinet(bs.cabinet);
		br.setFrame(bs.frame);
		br.setBoard(bs.board);
		br.setIpAddress(bs.address);
		br.setMachineName(bs.machineName);

		// Inflate the other properties
		br.setPowered(bs.getPower());
		br.setLastPowerOff(bs.getPowerOffTime().orElse(null));
		br.setLastPowerOn(bs.getPowerOnTime().orElse(null));
		br.setJobId(bs.getAllocatedJob().orElse(null));
		br.setReports(bs.getReports());
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
		var mav = MACHINE_VIEW.view();
		addMachineList(mav, getMachineNames(true));
		var tagging = machineController.getMachineTagging();
		tagging.forEach(
				t -> t.setUrl(uri(system().getMachineInfo(t.getName()))));
		addMachineTagging(mav, tagging);
		addMachineReports(mav, machineController.getMachineReports());
		return addStandardContext(mav);
	}

	@Override
	@Action("retagging a machine")
	public ModelAndView retagMachine(String machineName, String newTags) {
		var tags =
				stream(newTags.split(",")).map(String::strip).collect(toSet());
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
		var machines = extractMachineDefinitions(file);
		for (var m : machines) {
			machineDefiner.loadMachineDefinition(m);
			log.info("defined machine {}", m.getName());
		}
		var mav = machineManagement();
		// Tailor with extra objects here
		mav.addObject(DEFINED_MACHINES_OBJ, machines);
		return mav;
	}

	private List<Machine> extractMachineDefinitions(MultipartFile file) {
		try (var input = buffer(file.getInputStream())) {
			return machineDefiner.readMachineDefinitions(input);
		} catch (IOException e) {
			throw new AdminException(
					"problem with processing file: " + e.getMessage());
		}
	}
}
