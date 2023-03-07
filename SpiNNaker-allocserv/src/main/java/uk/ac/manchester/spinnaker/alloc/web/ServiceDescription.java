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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH;

import java.net.URI;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.springframework.security.web.csrf.CsrfToken;

import com.fasterxml.jackson.annotation.JsonInclude;

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
