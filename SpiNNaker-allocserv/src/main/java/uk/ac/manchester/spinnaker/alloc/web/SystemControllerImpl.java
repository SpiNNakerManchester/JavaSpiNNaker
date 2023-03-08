/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.web;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_READER;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.MAY_SEE_JOB_DETAILS;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.error;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.errorMessage;
import static uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.uri;

import java.security.Principal;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import com.google.errorprone.annotations.Keep;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import uk.ac.manchester.spinnaker.alloc.ServiceConfig.URLPathMaker;
import uk.ac.manchester.spinnaker.alloc.ServiceVersion;
import uk.ac.manchester.spinnaker.alloc.admin.UserControl;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.model.PasswordChangeRecord;
import uk.ac.manchester.spinnaker.alloc.security.AppAuthTransformationFilter;
import uk.ac.manchester.spinnaker.alloc.security.Permit;
import uk.ac.manchester.spinnaker.alloc.web.ControllerUtils.ViewFactory;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * The main web interface controller.
 *
 * @author Donal Fellows
 */
@Controller("mvc.mainUI")
public class SystemControllerImpl implements SystemController {
	private static final Logger log = getLogger(SystemControllerImpl.class);

	private static final ViewFactory MAIN_VIEW = new ViewFactory("index");

	private static final ViewFactory PASSWORD_CHANGE_VIEW =
			new ViewFactory("password");

	private static final ViewFactory MACHINE_LIST_VIEW =
			new ViewFactory("listmachines");

	private static final ViewFactory MACHINE_VIEW =
			new ViewFactory("machinedetails");

	private static final ViewFactory JOB_LIST_VIEW =
			new ViewFactory("listjobs");

	private static final ViewFactory JOB_VIEW = new ViewFactory("jobdetails");

	// Must match what views expect
	private static final String VERSION_OBJ = "version";

	private static final String BUILD_OBJ = "build";

	private static final String JOBS_OBJ = "jobList";

	private static final String ONE_JOB_OBJ = "job";

	private static final String MACHINES_OBJ = "machineList";

	private static final String ONE_MACHINE_OBJ = "machine";

	@Autowired
	private SpallocAPI spallocCore;

	@Autowired
	private LogoutHandler logoutHandler;

	@Autowired
	private UserControl userManager;

	@Autowired
	private URLPathMaker urlMaker;

	@Autowired
	private ServiceVersion version;

	private ModelAndView view(ViewFactory name) {
		var auth = getContext().getAuthentication();
		return name.view(USER_MAY_CHANGE_PASSWORD,
				auth instanceof UsernamePasswordAuthenticationToken);
	}

	private ModelAndView view(ViewFactory name, String key, Object value) {
		var mav = name.view();
		mav.addObject(key, value);
		return mav;
	}

	@Override
	@GetMapping("/")
	public ModelAndView index() {
		return view(MAIN_VIEW).addObject(VERSION_OBJ, version.getFullVersion())
				.addObject(BUILD_OBJ, version.getBuildTimestamp());
	}

	@Override
	@GetMapping("/change_password")
	@Action("preparing for password change")
	public ModelAndView getPasswordChangeForm(Principal principal) {
		return view(PASSWORD_CHANGE_VIEW, USER_PASSWORD_CHANGE_ATTR,
				userManager.getUser(principal));
	}

	/**
	 * Handle {@linkplain Valid invalid} arguments.
	 *
	 * @param result
	 *            The result of validation.
	 * @return How to render this to the user.
	 */
	@Keep
	@ExceptionHandler(BindException.class)
	@UsedInJavadocOnly(Valid.class)
	private ModelAndView bindingError(BindException result) {
		if (result.hasGlobalErrors()) {
			log.debug("binding problem", result);
			return error(errorMessage(result.getGlobalError()));
		} else if (result.hasFieldErrors()) {
			log.debug("binding problem", result);
			return error(errorMessage(result.getFieldError()));
		} else {
			log.error("unknown binding error", result);
			return error("unknown error");
		}
	}

	/**
	 * Handle database and auth exceptions flowing out through this controller.
	 *
	 * @param e
	 *            The exception that happened.
	 * @param hm
	 *            What was happening to cause a problem.
	 * @return View to describe what's going on to the user.
	 */
	@Keep
	@ExceptionHandler({
		AuthenticationException.class, DataAccessException.class
	})
	private ModelAndView dbError(RuntimeException e, HandlerMethod hm) {
		var a = hm.getMethodAnnotation(Action.class);
		String message;
		if (e instanceof AuthenticationException) {
			message = "authentication problem";
			if (nonNull(a)) {
				log.error("auth problem when {}", a.value(), e);
			} else {
				log.error("general authentication problem", e);
			}
		} else if (e instanceof DataAccessException) {
			message = "database problem";
			if (nonNull(a)) {
				log.error("database problem when {}", a.value(), e);
			} else {
				log.error("general database problem", e);
			}
		} else {
			message = "general problem";
			log.error("general problem", e);
		}
		if (new Permit(getContext()).admin) {
			return error(message + ": " + e.getMessage());
		} else {
			return error(message);
		}
	}

	@Override
	@Action("changing password")
	public ModelAndView postPasswordChangeForm(
			PasswordChangeRecord user,
			Principal principal) {
		log.info("changing password for {}", principal.getName());
		return view(PASSWORD_CHANGE_VIEW, USER_PASSWORD_CHANGE_ATTR,
				userManager.updateUser(principal, user));
	}

	@Override
	@Action("logging out")
	public String performLogout(HttpServletRequest request,
			HttpServletResponse response) {
		var auth = getContext().getAuthentication();
		AppAuthTransformationFilter.clearToken(request);
		if (nonNull(auth)) {
			log.info("logging out {}", auth.getPrincipal());
			logoutHandler.logout(request, response, auth);
		}
		return "redirect:" + urlMaker.systemUrl("login.html");
	}

	private static SystemController self() {
		// Do not refactor to a constant; request context aware!
		return on(SystemController.class);
	}

	@Override
	@PreAuthorize(IS_READER)
	@Action("listing machines")
	public ModelAndView getMachineList() {
		var table = spallocCore.listMachines(false);
		table.forEach(rec -> rec
				.setDetailsUrl(uri(self().getMachineInfo(rec.getName()))));
		return view(MACHINE_LIST_VIEW, MACHINES_OBJ, table);
	}

	@Override
	@PreAuthorize(IS_READER)
	@Action("getting machine details")
	public ModelAndView getMachineInfo(String machine) {
		var permit = new Permit(getContext());
		/*
		 * Admins can get the view for disabled machines, but if they know it is
		 * there.
		 */
		var mach = spallocCore
				.getMachineInfo(machine, permit.admin, permit)
				.orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
		// Owners and admins may drill down further into jobs
		mach.getJobs().stream().filter(j -> j.getOwner().isPresent())
				.forEach(j -> j.setUrl(uri(self().getJobInfo(j.getId()))));
		return view(MACHINE_VIEW, ONE_MACHINE_OBJ, mach);
	}

	@Override
	@PreAuthorize(IS_READER)
	@Action("listing jobs")
	public ModelAndView getJobList() {
		var table = spallocCore.listJobs(new Permit(getContext()));
		table.forEach(entry -> {
			entry.setDetailsUrl(uri(self().getJobInfo(entry.getId())));
			entry.setMachineUrl(
					uri(self().getMachineInfo(entry.getMachineName())));
		});
		return view(JOB_LIST_VIEW, JOBS_OBJ, table);
	}

	@Override
	@PreAuthorize(IS_READER)
	@Action("getting job details")
	public ModelAndView getJobInfo(int id) {
		var permit = new Permit(getContext());
		var mach = spallocCore.getJobInfo(permit, id)
				.orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
		if (nonNull(mach.getRequestBytes())) {
			mach.setRequest(new String(mach.getRequestBytes(), UTF_8));
		}
		mach.setMachineUrl(uri(self().getMachineInfo(mach.getMachine())));
		var mav = view(JOB_VIEW, ONE_JOB_OBJ, mach);
		mav.addObject("deleteUri", uri(self().destroyJob(id, null)));
		return mav;
	}

	@Override
	@PreAuthorize(MAY_SEE_JOB_DETAILS)
	@Action("deleting job")
	public ModelAndView destroyJob(int id, String reason) {
		var permit = new Permit(getContext());
		var job = spallocCore.getJob(permit, id)
				.orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
		job.destroy(reason);
		var mach = spallocCore.getJobInfo(permit, id)
				.orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
		if (nonNull(mach.getRequestBytes())) {
			mach.setRequest(new String(mach.getRequestBytes(), UTF_8));
		}
		mach.setMachineUrl(uri(self().getMachineInfo(mach.getMachine())));
		return view(JOB_VIEW, ONE_JOB_OBJ, mach);
	}
}
