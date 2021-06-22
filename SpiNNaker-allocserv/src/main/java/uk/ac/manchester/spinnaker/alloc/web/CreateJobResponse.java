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

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.annotation.JsonInclude;

import uk.ac.manchester.spinnaker.alloc.allocator.Spalloc;

/**
 * Describes a newly-created job.
 *
 * @author Donal Fellows
 */
public class CreateJobResponse {
	/** The ID of the job. Probably should be ignored. */
	public final int jobId;

	/** The link to the job. Clients should not make this themselves. */
	@JsonInclude(NON_NULL)
	public final URI jobRef;

	CreateJobResponse(Spalloc.Job j, UriInfo ui) {
		jobId = j.getId();
		jobRef = ui.getRequestUriBuilder().path("{id}").build(j.getId());
	}
}
