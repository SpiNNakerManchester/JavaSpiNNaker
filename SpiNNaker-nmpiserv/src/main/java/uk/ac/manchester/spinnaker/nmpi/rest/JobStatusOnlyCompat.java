/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.rest;

/**
 * A Job where only the status is set (to allow log updates).
 */
public class JobStatusOnlyCompat {
	private int id;

	private String status;

	/**
	 * Create a job with only a status.
	 *
	 * @param id
	 *            The job id.
	 * @param status
	 *            The status to set.
	 */
	public JobStatusOnlyCompat(int id, String status) {
		this.id = id;
		this.status = status;
	}

	/**
	 * Get the status.
	 *
	 * @return The status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Set the status.
	 *
	 * @param status
	 *            The status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Get the id.
	 *
	 * @return The id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the id.
	 *
	 * @param id
	 *            The id
	 */
	public void setId(int id) {
		this.id = id;
	}
}
