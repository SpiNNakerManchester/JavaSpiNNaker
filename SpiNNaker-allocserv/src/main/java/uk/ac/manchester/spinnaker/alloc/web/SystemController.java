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

import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_READER;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import uk.ac.manchester.spinnaker.alloc.model.PasswordChangeRecord;

/**
 * The main web interface controller.
 *
 * @author Donal Fellows
 */
@RequestMapping(SystemController.ROOT_PATH)
public interface SystemController {
	/** The root path to the controller within the overall application. */
	String ROOT_PATH = "/system";

	/** The name of the main attribute supporting a password change form. */
	String USER_PASSWORD_CHANGE_ATTR = "user";

	/**
	 * The name of the boolean view attribute describing whether the password
	 * change form may be used. Only local users may change their password!
	 */
	String USER_MAY_CHANGE_PASSWORD = "userMayChangePassword";

	/**
	 * Get the view for the main page of the service.
	 *
	 * @return View name
	 */
	@GetMapping("/")
	ModelAndView index();

	/**
	 * Get the view for the general machine list.
	 *
	 * @return View and model
	 */
	@GetMapping("/list_machines")
	@PreAuthorize(IS_READER)
	ModelAndView getMachineList();

	/**
	 * Get the view for some machine details.
	 *
	 * @param machine
	 *            Which machine is being asked for
	 * @return View and model
	 */
	@GetMapping("/machine_info/{machine}")
	@PreAuthorize(IS_READER)
	ModelAndView getMachineInfo(@PathVariable("machine") String machine);

	/**
	 * Get the view for the general job list.
	 *
	 * @return View and model
	 */
	@GetMapping("/list_jobs")
	@PreAuthorize(IS_READER)
	ModelAndView getJobList();

	/**
	 * Get the view for some job details.
	 *
	 * @param id
	 *            Which job is being asked for
	 * @return View and model
	 */
	@GetMapping("/job_info/{id}")
	@PreAuthorize(IS_READER)
	ModelAndView getJobInfo(@PathVariable("id") int id);

	/**
	 * Get the view and model for the password change form.
	 *
	 * @param principal
	 *            Who is changing their password.
	 * @return View and model
	 */
	@GetMapping("/change_password")
	ModelAndView getPasswordChangeForm(Principal principal);

	/**
	 * Carry out a password change.
	 *
	 * @param user
	 *            The description of what to change.
	 * @param result
	 *            Description of whether the {@code user} is actually valid.
	 * @param principal
	 *            Who is changing their password.
	 * @return View and model
	 */
	@PostMapping("/change_password")
	ModelAndView postPasswordChangeForm(
			@ModelAttribute(USER_PASSWORD_CHANGE_ATTR)
			@Valid PasswordChangeRecord user, BindingResult result,
			Principal principal);

	/**
	 * Log the user out.
	 *
	 * @param request
	 *            What request was made
	 * @param response
	 *            What response to build
	 * @return Redirect to the login page
	 * @see LogoutHandler
	 */
	@GetMapping("/perform_logout")
	String performLogout(HttpServletRequest request,
			HttpServletResponse response);
}
