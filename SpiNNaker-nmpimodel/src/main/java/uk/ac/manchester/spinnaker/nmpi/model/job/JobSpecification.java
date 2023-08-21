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
package uk.ac.manchester.spinnaker.nmpi.model.job;

import uk.ac.manchester.spinnaker.nmpi.model.machine.SpinnakerMachine;

/**
 * The specification for a job. Can be any type of job, though the rest of this
 * implementation only really supports PyNN jobs running on SpiNNaker hardware
 * using sPyNNaker.
 */
public class JobSpecification {
	/** The machine to execute the job on. */
	private SpinnakerMachine machine;

	/** The parameters of the job. */
	private JobParameters parameters;

	/** The ID of the job. */
	private int id;

	/** The URL of the job to send results and status to. */
	private String url;

	/**
	 * Constructor for serialisation.
	 */
	public JobSpecification() {
		// Does Nothing
	}

	/**
	 * Create the specification for running a job.
	 *
	 * @param machine
	 *            The machine to run the job on.
	 * @param parameters
	 *            The parameters to the job.
	 * @param id
	 *            The ID of the job.
	 * @param url
	 *            The URL of the job to send results and status to.
	 */
	public JobSpecification(SpinnakerMachine machine, JobParameters parameters,
			int id, String url) {
		this.machine = machine;
		this.parameters = parameters;
		this.id = id;
		this.url = url;
	}

	/**
	 * Get the machine to run the job on.
	 *
	 * @return the machine
	 */
	public SpinnakerMachine getMachine() {
		return machine;
	}

	/**
	 * Sets the machine.
	 *
	 * @param machine
	 *            the machine to set
	 */
	public void setMachine(SpinnakerMachine machine) {
		this.machine = machine;
	}

	/**
	 * Get the parameters of the job.
	 *
	 * @return the parameters
	 */
	public JobParameters getParameters() {
		return parameters;
	}

	/**
	 * Sets the parameters.
	 *
	 * @param parameters
	 *            the parameters to set
	 */
	public void setParameters(JobParameters parameters) {
		this.parameters = parameters;
	}

	/**
	 * Get the ID of the job.
	 *
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the ID.
	 *
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Get the URL of the job to send results and status to.
	 *
	 * @return the URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the URL.
	 *
	 * @param url
	 *            the URL to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
}
