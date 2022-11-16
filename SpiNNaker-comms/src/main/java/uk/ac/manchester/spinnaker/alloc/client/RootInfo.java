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
