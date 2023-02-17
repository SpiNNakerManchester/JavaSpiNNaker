/*
 * Copyright (c) 2021 The University of Manchester
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

import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_ADMIN;

import java.security.Principal;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.errorprone.annotations.Keep;

import uk.ac.manchester.spinnaker.alloc.model.BoardRecord;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.TagList;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.web.SystemController;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;

/**
 * The API for the controller for the admin user interface.
 *
 * @author Donal Fellows
 */
@RequestMapping(AdminController.BASE_PATH)
@PreAuthorize(IS_ADMIN)
public interface AdminController {
	/** The base path of this interface within the MVC domain. */
	String BASE_PATH = SystemController.ROOT_PATH + "/admin";

	/** Path to all-users operations. */
	String USERS_PATH = "/users";

	/** Path to user creation operations. */
	String CREATE_USER_PATH = "/create-user";

	/** Path to single-user operations. */
	String USER_PATH = USERS_PATH + "/{id}";

	/** Path to single-user-deletion operation. */
	String USER_DELETE_PATH = USER_PATH + "/delete";

	/** Path to all-groups operations. */
	String GROUPS_PATH = "/groups";

	/** Path to group creation operations. */
	String CREATE_GROUP_PATH = "/create-group";

	/** Path to single-group operations. */
	String GROUP_PATH = GROUPS_PATH + "/{id}";

	/** Path to user-add-to-group operation. */
	String GROUP_ADD_USER_PATH = GROUP_PATH + "/add-user";

	/** Path to user-remove-from-group operation. */
	String GROUP_REMOVE_USER_PATH = GROUP_PATH + "/remove-user/{userid}";

	/** Path to quota-adjustment operation. */
	String GROUP_QUOTA_PATH = GROUP_PATH + "/adjust-quota";

	/** Path to single-user-deletion operation. */
	String GROUP_DELETE_PATH = GROUP_PATH + "/delete";

	/** Path to boards operations. */
	String BOARDS_PATH = "/boards";

	/** Path to blacklist operations. */
	String BLACKLIST_PATH = "/boards/blacklist";

	/** Path to machine-instantiation operations. */
	String MACHINE_PATH = "/machine";

	/** Name of parameter used when submitting a new machine definition. */
	String MACHINE_FILE_PARAM = "file";

	/** Name of parameter used to mark a retagging. */
	String MACHINE_RETAG_PARAM = "retag";

	/**
	 * Get supported ops.
	 *
	 * @return the view
	 */
	@GetMapping("/")
	ModelAndView mainUI();

	/**
	 * List all users.
	 *
	 * @return the model and view
	 */
	@GetMapping(USERS_PATH)
	ModelAndView listUsers();

	/**
	 * Get the form for creating a user.
	 *
	 * @return the model and view
	 */
	@GetMapping(CREATE_USER_PATH)
	ModelAndView getUserCreationForm();

	/**
	 * Create a user.
	 *
	 * @param user
	 *            The description of the user to create
	 * @param model
	 *            Overall model
	 * @param attrs
	 *            Where to put attributes of the model so that they are
	 *            respected after the redirect without being present in the URL.
	 * @return the model and view
	 */
	@PostMapping(CREATE_USER_PATH)
	ModelAndView createUser(@Valid @ModelAttribute("user") UserRecord user,
			ModelMap model, RedirectAttributes attrs);

	/**
	 * Show user details.
	 *
	 * @param id
	 *            The user ID
	 * @return the model and view
	 */
	@GetMapping(USER_PATH)
	ModelAndView showUserForm(@PathVariable("id") int id);

	/**
	 * Modify user details.
	 *
	 * @param id
	 *            The user ID
	 * @param user
	 *            The description of the user to update
	 * @param model
	 *            Overall model
	 * @param principal
	 *            Who is the admin? Used for safety checks.
	 * @return the model and view
	 */
	@PostMapping(USER_PATH)
	ModelAndView submitUserForm(@PathVariable("id") int id,
			@Valid @ModelAttribute("user") UserRecord user, ModelMap model,
			Principal principal);

	/**
	 * Delete a user.
	 *
	 * @param id
	 *            The user ID to delete
	 * @param principal
	 *            Who is the admin? Used for safety checks.
	 * @param attrs
	 *            Where to put attributes of the model so that they are
	 *            respected after the redirect without being present in the URL.
	 * @return the model and view
	 */
	@PostMapping(USER_DELETE_PATH)
	ModelAndView deleteUser(@PathVariable("id") int id, Principal principal,
			RedirectAttributes attrs);

	/**
	 * List all groups.
	 *
	 * @return the model and view
	 */
	@GetMapping(GROUPS_PATH)
	ModelAndView listGroups();

	/**
	 * Get info about a particular group.
	 *
	 * @param id
	 *            The ID of the group to get info about.
	 * @return the model and view
	 */
	@GetMapping(GROUP_PATH)
	ModelAndView showGroupInfo(@PathVariable("id") int id);

	/**
	 * Get the form for creating a group.
	 *
	 * @return the model (a {@link CreateGroupModel}) and view
	 */
	@GetMapping(CREATE_GROUP_PATH)
	ModelAndView getGroupCreationForm();

	/**
	 * Create a group.
	 *
	 * @param group
	 *            The description of the group to create
	 * @param attrs
	 *            Where to put attributes of the model so that they are
	 *            respected after the redirect without being present in the URL.
	 * @return the model and view
	 */
	@PostMapping(CREATE_GROUP_PATH)
	ModelAndView createGroup(
			@Valid @ModelAttribute("group") CreateGroupModel group,
			RedirectAttributes attrs);

	/**
	 * Model used when creating a group. No other purpose.
	 *
	 * @author Donal Fellows
	 */
	class CreateGroupModel {
		private String name = "";

		@PositiveOrZero
		private Long quota = null;

		private boolean quotaDefined = false;

		private static final int DECIMAL = 10;

		/** @return The name of the group to create. */
		@NotBlank
		public String getName() {
			return name;
		}

		/** @param name The name of the group to create. */
		public void setName(String name) {
			this.name = name.strip();
		}

		/**
		 * @return The quota of the group to create, as a {@link String}. Empty
		 *         if the group is quota-less.
		 */
		public String getQuota() {
			return Objects.toString(quota, "");
		}

		/**
		 * @param quota The quota of the group to create, as a {@link String}.
		 */
		public void setQuota(String quota) {
			try {
				this.quota = Long.parseLong(quota, DECIMAL);
			} catch (NumberFormatException e) {
				this.quota = null;
			}
		}

		/**
		 * @return This request, as a partial group record.
		 */
		public GroupRecord toGroupRecord() {
			var gr = new GroupRecord();
			gr.setGroupName(name);
			if (quotaDefined) {
				gr.setQuota(quota);
			}
			return gr;
		}

		/** @return Whether the group has a quota. */
		public boolean isQuotad() {
			return quotaDefined;
		}

		/** @param value Whether the group has a quota. */
		public void setQuotad(boolean value) {
			quotaDefined = value;
		}
	}

	/**
	 * Add a user to a group.
	 *
	 * @param id
	 *            The group ID to adjust
	 * @param user
	 *            The name of the user to add
	 * @param attrs
	 *            Where to put attributes of the model so that they are
	 *            respected after the redirect without being present in the URL.
	 * @return the model and view
	 */
	@PostMapping(GROUP_ADD_USER_PATH)
	ModelAndView addUserToGroup(@PathVariable("id") int id,
			@NotBlank @RequestParam("user") String user,
			RedirectAttributes attrs);

	/**
	 * Remove a user from a group.
	 *
	 * @param id
	 *            The group ID to adjust
	 * @param userid
	 *            The ID of the user to remove
	 * @param attrs
	 *            Where to put attributes of the model so that they are
	 *            respected after the redirect without being present in the URL.
	 * @return the model and view
	 */
	@PostMapping(GROUP_REMOVE_USER_PATH)
	ModelAndView removeUserFromGroup(@PathVariable("id") int id,
			@PathVariable("userid") int userid,
			RedirectAttributes attrs);

	/**
	 * Adjust the quota of a group.
	 *
	 * @param id
	 *            The group ID to adjust
	 * @param delta
	 *            By how much are we to adjust the quota. In
	 *            <em>board-hours</em>.
	 * @param attrs
	 *            Where to put attributes of the model so that they are
	 *            respected after the redirect without being present in the URL.
	 * @return the model and view
	 */
	@PostMapping(GROUP_QUOTA_PATH)
	ModelAndView adjustGroupQuota(@PathVariable("id") int id,
			@RequestParam("delta") int delta, RedirectAttributes attrs);

	/**
	 * Delete a group. It is legal for a user to not be a member of any group;
	 * they just won't be able to submit jobs if that's the case.
	 *
	 * @param id
	 *            The group ID to delete
	 * @param attrs
	 *            Where to put attributes of the model so that they are
	 *            respected after the redirect without being present in the URL.
	 * @return the model and view
	 */
	@PostMapping(GROUP_DELETE_PATH)
	ModelAndView deleteGroup(@PathVariable("id") int id,
			RedirectAttributes attrs);

	/**
	 * UI for boards.
	 *
	 * @return the model and view
	 */
	@GetMapping(BOARDS_PATH)
	ModelAndView boards();

	/**
	 * Manipulate a board.
	 *
	 * @param board
	 *            The board coordinates, and possibly the state change
	 * @param model
	 *            Overall model
	 * @return the model and view
	 */
	@PostMapping(BOARDS_PATH)
	ModelAndView board(@Valid @ModelAttribute("board") BoardRecord board,
			ModelMap model);

	/**
	 * Manipulate a blacklist.
	 *
	 * @param bldata
	 *            The blacklist data.
	 * @param model
	 *            Overall model
	 * @return the model and view
	 */
	@PostMapping(BLACKLIST_PATH)
	ModelAndView blacklistSave(
			@Valid @ModelAttribute("bldata") BlacklistData bldata,
			ModelMap model);

	/**
	 * Fetch the blacklist for a board from the machine.
	 *
	 * @param bldata
	 *            The blacklist data.
	 * @param model
	 *            Overall model
	 * @return the model and view in a future
	 */
	@Async
	@PostMapping(value = BLACKLIST_PATH, params = "fetch")
	CompletableFuture<ModelAndView> blacklistFetch(
			@Valid @ModelAttribute("bldata") BlacklistData bldata,
			ModelMap model);

	/**
	 * Push the blacklist for a board to the machine.
	 *
	 * @param bldata
	 *            The blacklist data.
	 * @param model
	 *            Overall model
	 * @return the model and view in a future
	 */
	@Async
	@PostMapping(value = BLACKLIST_PATH, params = "push")
	CompletableFuture<ModelAndView> blacklistPush(
			@Valid @ModelAttribute("bldata") BlacklistData bldata,
			ModelMap model);

	/**
	 * Provide the form for uploading a machine definition.
	 *
	 * @return the model and view
	 */
	@GetMapping(MACHINE_PATH)
	ModelAndView machineManagement();

	/**
	 * Handle the change of the tags of a machine.
	 *
	 * @param machineName
	 *            The name of the machine being retagged
	 * @param newTags
	 *            The tags of the machine; comma-separated list
	 * @return the model and view
	 */
	@PostMapping(path = MACHINE_PATH, params = MACHINE_RETAG_PARAM)
	ModelAndView retagMachine(
			@NotEmpty @ModelAttribute("machine") String machineName, @TagList
			@NotNull @ModelAttribute(MACHINE_RETAG_PARAM) String newTags);

	/**
	 * Mark a machine as out of service.
	 *
	 * @param machineName
	 *            The name of the machine being disabled
	 * @return the model and view
	 */
	@PostMapping(value = MACHINE_PATH, params = "outOfService")
	ModelAndView disableMachine(
			@NotEmpty @ModelAttribute("machine") String machineName);

	/**
	 * Mark a machine as in service.
	 *
	 * @param machineName
	 *            The name of the machine being enabled
	 * @return the model and view
	 */
	@PostMapping(value = MACHINE_PATH, params = "intoService")
	ModelAndView enableMachine(
			@NotEmpty @ModelAttribute("machine") String machineName);

	/**
	 * Handle the upload of a machine definition. Note that no user has any
	 * quota set on a newly defined machine; it's totally free to use by
	 * default.
	 *
	 * @param file
	 *            The file being uploaded
	 * @return the model and view
	 */
	@PostMapping(path = MACHINE_PATH)
	ModelAndView defineMachine(
			@NotNull @RequestParam(MACHINE_FILE_PARAM) MultipartFile file);

	/**
	 * The model of a blacklist used by the administration web interface.
	 */
	class BlacklistData {
		private int id;

		private String blacklist;

		private boolean present;

		private boolean synched;

		/** @return The board ID. */
		@Positive
		public int getId() {
			return id;
		}

		/** @param id The board ID. */
		public void setId(int id) {
			this.id = id;
		}

		/** @return The text of the blacklist, if present. */
		public String getBlacklist() {
			return blacklist;
		}

		/** @param blacklist The text of the blacklist. */
		public void setBlacklist(String blacklist) {
			this.blacklist = blacklist;
		}

		/**
		 * @return The parsed blacklist.
		 * @throws NullPointerException
		 *             If no blacklist data is present.
		 * @throws IllegalArgumentException
		 *             If the string is invalid.
		 */
		public Blacklist getParsedBlacklist() {
			return new Blacklist(blacklist);
		}

		/** @return Whether there is blacklist data present. */
		public boolean isPresent() {
			return present;
		}

		/** @param present Whether there is blacklist data present. */
		public void setPresent(boolean present) {
			this.present = present;
		}

		/**
		 * @return Whether the blacklist data is believed to be the same as the
		 *         data on the board.
		 */
		public boolean isSynched() {
			return synched;
		}

		/**
		 * @param synched
		 *            Whether the blacklist data is believed to be the same as
		 *            the data on the board.
		 */
		public void setSynched(boolean synched) {
			this.synched = synched;
		}

		@Keep
		@AssertTrue(message = "blacklist must be validly formatted")
		private boolean isValidBlacklist() {
			if (present) {
				try {
					new Blacklist(blacklist);
				} catch (Exception e) {
					return false;
				}
			}
			return true;
		}
	}
}
