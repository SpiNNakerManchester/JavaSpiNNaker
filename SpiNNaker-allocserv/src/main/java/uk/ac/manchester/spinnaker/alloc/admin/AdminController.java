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

import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_ADMIN;

import java.security.Principal;

import javax.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
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

import uk.ac.manchester.spinnaker.alloc.model.BoardRecord;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.web.SystemController;

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

	/** Path to quota-adjustment operation. */
	String USER_QUOTA_PATH = USER_PATH + "/adjust-quota";

	/** Path to boards operations. */
	String BOARDS_PATH = "/boards";

	/** Path to machine-instantiation operations. */
	String MACHINE_PATH = "/machine";

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
	 * @param result
	 *            Validation results
	 * @param model
	 *            Overall model
	 * @return the model and view
	 */
	@PostMapping(CREATE_USER_PATH)
	ModelAndView createUser(@Valid @ModelAttribute("user") UserRecord user,
			BindingResult result, ModelMap model);

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
	 * @param result
	 *            Validation results
	 * @param model
	 *            Overall model
	 * @param principal
	 *            Who is the admin? Used for safety checks.
	 * @return the model and view
	 */
	@PostMapping(USER_PATH)
	ModelAndView submitUserForm(@PathVariable("id") int id,
			@Valid @ModelAttribute("user") UserRecord user,
			BindingResult result, ModelMap model, Principal principal);

	/**
	 * Delete a user.
	 *
	 * @param id
	 *            The user ID to delete
	 * @param principal
	 *            Who is the admin? Used for safety checks.
	 * @return the model and view
	 */
	@PostMapping(USER_DELETE_PATH)
	ModelAndView deleteUser(@PathVariable("id") int id, Principal principal);

	/**
	 * Adjust the quota of a user.
	 *
	 * @param id
	 *            The user ID to adjust
	 * @param machine
	 *            For which machine are we to adjust the user's quota?
	 * @param delta
	 *            By how much are we to adjust the quota. In
	 *            <em>board-hours</em>.
	 * @return the model and view
	 */
	@PostMapping(USER_QUOTA_PATH)
	ModelAndView adjustQuota(@PathVariable int id, @RequestParam String machine,
			@RequestParam int delta);

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
	 * @param result
	 *            Validation results
	 * @param model
	 *            Overall model
	 * @return the model and view
	 */
	@PostMapping(BOARDS_PATH)
	ModelAndView board(@Valid @ModelAttribute("board") BoardRecord board,
			BindingResult result, ModelMap model);

	/**
	 * Provide the form for uploading a machine definition.
	 *
	 * @return the model and view
	 */
	@GetMapping(MACHINE_PATH)
	ModelAndView machineUploadForm();

	/**
	 * Handle the upload of a machine. Note that no user has any quota set on a
	 * newly defined machine; it's totally free to use by default.
	 *
	 * @param file
	 *            The file being uploaded
	 * @param modelMap
	 *            the model of the form
	 * @return the model and view
	 */
	@PostMapping(MACHINE_PATH)
	ModelAndView defineMachine(@RequestParam("file") MultipartFile file,
			ModelMap modelMap);
}
