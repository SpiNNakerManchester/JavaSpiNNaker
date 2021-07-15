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
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_READER;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.MVC_ERROR;

import java.net.URI;
import java.security.Principal;
import java.sql.SQLException;
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
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import uk.ac.manchester.spinnaker.alloc.admin.UserControl;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
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

	private static final String MACHINE_LIST_VIEW = "listmachines";

	private static final String MACHINE_VIEW = "machinedetails";

	private static final String JOB_LIST_VIEW = "listjobs";

	/**
	 * Special delegate for building URIs only.
	 *
	 * @see MvcUriComponentsBuilder#fromMethodCall(Object)
	 */
	private static final RootController SELF = on(RootController.class);

	@Autowired
	private SpallocAPI spallocCore;

	@Autowired
	private LogoutHandler logoutHandler;

	@Autowired
	private UserControl userControl;

	private static URI uri(Object selfCall) {
		// No template variables in the overall controller, so can factor out
		return fromMethodCall(selfCall).buildAndExpand().toUri();
	}

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
		try {
			List<MachineListEntryRecord> table = spallocCore.listMachines();
			for (MachineListEntryRecord entry : table) {
				entry.setDetailsUrl(
						uri(SELF.getMachineInfo(entry.getName(), null)));
			}
			return new ModelAndView(MACHINE_LIST_VIEW, "machineList", table);
		} catch (SQLException e) {
			log.error("database problem", e);
			return new ModelAndView(MVC_ERROR);
		}
	}

	private static boolean isAdmin() {
		return getContext().getAuthentication().getAuthorities()
				.stream().anyMatch(g -> g.getAuthority().endsWith("ADMIN"));
	}

	@Override
	@PreAuthorize(IS_READER)
	public ModelAndView getMachineInfo(String machine, Principal principal) {
		try {
			return new ModelAndView(MACHINE_VIEW, "machine", spallocCore
					.getMachineInfo(machine, principal.getName(), isAdmin()));
		} catch (SQLException e) {
			// TODO Auto-generated method stub
			return new ModelAndView(MVC_ERROR);
		}
	}

	@Override
	@PreAuthorize(IS_READER)
	public ModelAndView getJobList(Principal principal) {
		try {
			List<JobListEntryRecord> table =
					spallocCore.listJobs(principal.getName(), isAdmin());
			for (JobListEntryRecord entry : table) {
				entry.setDetailsUrl(uri(SELF.getJobInfo(entry.getId(), null)));
				entry.setMachineUrl(
						uri(SELF.getMachineInfo(entry.getMachineName(), null)));
			}
			return new ModelAndView(JOB_LIST_VIEW, "jobList", table);
		} catch (SQLException e) {
			log.error("database problem", e);
			return new ModelAndView(MVC_ERROR);
		}
	}

	@Override
	@PreAuthorize(IS_READER)
	public ModelAndView getJobInfo(int id, Principal principal) {
		// TODO Auto-generated method stub
		return new ModelAndView(MVC_ERROR);
	}
}
