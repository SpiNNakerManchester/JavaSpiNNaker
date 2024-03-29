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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * A NMPI create session request.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SessionRequest {
	/** The ID of the collaboratory to create the session in. */
	private String collab;

	/** The ID of the user who is requesting the creation. */
	private String userId;

	/** The hardware platform to create the session in. */
	private String hardwarePlatform;

	/** The specified configuration of the hardware to use. */
	private Map<String, String> hardwareConfig = new HashMap<>();

	/**
	 * Get the ID of the collaboratory to create the session in.
	 *
	 * @return the collab.
	 */
	public String getCollab() {
		return collab;
	}

	/**
	 * Sets the collab.
	 *
	 * @param collab
	 *            the collab to set
	 */
	public void setCollab(final String collab) {
		this.collab = collab;
	}

	/**
	 * Get the ID of the user who is requesting the creation.
	 *
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Sets the userId.
	 *
	 * @param userId
	 *            the userId to set
	 */
	public void setUserId(final String userId) {
		this.userId = userId;
	}

	/**
	 * @return the hardware platform to create the session in.
	 */
	public String getHardwarePlatform() {
		return hardwarePlatform;
	}

	/**
	 * @param hardwarePlatform
	 *            the Hardware Platform to set
	 */
	public void setHardwarePlatform(String hardwarePlatform) {
		this.hardwarePlatform = hardwarePlatform;
	}

	/**
	 * @return the specified configuration of the hardware to use.
	 */
	public Map<String, String> getHardwareConfig() {
		return hardwareConfig;
	}

	/**
	 * @param hardwareConfig
	 *            the Hardware Configuration to set
	 */
	public void setHardwareConfig(Map<String, String> hardwareConfig) {
		this.hardwareConfig = hardwareConfig;
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
		// Ignore any other values
	}
}
