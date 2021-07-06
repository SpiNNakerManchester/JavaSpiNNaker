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

@Controller
@PreAuthorize(IS_ADMIN)
public class AdminController {
	@Autowired
	private UserControl userController;

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
				mav = new ModelAndView("user");
				mav.addObject("user", updatedUser.get().sanitise());
			}
		}
		return addStandardContext(mav);
	}
}
