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

import static java.util.Collections.unmodifiableMap;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequestUri;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_ADMIN;

import java.security.Principal;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.User;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;

@Controller
@PreAuthorize(IS_ADMIN)
public class AdminController {
	@Autowired
	private UserControl userController;

	@Autowired
	private MachineStateControl machineController;

	private ModelAndView addStandardContext(ModelAndView mav) {
		mav.addObject("baseuri", fromCurrentRequestUri().toUriString());
		mav.addObject("trustLevels", TrustLevel.values());
		return mav;
	}

	@GetMapping("/users")
	public ModelAndView listUsers() throws SQLException {
		Map<String, Integer> result = new TreeMap<>();
		for (User user : userController.listUsers()) {
			result.put(user.getUserName(), user.getUserId());
		}

		ModelAndView mav = new ModelAndView("users");
		mav.addObject("userlist", unmodifiableMap(result));
		mav.addObject("user", new User());
		return addStandardContext(mav);
	}

	@PostMapping("/users")
	public ModelAndView createUser(@Valid @ModelAttribute("user") User user,
			BindingResult result, ModelMap model) throws SQLException {
		ModelAndView mav;
		if (result.hasErrors()) {
			mav = new ModelAndView("error");
		} else {
			user.initCreationDefaults();
			Optional<User> realUser = userController.createUser(user);
			if (realUser.isPresent()) {
				mav = new ModelAndView("users");
			} else {
				mav = new ModelAndView("error");
			}
		}
		return addStandardContext(mav);
	}

	@GetMapping("/users/{id}")
	public ModelAndView showUserForm(@PathVariable int id) throws SQLException {
		// return "formtest";
		ModelAndView mav;
		Optional<User> user = userController.getUser(id);
		if (user.isPresent()) {
			mav = new ModelAndView("user");
			mav.addObject("user", user.get().sanitise());
		} else {
			mav = new ModelAndView("error");
		}
		return addStandardContext(mav);
	}

	@PostMapping("/users/{id}")
	public ModelAndView submitUserForm(@PathVariable int id,
			@Valid @ModelAttribute("user") User user, BindingResult result,
			ModelMap model, Principal principal) throws SQLException {
		ModelAndView mav;
		if (result.hasErrors()) {
			mav = new ModelAndView("error");
		} else {
			String adminUser = principal.getName();
			user.setUserId(null);
			Optional<User> updatedUser =
					userController.updateUser(id, user, adminUser);
			if (!updatedUser.isPresent()) {
				mav = new ModelAndView("error");
			} else {
				mav = new ModelAndView("user", model);
				mav.addObject("user", updatedUser.get().sanitise());
			}
		}
		return addStandardContext(mav);
	}

	@PostMapping("/users/{id}/delete")
	public ModelAndView deleteUser(@PathVariable int id,
			@Valid @ModelAttribute("user") User user, BindingResult result,
			ModelMap model, Principal principal) throws SQLException {
		ModelAndView mav;
		if (result.hasErrors()) {
			mav = new ModelAndView("error");
		} else {
			String adminUser = principal.getName();
			if (userController.deleteUser(id, adminUser).isPresent()) {
				mav = new ModelAndView("redirect:/users");
				mav.addObject("notice", "deleted " + user.getUserName());
				mav.addObject("user", new User());
			} else {
				mav = new ModelAndView("error");
			}
		}
		return addStandardContext(mav);
	}

	@GetMapping("/boards")
	public ModelAndView boards(ModelMap model) {
		ModelAndView mav = new ModelAndView("board", model);
		mav.addObject("board", new BoardInfo());
		return mav;
	}

	@PostMapping("/boards")
	public ModelAndView board(@Valid @ModelAttribute("board") BoardInfo board,
			BindingResult result, ModelMap model) throws SQLException {
		ModelAndView mav = new ModelAndView("board", model);
		if (result.hasErrors()) {
			return new ModelAndView("error");
		}
		BoardState bs = null;
		if (board.isTriadCoordPresent()) {
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
		}
		if (bs == null) {
			return new ModelAndView("error");
		}

		// Inflate the coordinates
		board.setX(bs.x);
		board.setY(bs.y);
		board.setZ(bs.z);
		board.setCabinet(bs.cabinet);
		board.setFrame(bs.frame);
		board.setBoard(bs.board);
		board.setIpAddress(bs.address);

		// Get or set
		if (board.isEnabled() == null) {
			board.setEnabled(bs.getState());
		} else {
			bs.setState(board.isEnabled());
		}
		model.put("board", bs);
		return mav;
	}

	public static class BoardInfo {
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
			return machineName != null && this.isTriadCoordPresent()
					|| this.isPhysicalCoordPresent() || this.isAddressPresent();
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
