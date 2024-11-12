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
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * A NMPI session response.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SessionResponse {
	/** The ID of the session. */
	private Integer id;

	/** A count of how much resource has been used by the job. */
	private ResourceUsage resourceUsage;

	/**
	 * Get the ID of the session.
	 *
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id
	 *            the id to set
	 */
	public void setId(final Integer id) {
		this.id = id;
	}

	/**
	 * Get the count of how much resource has been used by the job.
	 *
	 * @return the resourceUsage
	 */
	public ResourceUsage getResourceUsage() {
		return resourceUsage;
	}

	/**
	 * Sets the resourceUsage.
	 *
	 * @param resourceUsage
	 *            the resourceUsage to set
	 */
	public void setResourceUsage(final ResourceUsage resourceUsage) {
		this.resourceUsage = resourceUsage;
	}

	/**
	 * Used for JSON serialisation; ignores other properties we don't care
	 * about.
	 *
	 * @param name
	 *            The parameter to set.
	 * @param value
	 *            The value to set it to.
	 * @hidden
	 */
	@JsonAnySetter
	public void set(final String name, final Object value) {
		System.err.println("Ignoring unknown property: " + name + " = " + value);
	}
}
