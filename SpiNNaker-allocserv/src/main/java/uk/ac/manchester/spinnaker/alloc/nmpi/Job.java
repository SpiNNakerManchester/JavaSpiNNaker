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

package uk.ac.manchester.spinnaker.alloc.nmpi;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * A NMPI job.
 */
public class Job {

	/**
	 * The ID of the job.
	 */
	private Integer id;

	/**
	 * The ID of the collaboratory in which the job is created.
	 */
	private String collab;

	/**
	 * The status of the job.
	 */
	private String status;

	/**
	 * The ID of the user which created the job.
	 */
	private String userId;

	/**
	 * A count of how much resource has been used by the job.
	 */
	private ResourceUsage resourceUsage;

	/**
	 * Get the id.
	 *
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param idParam the id to set
	 */
	public void setId(final Integer idParam) {
		this.id = idParam;
	}

	/**
	 * Get the collab.
	 *
	 * @return the collab
	 */
	public String getCollab() {
		return collab;
	}

	/**
	 * Sets the collab.
	 *
	 * @param collab the collab to set
	 */
	public void setCollab(final String collab) {
		this.collab = collab;
	}

	/**
	 * Get the status.
	 *
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Sets the status.
	 *
	 * @param statusParam the status to set
	 */
	public void setStatus(final String statusParam) {
		this.status = statusParam;
	}

	/**
	 * Get the userId.
	 *
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Sets the userId.
	 *
	 * @param userIdParam the userId to set
	 */
	public void setUserId(final String userIdParam) {
		this.userId = userIdParam;
	}

	/**
	 * Get the resourceUsage.
	 *
	 * @return the resourceUsage
	 */
	public ResourceUsage getResourceUsage() {
		return resourceUsage;
	}

	/**
	 * Sets the resourceUsage.
	 *
	 * @param resourceUsageParam the resourceUsage to set
	 */
	public void setResourceUsage(final ResourceUsage resourceUsageParam) {
		this.resourceUsage = resourceUsageParam;
	}

	/**
	 * Used for JSON serialisation;
	 * ignores other properties we don't care about.
	 *
	 * @param name
	 *            The parameter to set.
	 * @param value
	 *            The value to set it to.
	 */
	@JsonAnySetter
	public void set(final String name, final Object value) {
		// Ignore any other values
	}
}
