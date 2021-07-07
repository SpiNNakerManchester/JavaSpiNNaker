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

import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_ADMIN;

import java.security.Principal;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

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

import uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.User;

/**
 * The API for the controller for the admin user interface.
 *
 * @author Donal Fellows
 */
@RequestMapping(AdminController.BASE_PATH)
@PreAuthorize(IS_ADMIN)
public interface AdminController {
	/** The base path of this interface within the MVC domain. */
	String BASE_PATH = "/admin";

	/** Path to all-users operations. */
	String USERS_PATH = "/users";

	/** Path to single-user operations. */
	String USER_PATH = USERS_PATH + "/{id}";

	/** Path to single-user-deletion operation. */
	String USER_DELETE_PATH = USER_PATH + "/delete";

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
	@PostMapping(USERS_PATH)
	ModelAndView createUser(@Valid @ModelAttribute("user") User user,
			BindingResult result, ModelMap model);

	/**
	 * Show user details.
	 *
	 * @param id
	 *            The user ID
	 * @return the model and view
	 */
	@GetMapping(USER_PATH)
	ModelAndView showUserForm(@PathVariable int id);

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
	ModelAndView submitUserForm(@PathVariable int id,
			@Valid @ModelAttribute("user") User user, BindingResult result,
			ModelMap model, Principal principal);

	/**
	 * Delete a user.
	 *
	 * @param id
	 *            The user ID
	 * @param user
	 *            The description of the user to delete
	 * @param result
	 *            Validation results
	 * @param model
	 *            Overall model
	 * @param principal
	 *            Who is the admin? Used for safety checks.
	 * @return the model and view
	 */
	@PostMapping(USER_DELETE_PATH)
	ModelAndView deleteUser(@PathVariable int id,
			@Valid @ModelAttribute("user") User user, BindingResult result,
			ModelMap model, Principal principal);

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
	ModelAndView board(@Valid @ModelAttribute("board") BoardModel board,
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

	/**
	 * Model of a board, for configuration purposes.
	 *
	 * @author Donal Fellows
	 */
	class BoardModel {
		@NotNull
		private String machineName;

		private Integer x;

		private Integer y;

		private Integer z;

		private Integer cabinet;

		private Integer frame;

		private Integer board;

		private String ipAddress;

		private Boolean enabled;

		/**
		 * @return the machine name
		 */
		public String getMachineName() {
			return machineName;
		}

		public void setMachineName(String machineName) {
			this.machineName = machineName;
		}

		/**
		 * @return the board X coordinate
		 */
		public Integer getX() {
			return x;
		}

		public void setX(Integer x) {
			this.x = x;
		}

		/**
		 * @return the board Y coordinate
		 */
		public Integer getY() {
			return y;
		}

		public void setY(Integer y) {
			this.y = y;
		}

		/**
		 * @return the board Z coordinate
		 */
		public Integer getZ() {
			return z;
		}

		public void setZ(Integer z) {
			this.z = z;
		}

		public boolean isTriadCoordPresent() {
			return x != null && y != null && z != null;
		}

		/**
		 * @return the cabinet number
		 */
		public Integer getCabinet() {
			return cabinet;
		}

		public void setCabinet(Integer cabinet) {
			this.cabinet = cabinet;
		}

		/**
		 * @return the frame number
		 */
		public Integer getFrame() {
			return frame;
		}

		public void setFrame(Integer frame) {
			this.frame = frame;
		}

		/**
		 * @return the board number
		 */
		public Integer getBoard() {
			return board;
		}

		public void setBoard(Integer board) {
			this.board = board;
		}

		public boolean isPhysicalCoordPresent() {
			return cabinet != null && frame != null && board != null;
		}

		/**
		 * @return the board's IP address
		 */
		public String getIpAddress() {
			return ipAddress;
		}

		public void setIpAddress(String ipAddress) {
			this.ipAddress = ipAddress;
		}

		public boolean isAddressPresent() {
			return ipAddress != null;
		}

		@AssertTrue
		boolean isValidBoardLocator() {
			return (machineName != null) && (this.isTriadCoordPresent()
					|| this.isPhysicalCoordPresent()
					|| this.isAddressPresent());
		}

		/**
		 * @return whether the board is enabled
		 */
		public Boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}
	}
}
