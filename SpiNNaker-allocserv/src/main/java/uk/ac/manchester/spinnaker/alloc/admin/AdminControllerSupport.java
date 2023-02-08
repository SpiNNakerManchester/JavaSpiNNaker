/*
 * Copyright (c) 2021-2022 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.admin;

import static java.util.Collections.unmodifiableMap;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import uk.ac.manchester.spinnaker.alloc.admin.AdminController.BlacklistData;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.Machine;
import uk.ac.manchester.spinnaker.alloc.model.BoardIssueReport;
import uk.ac.manchester.spinnaker.alloc.model.BoardRecord;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.MachineTagging;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.ViewFactory;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Support code for {@link AdminController}.
 */
@UsedInJavadocOnly({ GroupType.class, BoardIssueReport.class })
interface AdminControllerSupport {
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
	static void addLocalUserList(ModelAndView mav, Map<String, URI> userList) {
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
	static void addRemoteUserList(ModelAndView mav, Map<String, URI> userList) {
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
	static void addUserList(ModelAndView mav, Map<String, URI> userList) {
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
	static void addUser(ModelAndView mav, UserRecord user) {
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
	static void addUser(RedirectAttributes attrs, UserRecord user) {
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
	static void addLocalGroupList(ModelAndView mav,
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
	static void addOrganisationList(ModelAndView mav,
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
	static void addCollabratoryList(ModelAndView mav,
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
	static void addGroup(ModelAndView mav, GroupRecord group) {
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
	static void addBoard(ModelAndView mav, BoardRecord board) {
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
	static void addBoard(ModelMap model, BoardRecord board) {
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
	static void addBlacklist(ModelMap model, BlacklistData bldata) {
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
	static void addMachineList(ModelAndView mav,
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
	static void addMachineList(ModelMap model,
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
	static void addMachineTagging(ModelAndView mav,
			List<MachineTagging> tagging) {
		mav.addObject(MACHINE_TAGGING_OBJ, tagging);
		mav.addObject(DEFAULT_TAGGING_COUNT, tagging.stream()
				.filter(MachineTagging::isTaggedAsDefault).count());
	}

	/**
	 * Add machine reports to model.
	 *
	 * @param mav
	 *            The model
	 * @param reports
	 *            The machine reports to add.
	 */
	static void addMachineReports(ModelAndView mav,
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
	static void addUrl(ModelAndView mav, String handle, URI url) {
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
	static void addUrl(ModelMap model, String handle, URI url) {
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
	static void addNotice(RedirectAttributes attrs, String msg) {
		attrs.addFlashAttribute("notice", msg);
	}
}
