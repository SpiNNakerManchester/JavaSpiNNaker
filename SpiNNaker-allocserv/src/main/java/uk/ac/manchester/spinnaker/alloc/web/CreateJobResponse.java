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
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;

/**
 * Describes a newly-created job.
 *
 * @author Donal Fellows
 * @param jobId
 *            The ID of the job. Probably should be ignored.
 * @param jobRef
 *            The link to the job. Clients should not make this themselves.
 */
@Immutable
public record CreateJobResponse(int jobId, @JsonInclude(NON_NULL) URI jobRef) {
	CreateJobResponse(SpallocAPI.Job j, UriInfo ui) {
		this(j.getId(),
				ui.getRequestUriBuilder().path("{id}").build(j.getId()));
	}
}
