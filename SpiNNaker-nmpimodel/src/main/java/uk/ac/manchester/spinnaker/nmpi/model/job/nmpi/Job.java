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

	/**
	 * Code to be executed.
	 */
	private String code;

	/**
	 * The hardware configuration.
	 */
	private Map<String, Object> hardwareConfig;

	/**
	 * The hardware platform.
	 */
	private String hardwarePlatform;

	/**
	 * The ID of the job.
	 */
	private Integer id;

	/**
	 * URLs of input data.
	 */
	private List<DataItem> inputData;

	/**
	 * The ID of the collaboratory in which the job is created.
	 */
	private String collab;

	/**
	 * The command used to execute the job.
	 */
	private String command;

	/**
	 * The ID of the user which created the job.
	 */
	private String userId;

	/**
	 * Get the code.
	 *
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Sets the code.
	 *
	 * @param codeParam the code to set
	 */
	public void setCode(final String codeParam) {
		this.code = codeParam;
	}

	/**
	 * Get the hardwareConfig.
	 *
	 * @return the hardwareConfig
	 */
	public Map<String, Object> getHardwareConfig() {
		return hardwareConfig;
	}

	/**
	 * Sets the hardwareConfig.
	 *
	 * @param hardwareConfigParam the hardwareConfig to set
	 */
	public void setHardwareConfig(
			final Map<String, Object> hardwareConfigParam) {
		this.hardwareConfig = hardwareConfigParam;
	}

	/**
	 * Get the hardwarePlatform.
	 *
	 * @return the hardwarePlatform
	 */
	public String getHardwarePlatform() {
		return hardwarePlatform;
	}

	/**
	 * Sets the hardwarePlatform.
	 *
	 * @param hardwarePlatformParam the hardwarePlatform to set
	 */
	public void setHardwarePlatform(final String hardwarePlatformParam) {
		this.hardwarePlatform = hardwarePlatformParam;
	}

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
	 * Get the inputData.
	 *
	 * @return the inputData
	 */
	public List<DataItem> getInputData() {
		return inputData;
	}

	/**
	 * Sets the inputData.
	 *
	 * @param inputDataParam the inputData to set
	 */
	public void setInputData(final List<DataItem> inputDataParam) {
		this.inputData = inputDataParam;
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
	 * @param collabParam the collab to set
	 */
	public void setCollab(final String collabParam) {
		this.collab = collabParam;
	}

	/**
	 * Get the command.
	 *
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Sets the command.
	 *
	 * @param commandParam the command to set
	 */
	public void setCommand(final String commandParam) {
		this.command = commandParam;
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
	 * Used for JSON serialisation.
	 *
	 * @param name
	 *            The parameter to set.
	 * @param value
	 *            The value to set it to.
	 */
	@JsonAnySetter
	public void set(final String name, final Object value) {
		// Ignore
	}
}
