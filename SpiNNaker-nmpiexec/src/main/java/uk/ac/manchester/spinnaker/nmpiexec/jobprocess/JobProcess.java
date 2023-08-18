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
package uk.ac.manchester.spinnaker.nmpiexec.jobprocess;

import java.io.File;
import java.util.List;

import uk.ac.manchester.spinnaker.nmpi.model.job.JobParameters;
import uk.ac.manchester.spinnaker.nmpi.model.job.Status;
import uk.ac.manchester.spinnaker.nmpi.model.machine.SpinnakerMachine;

/**
 * An interface to an executable job process type.
 *
 * @param <P>
 *            The type of the parameters
 */
public interface JobProcess<P extends JobParameters> {
	/**
	 * Executes the job.
	 *
	 * @param machineUrl
	 *            The URL to request a machine using (or {@code null} if
	 *            machine is given)
	 * @param machine
	 *            The machine to execute the job on (or {@code null} if a URL
	 *            is given)
	 * @param parameters
	 *            The parameters of the job
	 * @param logWriter
	 *            Somewhere to write logs to
	 */
	void execute(String machineUrl, SpinnakerMachine machine, P parameters,
			LogWriter logWriter);

	/**
	 * Gets the status of the job.
	 *
	 * @return The status
	 */
	Status getStatus();

	/**
	 * Gets any errors returned by the job. If the status is not Error, this
	 * should return {@code null}
	 *
	 * @return An error, or {@code null} if no error
	 */
	Throwable getError();

	/**
	 * Gets any outputs from the job. Should always return a list, but this list
	 * can be empty if there are no outputs.
	 *
	 * @return A list of output files.
	 */
	List<File> getOutputs();

	/**
	 * Gets any provenance data from the job. Should always return a list, but
	 * this map can be empty if there is no provenance.
	 *
	 * @return A list of provenance items to add
	 */
	List<ProvenanceItem> getProvenance();

	/**
	 * Cleans up the job, removing any associated files.
	 */
	void cleanup();
}
