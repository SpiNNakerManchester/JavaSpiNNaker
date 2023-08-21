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
package uk.ac.manchester.spinnaker.nmpi.model.job.pynn;

import java.util.Map;

import uk.ac.manchester.spinnaker.nmpi.model.job.JobParameters;
import uk.ac.manchester.spinnaker.nmpi.model.job.JobParametersTypeName;

/**
 * Represents the parameters required for a PyNN job.
 */
@JobParametersTypeName("PyNNJobParameters")
public class PyNNJobParameters implements JobParameters {
	/** The directory in which the job should be run. */
	private String workingDirectory;

	/** The system (bash) script to be executed to setup the environment. */
	private String setupScript;

	/** The user (python) script to eventually execute. */
	private String userScript;

	/** The configuration of the hardware. */
	private Map<String, Object> hardwareConfiguration;

	/**
	 * Create an empty parameters for serialisation.
	 */
	public PyNNJobParameters() {
		// Does Nothing
	}

	/**
	 * Create a description of the job parameters for a PyNN job.
	 *
	 * @param workingDirectory
	 *            The working directory to use.
	 * @param setupScript
	 *            The setup script to run before execution
	 * @param userScript
	 *            The user script to run.
	 * @param hardwareConfiguration
	 *            The hardware configuration desired.
	 */
	public PyNNJobParameters(final String workingDirectory,
			final String setupScript, final String userScript,
			final Map<String, Object> hardwareConfiguration) {
		this.workingDirectory = workingDirectory;
		this.userScript = userScript;
		this.setupScript = setupScript;
		this.hardwareConfiguration = hardwareConfiguration;
	}

	/**
	 * Get the workingDirectory.
	 *
	 * @return the workingDirectory
	 */
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	/**
	 * Sets the workingDirectory.
	 *
	 * @param workingDirectory
	 *            the workingDirectory to set
	 */
	public void setWorkingDirectory(final String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	/**
	 * Get the setup script.
	 *
	 * @return the script
	 */
	public String getSetupScript() {
		return setupScript;
	}

	/**
	 * Set the setup script.
	 *
	 * @param setupScript
	 *            the script
	 */
	public void setSetupScript(final String setupScript) {
		this.setupScript = setupScript;
	}

	/**
	 * Get the user script.
	 *
	 * @return the script
	 */
	public String getUserScript() {
		return userScript;
	}

	/**
	 * Sets the user script.
	 *
	 * @param userScript
	 *            the script to set
	 */
	public void setUserScript(final String userScript) {
		this.userScript = userScript;
	}

	/**
	 * Get the hardwareConfiguration.
	 *
	 * @return the hardwareConfiguration
	 */
	public Map<String, Object> getHardwareConfiguration() {
		return hardwareConfiguration;
	}

	/**
	 * Sets the hardwareConfiguration.
	 *
	 * @param hardwareConfiguration
	 *            the hardwareConfiguration to set
	 */
	public void setHardwareConfiguration(
			final Map<String, Object> hardwareConfiguration) {
		this.hardwareConfiguration = hardwareConfiguration;
	}
}
