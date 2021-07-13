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
package uk.ac.manchester.spinnaker.alloc.web;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_READER;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.MVC_ERROR;

import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import uk.ac.manchester.spinnaker.alloc.admin.UserControl;
import uk.ac.manchester.spinnaker.alloc.model.JobListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.model.MachineListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.model.PasswordChangeRecord;

/**
 * The main web interface controller.
 *
 * @author Donal Fellows
 */
@Controller
@RequestMapping("/")
public class RootControllerImpl implements RootController {
	private static final Logger log = getLogger(RootControllerImpl.class);

	private static final String MAIN_VIEW = "index";

	private static final String PASSWORD_CHANGE_VIEW = "password";

	private static final String MACHINE_LIST_VIEW = "machinelist";

	private static final String JOB_LIST_VIEW = "joblist";

	@Autowired
	private LogoutHandler logoutHandler;

	@Autowired
	private UserControl userControl;

	@Override
	@GetMapping("/")
	public String index() {
		return MAIN_VIEW;
	}

	@Override
	@GetMapping("/change_password")
	public ModelAndView getPasswordChangeForm(Principal principal) {
		ModelAndView mav = new ModelAndView(PASSWORD_CHANGE_VIEW);
		try {
			mav.addObject(USER_PASSWORD_CHANGE_ATTR,
					userControl.getUserForPrincipal(principal));
		} catch (AuthenticationException | SQLException e) {
			return new ModelAndView(MVC_ERROR);
		}
		return mav;
	}

	@Override
	@PostMapping("/change_password")
	public ModelAndView postPasswordChangeForm(
			@Valid @ModelAttribute("user") PasswordChangeRecord user,
			BindingResult result, Principal principal) {
		if (result.hasErrors()) {
			return new ModelAndView(MVC_ERROR);
		}
		log.info("changing password for {}", principal.getName());
		ModelAndView mav = new ModelAndView(PASSWORD_CHANGE_VIEW);
		try {
			mav.addObject(USER_PASSWORD_CHANGE_ATTR,
					userControl.updateUserOfPrincipal(principal, user));
		} catch (AuthenticationException | SQLException e) {
			return new ModelAndView(MVC_ERROR);
		}
		return mav;
	}

	@Override
	@GetMapping("/perform_logout")
	public String performLogout(HttpServletRequest request,
			HttpServletResponse response) {
		Authentication auth = getContext().getAuthentication();
		if (auth != null) {
			log.info("logging out {}", auth.getPrincipal());
			logoutHandler.logout(request, response, auth);
		}
		return "redirect:/system/login.html";
	}

	@Override
	@PreAuthorize(IS_READER)
	public ModelAndView getMachineList() {
		List<MachineListEntryRecord> table = new ArrayList<>();
		// TODO populate the table of machines
		return new ModelAndView(MACHINE_LIST_VIEW, "machineList", table);
	}

	@Override
	@PreAuthorize(IS_READER)
	public ModelAndView getJobList() {
		List<JobListEntryRecord> table = new ArrayList<>();
		// TODO build info for table
		return new ModelAndView(JOB_LIST_VIEW, "jobList", table);
	}
}
