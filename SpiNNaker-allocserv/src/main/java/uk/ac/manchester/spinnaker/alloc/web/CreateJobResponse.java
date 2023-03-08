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
