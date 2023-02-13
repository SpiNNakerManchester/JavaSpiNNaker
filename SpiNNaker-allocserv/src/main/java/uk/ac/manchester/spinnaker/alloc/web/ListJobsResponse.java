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
package uk.ac.manchester.spinnaker.alloc.web;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.stream.Collectors.toList;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.copy;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.annotation.JsonInclude;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Jobs;

/**
 * The list of jobs.
 *
 * @author Donal Fellows
 */
public final class ListJobsResponse {
	/** The list of jobs, by URI. Clients should not construct these by hand. */
	public final List<URI> jobs;

	private URI prev;

	private URI next;

	ListJobsResponse(Jobs jc, UriInfo ui) {
		var b = ui.getAbsolutePathBuilder().path("{id}");
		jobs = copy(jc.ids().stream().map(b::build).collect(toList()));
	}

	/** @return URL of previous page when paging is used in this response. */
	@JsonInclude(value = NON_NULL)
	public URI getPrev() {
		return prev;
	}

	void setPrev(URI prev) {
		this.prev = prev;
	}

	/** @return URL of next page when paging is used in this response. */
	@JsonInclude(value = NON_NULL)
	public URI getNext() {
		return next;
	}

	void setNext(URI next) {
		this.next = next;
	}
}
