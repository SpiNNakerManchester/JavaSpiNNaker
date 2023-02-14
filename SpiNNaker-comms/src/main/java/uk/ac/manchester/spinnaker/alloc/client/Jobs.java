/*
 * Copyright (c) 2018 The University of Manchester
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

import static uk.ac.manchester.spinnaker.alloc.client.ClientUtils.readOnlyCopy;

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
		this.jobs = readOnlyCopy(jobs);
	}

	public void setNext(URI next) {
		this.next = next;
	}

	public void setPrev(URI prev) {
		this.prev = prev;
	}
}
