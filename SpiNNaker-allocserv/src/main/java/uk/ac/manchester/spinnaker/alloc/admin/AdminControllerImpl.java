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

import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.User;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;

@Controller("mvc.adminController")
@RequestMapping("/admin")
@PreAuthorize(IS_ADMIN)
public class AdminControllerImpl implements AdminController {
	private static final String USER_LIST_VIEW = "users";

	private static final String USER_DETAILS_VIEW = "user";

	private static final String ERROR_VIEW = "error";

	private static final String BOARD_VIEW = "board";

	private static final String USER_OBJ = "user";

	private static final String BOARD_OBJ = "board";

	@Autowired
	private UserControl userController;

	@Autowired
	private MachineStateControl machineController;

	@Autowired
	private MachineDefinitionLoader machineDefiner;

	/**
	 * Special delegate for building URIs only.
	 *
	 * @see MvcUriComponentsBuilder#fromMethodCall(Object)
	 */
	private static final AdminController SELF = on(AdminController.class);

	private static URI uri(Object selfCall) {
		// No template variables in the overall controller, so can factor out
		return fromMethodCall(selfCall).buildAndExpand().toUri();
	}

	private static ModelAndView addStandardContext(ModelAndView mav) {
		mav.addObject("baseuri", fromCurrentRequestUri().toUriString());
		mav.addObject("trustLevels", TrustLevel.values());
		mav.addObject("usersUri", uri(SELF.listUsers()));
		mav.addObject("boardsUri", uri(SELF.boards()));
		mav.addObject("machineUri", uri(SELF.machineUploadForm()));
		return mav;
	}

	private static ModelAndView addStandardContext(String viewName) {
		return addStandardContext(new ModelAndView("viewName"));
	}

	private static ModelAndView errors(BindingResult result) {
		if (result.hasGlobalErrors()) {
			return addStandardContext(new ModelAndView(ERROR_VIEW, "error",
					result.getGlobalError().toString()));
		}
		if (result.hasFieldErrors()) {
			return addStandardContext(new ModelAndView(ERROR_VIEW, "error",
					result.getFieldError().toString()));
		}
		return addStandardContext(new ModelAndView(ERROR_VIEW));
	}

	private static ModelAndView errors(String message) {
		return addStandardContext(
				new ModelAndView(ERROR_VIEW, "error", message));
	}

	@Override
	public ModelAndView mainUI() {
		return addStandardContext("main");
	}

	@Override
	@GetMapping(USERS_PATH)
	public ModelAndView listUsers() {
		Map<String, URI> result = new TreeMap<>();
		try {
			for (User user : userController.listUsers()) {
				result.put(user.getUserName(),
						uri(SELF.showUserForm(user.getUserId())));
			}
		} catch (SQLException e) {
			return errors("database access failed: " + e.getMessage());
		}

		ModelAndView mav = new ModelAndView(USER_LIST_VIEW);
		mav.addObject("userlist", unmodifiableMap(result));
		mav.addObject(USER_OBJ, new User());
		return addStandardContext(mav);
	}

	@Override
	@PostMapping(USERS_PATH)
	public ModelAndView createUser(@Valid @ModelAttribute(USER_OBJ) User user,
			BindingResult result, ModelMap model) {
		if (result.hasErrors()) {
			return errors(result);
		}
		user.initCreationDefaults();
		Optional<User> realUser;
		try {
			realUser = userController.createUser(user);
		} catch (SQLException e) {
			return errors("database access failed: " + e.getMessage());
		}
		if (!realUser.isPresent()) {
			return errors("creation failed");
		}
		ModelAndView mav = new ModelAndView(USER_LIST_VIEW);
		mav.addObject("user", realUser.get().sanitise());
		return addStandardContext(mav);
	}

	@Override
	@GetMapping(USER_PATH)
	public ModelAndView showUserForm(@PathVariable int id) {
		try {
			Optional<User> user = userController.getUser(id);
			if (!user.isPresent()) {
				return errors("no such user");
			}
			ModelAndView mav = new ModelAndView(USER_DETAILS_VIEW);
			User realUser = user.get().sanitise();
			mav.addObject(USER_OBJ, realUser);
			mav.addObject("deleteUri",
					uri(SELF.deleteUser(id, realUser, null, null, null)));
			return addStandardContext(mav);
		} catch (SQLException e) {
			return errors("database access failed: " + e.getMessage());
		}
	}

	@Override
	@PostMapping(USER_PATH)
	public ModelAndView submitUserForm(@PathVariable int id,
			@Valid @ModelAttribute(USER_OBJ) User user, BindingResult result,
			ModelMap model, Principal principal) {
		if (result.hasErrors()) {
			return errors(result);
		}
		String adminUser = principal.getName();
		user.setUserId(null);
		Optional<User> updatedUser;
		try {
			updatedUser = userController.updateUser(id, user, adminUser);
		} catch (SQLException e) {
			return errors("database access failed: " + e.getMessage());
		}
		if (!updatedUser.isPresent()) {
			return errors("no such user");
		}
		ModelAndView mav = new ModelAndView(USER_DETAILS_VIEW, model);
		mav.addObject(USER_OBJ, updatedUser.get().sanitise());
		return addStandardContext(mav);
	}

	@Override
	@PostMapping(USER_DELETE_PATH)
	public ModelAndView deleteUser(@PathVariable int id,
			@Valid @ModelAttribute(USER_OBJ) User user, BindingResult result,
			ModelMap model, Principal principal) {
		if (result.hasErrors()) {
			return errors(result);
		}
		String adminUser = principal.getName();
		try {
			if (!userController.deleteUser(id, adminUser).isPresent()) {
				return errors("could not delete that user");
			}
			URI target = uri(SELF.listUsers());
			ModelAndView mav = new ModelAndView("redirect:" + target.getPath());
			// Not sure that these are the correct place
			mav.addObject("notice", "deleted " + user.getUserName());
			mav.addObject(USER_OBJ, new User());
			return addStandardContext(mav);
		} catch (SQLException e) {
			return errors("database access failed: " + e.getMessage());
		}
	}

	@Override
	@GetMapping(BOARDS_PATH)
	public ModelAndView boards() {
		ModelAndView mav = new ModelAndView(BOARD_VIEW);
		mav.addObject(BOARD_OBJ, new BoardModel());
		return mav;
	}

	@Override
	@PostMapping(BOARDS_PATH)
	public ModelAndView board(
			@Valid @ModelAttribute(BOARD_OBJ) BoardModel board,
			BindingResult result, ModelMap model) {
		ModelAndView mav = new ModelAndView(BOARD_VIEW, model);
		if (result.hasErrors()) {
			return errors(result);
		}
		BoardState bs = null;
		try {
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
			} else {
				return errors("bad address");
			}
		} catch (SQLException e) {
			return errors("database access failed: " + e.getMessage());
		}
		if (bs == null) {
			return errors("no such board");
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
		try {
			if (board.isEnabled() == null) {
				board.setEnabled(bs.getState());
			} else {
				bs.setState(board.isEnabled());
			}
		} catch (SQLException e) {
			return errors("database access failed: " + e.getMessage());
		}
		model.put(BOARD_OBJ, bs);
		return mav;
	}

	@Override
	@GetMapping(MACHINE_PATH)
	public ModelAndView machineUploadForm() {
		return addStandardContext("machine");
	}

	@Override
	@PostMapping(MACHINE_PATH)
	public ModelAndView defineMachine(@RequestParam("file") MultipartFile file,
			ModelMap modelMap) {
		try (InputStream input = file.getInputStream()) {
			machineDefiner.loadMachineDefinitions(input);
		} catch (IOException | SQLException e) {
			return errors("problem with processing file: " + e.getMessage());
		}
		ModelAndView mav = new ModelAndView("machine");
		// Not sure what attributes to put here
		return addStandardContext(mav);
	}
}
