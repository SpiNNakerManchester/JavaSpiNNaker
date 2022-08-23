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
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
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
		UriBuilder b = ui.getAbsolutePathBuilder().path("{id}");
		jobs = unmodifiableList(
				jc.ids().stream().map(id -> b.build(id)).collect(toList()));
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
