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
import java.sql.SQLException;

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
import org.springframework.web.servlet.ModelAndView;

import uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.User;

@RequestMapping("/")
@PreAuthorize(IS_ADMIN)
public interface AdminController {

	@GetMapping("/users")
	ModelAndView listUsers() throws SQLException;

	@PostMapping("/users")
	ModelAndView createUser(@Valid @ModelAttribute("user") User user,
			BindingResult result, ModelMap model) throws SQLException;

	@GetMapping("/users/{id}")
	ModelAndView showUserForm(@PathVariable int id) throws SQLException;

	@PostMapping("/users/{id}")
	ModelAndView submitUserForm(@PathVariable int id,
			@Valid @ModelAttribute("user") User user, BindingResult result,
			ModelMap model, Principal principal) throws SQLException;

	@PostMapping("/users/{id}/delete")
	ModelAndView deleteUser(@PathVariable int id,
			@Valid @ModelAttribute("user") User user, BindingResult result,
			ModelMap model, Principal principal) throws SQLException;

	@GetMapping("/boards")
	ModelAndView boards(ModelMap model);

	@PostMapping("/boards")
	ModelAndView board(@Valid @ModelAttribute("board") BoardInfo board,
			BindingResult result, ModelMap model) throws SQLException;

	class BoardInfo {
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
