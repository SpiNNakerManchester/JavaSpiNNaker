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
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequestUri;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_ADMIN;

import java.net.URI;
import java.security.Principal;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.User;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;

@Controller("mvc.adminController")
@RequestMapping("/")
@PreAuthorize(IS_ADMIN)
public class AdminControllerImpl implements AdminController {
	@Autowired
	private UserControl userController;

	@Autowired
	private MachineStateControl machineController;

	private ModelAndView addStandardContext(ModelAndView mav) {
		mav.addObject("baseuri", fromCurrentRequestUri().toUriString());
		mav.addObject("trustLevels", TrustLevel.values());
		return mav;
	}

	@Override
	@GetMapping("/users")
	public ModelAndView listUsers() throws SQLException {
		Map<String, URI> result = new TreeMap<>();
		AdminController controller = on(AdminController.class);
		for (User user : userController.listUsers()) {
			result.put(user.getUserName(),
					fromMethodCall(controller.showUserForm(user.getUserId()))
							.buildAndExpand().toUri());
		}

		ModelAndView mav = new ModelAndView("users");
		mav.addObject("userlist", unmodifiableMap(result));
		mav.addObject("user", new User());
		return addStandardContext(mav);
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@Override
	@GetMapping("/boards")
	public ModelAndView boards(ModelMap model) {
		ModelAndView mav = new ModelAndView("board", model);
		mav.addObject("board", new BoardInfo());
		return mav;
	}

	@Override
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
}
