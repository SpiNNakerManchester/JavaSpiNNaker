/*
 * Copyright (c) 2014 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.model.job.nmpi;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * A NMPI job.
 */
public class Job {
	/** Code to be executed. */
	private String code;

	/** The hardware configuration. */
	private Map<String, Object> hardwareConfig;

	/** The hardware platform. */
	private String hardwarePlatform;

	/** The ID of the job. */
	private Integer id;

	/** URLs of input data. */
	private List<DataItem> inputData;

	/** The ID of the collaboratory in which the job is created. */
	private String collab;

	/** The command used to execute the job. */
	private String command;

	/** The ID of the user which created the job. */
	private String userId;

	/**
	 * Get the code to be executed.
	 *
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Sets the code.
	 *
	 * @param code
	 *            the code to set
	 */
	public void setCode(final String code) {
		this.code = code;
	}

	/**
	 * Get the hardware configuration.
	 *
	 * @return the hardwareConfig
	 */
	public Map<String, Object> getHardwareConfig() {
		return hardwareConfig;
	}

	/**
	 * Sets the hardwareConfig.
	 *
	 * @param hardwareConfig
	 *            the hardwareConfig to set
	 */
	public void setHardwareConfig(
			final Map<String, Object> hardwareConfig) {
		this.hardwareConfig = hardwareConfig;
	}

	/**
	 * Get the hardware platform.
	 *
	 * @return the hardwarePlatform
	 */
	public String getHardwarePlatform() {
		return hardwarePlatform;
	}

	/**
	 * Sets the hardwarePlatform.
	 *
	 * @param hardwarePlatform
	 *            the hardwarePlatform to set
	 */
	public void setHardwarePlatform(final String hardwarePlatform) {
		this.hardwarePlatform = hardwarePlatform;
	}

	/**
	 * Get the ID of the job.
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
	 * Get the URLs of input data.
	 *
	 * @return the inputData
	 */
	public List<DataItem> getInputData() {
		return inputData;
	}

	/**
	 * Sets the inputData.
	 *
	 * @param inputData
	 *            the inputData to set
	 */
	public void setInputData(final List<DataItem> inputData) {
		this.inputData = inputData;
	}

	/**
	 * Get the ID of the collaboratory in which the job is created.
	 *
	 * @return the collab
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
	 * Get the command used to execute the job.
	 *
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Sets the command.
	 *
	 * @param command
	 *            the command to set
	 */
	public void setCommand(final String command) {
		this.command = command;
	}

	/**
	 * Get the ID of the user who created the job.
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
	 * Used for JSON serialisation.
	 *
	 * @param name
	 *            The parameter to set.
	 * @param value
	 *            The value to set it to.
	 * @hidden
	 */
	@JsonAnySetter
	public void set(final String name, final Object value) {
		// Ignore
	}
}
