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

import java.net.URI;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.messages.model.Version;

/**
 * The description of the overall service.
 *
 * @author Donal Fellows
 */
public class ServiceDescription {
	private Version version;

	private URI jobsRef;

	private URI machinesRef;

	public ServiceDescription() {
	}

	ServiceDescription(Version version, UriInfo ui) {
		this.version = version;
		UriBuilder ub = ui.getAbsolutePathBuilder().path("{resource}");
		jobsRef = ub.build("jobs");
		machinesRef = ub.build("machines");
	}

	/** @return The service version */
	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	/** @return Where to work with jobs */
	public URI getJobsRef() {
		return jobsRef;
	}

	public void setJobsRef(URI jobsRef) {
		this.jobsRef = jobsRef;
	}

	/** @return Where to work with machines */
	public URI getMachinesRef() {
		return machinesRef;
	}

	public void setMachinesRef(URI machinesRef) {
		this.machinesRef = machinesRef;
	}
}
