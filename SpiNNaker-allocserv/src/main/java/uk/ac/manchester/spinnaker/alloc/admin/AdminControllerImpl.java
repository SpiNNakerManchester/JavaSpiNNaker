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

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
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
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.model.BoardIssueReport;
import uk.ac.manchester.spinnaker.alloc.model.BoardRecord;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType;
import uk.ac.manchester.spinnaker.alloc.model.MachineTagging;
import uk.ac.manchester.spinnaker.alloc.model.MemberRecord;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.web.Action;
import uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.ViewFactory;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;
import uk.ac.manchester.spinnaker.alloc.web.SystemController;

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
	 * Only call from
	 * {@link #addStandardContext(ModelAndView, RedirectAttributes)}.
	 *
	 * @param model
	 *            The base model to add to. This may be the real model or the
	 *            flash attributes.
	 */
	private static void addStandardContextAttrs(Map<String, Object> model) {
		Authentication auth = getContext().getAuthentication();
		boolean mayChangePassword =
				auth instanceof UsernamePasswordAuthenticationToken;

		model.put(BASE_URI, fromCurrentRequestUri().toUriString());
		model.put(TRUST_LEVELS, TrustLevel.values());
		model.put(USERS_URI, uri(admin().listUsers()));
		model.put(CREATE_USER_URI, uri(admin().getUserCreationForm()));
		model.put(CREATE_GROUP_URI,
				uri(admin().getGroupCreationForm()));
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

	// Type-safe manipulators for models
	// These are static so they *can't* access state they shouldn't

	/**
	 * Add local user list to model.
	 *
	 * @param mav
	 *            The model
	 * @param userList
	 *            The user list to add.
	 */
	private static void addLocalUserList(ModelAndView mav,
			Map<String, URI> userList) {
		mav.addObject(LOCAL_USER_LIST_OBJ, unmodifiableMap(userList));
	}

	/**
	 * Add openid-based (remote) user list to model.
	 *
	 * @param mav
	 *            The model
	 * @param userList
	 *            The user list to add.
	 */
	private static void addRemoteUserList(ModelAndView mav,
			Map<String, URI> userList) {
		mav.addObject(OPENID_USER_LIST_OBJ, unmodifiableMap(userList));
	}

	/**
	 * Add general user list to model.
	 *
	 * @param mav
	 *            The model
	 * @param userList
	 *            The group list to add.
	 */
	private static void addUserList(ModelAndView mav,
			Map<String, URI> userList) {
		mav.addObject(USER_LIST_OBJ, unmodifiableMap(userList));
	}

	/**
	 * Add user record to model.
	 *
	 * @param mav
	 *            The model
	 * @param user
	 *            The user record to add.
	 */
	private static void addUser(ModelAndView mav, UserRecord user) {
		mav.addObject(USER_OBJ, user.sanitise());
	}

	/**
	 * Add user record to model.
	 *
	 * @param attrs
	 *            The model
	 * @param user
	 *            The user record to add.
	 */
	private static void addUser(RedirectAttributes attrs, UserRecord user) {
		attrs.addFlashAttribute(USER_OBJ, user);
	}

	/**
	 * Add local group list to model.
	 *
	 * @param mav
	 *            The model
	 * @param groupList
	 *            The group list to add.
	 */
	private static void addLocalGroupList(ModelAndView mav,
			Map<String, URI> groupList) {
		mav.addObject(LOCAL_GROUP_LIST_OBJ, unmodifiableMap(groupList));
	}

	/**
	 * Add organisation list to model.
	 *
	 * @param mav
	 *            The model
	 * @param orgList
	 *            The group list to add.
	 */
	private static void addOrganisationList(ModelAndView mav,
			Map<String, URI> orgList) {
		mav.addObject(ORG_GROUP_LIST_OBJ, unmodifiableMap(orgList));
	}

	/**
	 * Add collabratory list to model.
	 *
	 * @param mav
	 *            The model
	 * @param collabList
	 *            The group list to add.
	 */
	private static void addCollabratoryList(ModelAndView mav,
			Map<String, URI> collabList) {
		mav.addObject(COLLAB_GROUP_LIST_OBJ, unmodifiableMap(collabList));
	}

	/**
	 * Add group record to model.
	 *
	 * @param mav
	 *            The model
	 * @param group
	 *            The group record to add.
	 */
	private static void addGroup(ModelAndView mav, GroupRecord group) {
		mav.addObject(GROUP_OBJ, group);
	}

	/**
	 * Add board record to model.
	 *
	 * @param mav
	 *            The model
	 * @param board
	 *            The board record to add.
	 */
	private static void addBoard(ModelAndView mav, BoardRecord board) {
		mav.addObject(BOARD_OBJ, board);
	}

	/**
	 * Add board record to model.
	 *
	 * @param model
	 *            The model
	 * @param board
	 *            The board record to add.
	 */
	private static void addBoard(ModelMap model, BoardRecord board) {
		model.addAttribute(BOARD_OBJ, board);
	}

	/**
	 * Add blacklist record to model.
	 *
	 * @param model
	 *            The model
	 * @param bldata
	 *            The blacklist record to add.
	 */
	private static void addBlacklist(ModelMap model, BlacklistData bldata) {
		model.addAttribute(BLACKLIST_DATA_OBJ, bldata);
	}

	/**
	 * Add machine list to model.
	 *
	 * @param mav
	 *            The model
	 * @param machineList
	 *            The machine list to add.
	 */
	private static void addMachineList(ModelAndView mav,
			Map<String, Boolean> machineList) {
		mav.addObject(MACHINE_LIST_OBJ, unmodifiableMap(machineList));
	}

	/**
	 * Add machine list to model.
	 *
	 * @param model
	 *            The model
	 * @param machineList
	 *            The machine list to add.
	 */
	private static void addMachineList(ModelMap model,
			Map<String, Boolean> machineList) {
		model.addAttribute(MACHINE_LIST_OBJ, unmodifiableMap(machineList));
	}

	/**
	 * Add machine tagging to model.
	 *
	 * @param mav
	 *            The model
	 * @param tagging
	 *            The machine tagging to add.
	 */
	private static void addMachineTagging(ModelAndView mav,
			List<MachineTagging> tagging) {
		mav.addObject(MACHINE_TAGGING_OBJ, tagging);
		mav.addObject(DEFAULT_TAGGING_COUNT, tagging.stream()
				.filter(MachineTagging::isTaggedAsDefault).count());
	}

	/**
	 * Add machine reports to model.
	 *
	 * @param model
	 *            The model
	 * @param reports
	 *            The machine reports to add.
	 */
	private static void addMachineReports(ModelAndView mav,
			Map<String, List<BoardIssueReport>> reports) {
		mav.addObject(MACHINE_REPORTS_OBJ, reports);
	}

	/**
	 * Add link to model.
	 *
	 * @param mav
	 *            The model
	 * @param handle
	 *            The name of the link
	 * @param url
	 *            The URL to add.
	 */
	private static void addUrl(ModelAndView mav, String handle, URI url) {
		mav.addObject(handle, url);
	}

	/**
	 * Add link to model.
	 *
	 * @param model
	 *            The model
	 * @param handle
	 *            The name of the link
	 * @param url
	 *            The URL to add.
	 */
	private static void addUrl(ModelMap model, String handle, URI url) {
		model.addAttribute(handle, url);
	}

	/**
	 * Add notice message to model.
	 *
	 * @param attrs
	 *            The model
	 * @param msg
	 *            The notice message to add.
	 */
	private static void addNotice(RedirectAttributes attrs, String msg) {
		attrs.addFlashAttribute("notice", msg);
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
		ModelAndView mav = USER_LIST_VIEW.view();
		addLocalUserList(mav,
				userManager.listUsers(true, this::showUserFormUrl));
		addRemoteUserList(mav,
				userManager.listUsers(false, this::showUserFormUrl));
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
		addNotice(attrs, "created " + realUser.getUserName());
		return redirectTo(showUserFormUrl(id), attrs);
	}

	@Override
	@Action("getting info about a user")
	public ModelAndView showUserForm(int id) {
		ModelAndView mav = USER_DETAILS_VIEW.view();
		UserRecord user = userManager.getUser(id, this::showGroupInfoUrl)
				.orElseThrow(NoUser::new);
		addUser(mav, user);
		addUrl(mav, "deleteUri", deleteUserUrl(id));
		return addStandardContext(mav);
	}

	/**
	 * Get URL for calling {@link #showUserForm(int) showUserForm()} later.
	 * @param id User ID
	 * @return URL
	 */
	private URI showUserFormUrl(int id) {
		return uri(admin().showUserForm(id));
	}

	/**
	 * Get URL for calling {@link #showUserForm(int) showUserForm()} later.
	 * @param member Record referring to user
	 * @return URL
	 */
	private URI showUserFormUrl(MemberRecord member) {
		return uri(admin().showUserForm(member.getUserId()));
	}

	/**
	 * Get URL for calling {@link #showUserForm(int) showUserForm()} later.
	 * @param user Record referring to user
	 * @return URL
	 */
	private URI showUserFormUrl(UserRecord user) {
		return uri(admin().showUserForm(user.getUserId()));
	}

	@Override
	@Action("updating a user's details")
	public ModelAndView submitUserForm(int id, UserRecord user, ModelMap model,
			Principal principal) {
		String adminUser = principal.getName();
		user.setUserId(null);
		log.info("updating user ID={}", id);
		UserRecord updatedUser = userManager
				.updateUser(id, user, adminUser, this::showGroupInfoUrl)
				.orElseThrow(NoUser::new);
		ModelAndView mav = USER_DETAILS_VIEW.view(model);
		addUser(mav, updatedUser);
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
		ModelAndView mav = GROUP_LIST_VIEW.view();
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
		ModelAndView mav = GROUP_DETAILS_VIEW.view();
		Map<String, URI> userLocations = new HashMap<>();
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
	 * @param id Group ID
	 * @return URL
	 */
	private URI showGroupInfoUrl(int id) {
		return uri(admin().showGroupInfo(id));
	}

	/**
	 * Get URL for calling {@link #showGroupInfo(int) showGroupInfo()} later.
	 * @param membership Record referring to group
	 * @return URL
	 */
	private URI showGroupInfoUrl(MemberRecord membership) {
		return uri(admin().showGroupInfo(membership.getGroupId()));
	}

	/**
	 * Get URL for calling {@link #showGroupInfo(int) showGroupInfo()} later.
	 * @param group Record referring to group
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
		GroupRecord realGroup =
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
		GroupRecord g =
				userManager.getGroup(id, null).orElseThrow(NoGroup::new);
		UserRecord u = userManager.getUser(user, null).orElseThrow(NoUser::new);
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
		GroupRecord g =
				userManager.getGroup(id, null).orElseThrow(NoGroup::new);
		UserRecord u =
				userManager.getUser(userid, null).orElseThrow(NoUser::new);
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
		String deletedGroupName = userManager.deleteGroup(id)
				.orElseThrow(NoGroup::new);
		log.info("deleted group ID={} groupname={}", id, deletedGroupName);
		ModelAndView mav = redirectTo(uri(admin().listGroups()), attrs);
		addNotice(attrs, "deleted " + deletedGroupName);
		return mav;
	}

	@Override
	@Action("getting the UI for finding boards")
	public ModelAndView boards() {
		ModelAndView mav = BOARD_VIEW.view();
		addBoard(mav, new BoardRecord());
		addMachineList(mav, getMachineNames(true));
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
		inflateBoardRecord(board, bs);
		addBoard(model, board);
		addMachineList(model, getMachineNames(true));
		return addStandardContext(BOARD_VIEW.view(model));
	}

	@Override
	@Action("saving changes to a board blacklist")
	public ModelAndView blacklistSave(BlacklistData bldata, ModelMap model) {
		BoardState bs = readAndRememberBoardState(model);

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
		BoardState bs = readAndRememberBoardState(model);

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
		BoardState bs = readAndRememberBoardState(model);

		log.info("pushing blacklist to board {}", bs);
		machineController.pushBlacklist(bs);
		addBlacklistData(bs, model);

		addMachineList(model, getMachineNames(true));
		return completedFuture(addStandardContext(BOARD_VIEW.view(model)));
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
		BoardRecord br = (BoardRecord) model.get(BOARD_OBJ);
		BoardState bs = getBoardState(br).orElseThrow(NoBoard::new);
		inflateBoardRecord(br, bs);
		br.setEnabled(bs.getState());
		// Replace the state in the model with the current values
		addBoard(model, br);
		return bs;
	}

	/**
	 * Add current blacklist data to model.
	 * @param board Which board's blacklist data to add.
	 * @param model The model to add it to.
	 */
	private void addBlacklistData(BoardState board, ModelMap model) {
		BlacklistData bldata = new BlacklistData();
		bldata.setId(board.id);
		Optional<Blacklist> bl = machineController.readBlacklistFromDB(board);
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
		ModelAndView mav = MACHINE_VIEW.view();
		addMachineList(mav, getMachineNames(true));
		List<MachineTagging> tagging = machineController.getMachineTagging();
		tagging.forEach(
				t -> t.setUrl(uri(system().getMachineInfo(t.getName()))));
		addMachineTagging(mav, tagging);
		addMachineReports(mav, machineController.getMachineReports());
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

@UsedInJavadocOnly({GroupType.class, BoardIssueReport.class})
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

	/**
	 * How to call {@link AdminController#listUsers() listUsers()}. In all
	 * views.
	 */
	String USERS_URI = "usersUri";

	/**
	 * How to call {@link AdminController#getUserCreationForm()
	 * getUserCreationForm()}. In all views.
	 */
	String CREATE_USER_URI = "createUserUri";

	/**
	 * How to call {@link AdminController#listGroups() listGroups()}. In all
	 * views.
	 */
	String GROUPS_URI = "groupsUri";

	/**
	 * How to call {@link AdminController#getGroupCreationForm()
	 * getGroupCreationForm()}. In all views.
	 */
	String CREATE_GROUP_URI = "createGroupUri";

	/** How to call {@link AdminController#boards() boards()}. In all views. */
	String BOARDS_URI = "boardsUri";

	/**
	 * How to call {@link AdminController#blacklistSave(BlacklistData,ModelMap)
	 * blacklistSave()},
	 * {@link AdminController#blacklistPush(BlacklistData, ModelMap)
	 * blacklistPush()}, and
	 * {@link AdminController#blacklistFetch(BlacklistData,ModelMap)
	 * blacklistFetch()}. In {@link #BOARD_VIEW}.
	 */
	String BLACKLIST_URI = "blacklistControlUri";

	/**
	 * How to call {@link AdminController#machineManagement()
	 * machineManagement()}. In all views.
	 */
	String MACHINE_URI = "machineUri";
}
