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
import java.util.List;

class Jobs {
	/** The jobs of the machine. */
	List<URI> jobs;

	/** The link to the next page of jobs. */
	URI next;

	/** The link to the previous page of jobs. */
	URI prev;

	public void setJobs(List<URI> jobs) {
		this.jobs = jobs;
	}

	public void setNext(URI next) {
		this.next = next;
	}

	public void setPrev(URI prev) {
		this.prev = prev;
	}
}
