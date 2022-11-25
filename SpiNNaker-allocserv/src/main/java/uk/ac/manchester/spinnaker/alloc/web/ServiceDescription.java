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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH;

import java.net.URI;

import org.springframework.security.web.csrf.CsrfToken;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import uk.ac.manchester.spinnaker.messages.model.Version;

/**
 * The description of the overall service.
 *
 * @author Donal Fellows
 */
public class ServiceDescription {
	private Version version;

	@JsonInclude(NON_NULL)
	private URI jobsRef;

	@JsonInclude(NON_NULL)
	private URI machinesRef;

	@JsonInclude(NON_NULL)
	private String csrfHeader;

	@JsonInclude(NON_NULL)
	private String csrfToken;

	/** Create an instance. */
	public ServiceDescription() {
	}

	ServiceDescription(Version version, UriInfo ui, SecurityContext sec,
			CsrfToken token) {
		this.version = version;
		if (sec.isUserInRole("READER")) {
			var ub = ui.getAbsolutePathBuilder().path("{resource}");
			jobsRef = ub.build(JOB);
			machinesRef = ub.build(MACH);
		}
		if (nonNull(token)) {
			csrfHeader = token.getHeaderName();
			csrfToken = token.getToken();
		}
	}

	/** @return The service version */
	public Version getVersion() {
		return version;
	}

	void setVersion(Version version) {
		this.version = version;
	}

	/** @return Where to work with jobs */
	public URI getJobsRef() {
		return jobsRef;
	}

	void setJobsRef(URI jobsRef) {
		this.jobsRef = jobsRef;
	}

	/** @return Where to work with machines */
	public URI getMachinesRef() {
		return machinesRef;
	}

	void setMachinesRef(URI machinesRef) {
		this.machinesRef = machinesRef;
	}

	/**
	 * @return the name of the HTTP header to pass the CSRF token in
	 */
	public String getCsrfHeader() {
		return csrfHeader;
	}

	/**
	 * @return the CSRF token
	 */
	public String getCsrfToken() {
		return csrfToken;
	}
}
