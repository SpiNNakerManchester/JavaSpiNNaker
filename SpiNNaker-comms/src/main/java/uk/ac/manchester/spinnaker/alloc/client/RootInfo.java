/*
 * Copyright (c) 2021-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonAlias;

import uk.ac.manchester.spinnaker.messages.model.Version;

class RootInfo {
	/** Service version. */
	Version version;

	/** Where to look up jobs. */
	URI jobsURI;

	/** Where to look up machines. */
	URI machinesURI;

	/** CSRF header name. */
	String csrfHeader;

	/** CSRF token value. */
	String csrfToken;

	public void setVersion(Version version) {
		this.version = version;
	}

	@JsonAlias("jobs-ref")
	public void setJobsURI(URI jobsURI) {
		this.jobsURI = jobsURI;
	}

	@JsonAlias("machines-ref")
	public void setMachinesURI(URI machinesURI) {
		this.machinesURI = machinesURI;
	}

	@JsonAlias("csrf-header")
	public void setCsrfHeader(String csrfHeader) {
		this.csrfHeader = csrfHeader;
	}

	@JsonAlias("csrf-token")
	public void setCsrfToken(String csrfToken) {
		this.csrfToken = csrfToken;
	}
}
