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
package uk.ac.manchester.spinnaker.alloc.web;

import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_READER;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import uk.ac.manchester.spinnaker.alloc.model.JobDescription;
import uk.ac.manchester.spinnaker.alloc.model.JobListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.model.MachineDescription;
import uk.ac.manchester.spinnaker.alloc.model.MachineListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.model.PasswordChangeRecord;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

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
	 * @return View ({@code index.jsp})
	 */
	@GetMapping("/")
	ModelAndView index();

	/**
	 * Get the view for the general machine list.
	 *
	 * @return View ({@code listmachines.jsp}) and model (based on
	 *         {@link MachineListEntryRecord})
	 */
	@GetMapping("/list_machines")
	@PreAuthorize(IS_READER)
	@UsedInJavadocOnly(MachineListEntryRecord.class)
	ModelAndView getMachineList();

	/**
	 * Get the view for some machine details.
	 *
	 * @param machine
	 *            Which machine is being asked for
	 * @return View ({@code machinedetails.jsp}) and model (based on
	 *         {@link MachineDescription})
	 */
	@GetMapping("/machine_info/{machine}")
	@PreAuthorize(IS_READER)
	@UsedInJavadocOnly(MachineDescription.class)
	ModelAndView getMachineInfo(@PathVariable("machine") String machine);

	/**
	 * Get the view for the general job list.
	 *
	 * @return View ({@code listjobs.jsp}) and model (based on
	 *         {@link JobListEntryRecord})
	 */
	@GetMapping("/list_jobs")
	@PreAuthorize(IS_READER)
	@UsedInJavadocOnly(JobListEntryRecord.class)
	ModelAndView getJobList();

	/**
	 * Get the view for some job details.
	 *
	 * @param id
	 *            Which job is being asked for
	 * @return View ({@code jobdetails.jsp}) and model (based on
	 *         {@link JobDescription})
	 */
	@GetMapping("/job_info/{id}")
	@PreAuthorize(IS_READER)
	@UsedInJavadocOnly(JobDescription.class)
	ModelAndView getJobInfo(@PathVariable("id") int id);

	/**
	 * Get the view and model for the password change form.
	 *
	 * @param principal
	 *            Who is changing their password.
	 * @return View ({@code password.jsp}) and model (based on
	 *         {@link PasswordChangeRecord})
	 */
	@GetMapping("/change_password")
	ModelAndView getPasswordChangeForm(Principal principal);

	/**
	 * Carry out a password change.
	 *
	 * @param user
	 *            The description of what to change.
	 * @param principal
	 *            Who is changing their password.
	 * @return View ({@code password.jsp}) and model (based on
	 *         {@link PasswordChangeRecord})
	 */
	@PostMapping("/change_password")
	ModelAndView
			postPasswordChangeForm(@ModelAttribute(USER_PASSWORD_CHANGE_ATTR)
			@Valid PasswordChangeRecord user, Principal principal);

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
	@UsedInJavadocOnly(LogoutHandler.class)
	String performLogout(HttpServletRequest request,
			HttpServletResponse response);
}
