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
package uk.ac.manchester.spinnaker.nmpiexec.job_parameters;

import static java.util.Objects.nonNull;

import java.io.File;
import java.util.List;
import java.util.Map;

import uk.ac.manchester.spinnaker.nmpi.model.job.JobParameters;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.Job;

/**
 * A factory that produces job parameters.
 */
public abstract class JobParametersFactory {
	/**
	 * The argument to append to the script name to request that the system is
	 * added to the command line.
	 */
	// NB has a space at the start
	public static final String SYSTEM_ARG = " {system}";

	/** The default name of the python script that will be constructed. */
	public static final String DEFAULT_SCRIPT_NAME = "run.py";

	/**
	 * Gets job parameters given job description data.
	 *
	 * @param job
	 *            The job to be executed
	 * @param workingDirectory
	 *            The working directory where the job will be run
	 * @param setupScript
	 *            The setup script to run
	 * @return A job description to be executed
	 * @throws UnsupportedJobException
	 *             If the factory does not support the job
	 * @throws JobParametersFactoryException
	 *             If there was an error getting the parameters
	 */
	public abstract JobParameters getJobParameters(Job job,
			File workingDirectory, String setupScript)
			throws UnsupportedJobException, JobParametersFactoryException;

	/** The factories for converting jobs into parameters. */
	private static final List<JobParametersFactory> JOB_PARAMETER_FACTORIES =
			List.of(new GitPyNNJobParametersFactory(),
					new ZipPyNNJobParametersFactory(),
					new DirectPyNNJobParametersFactory());

	/**
	 * Get the parameters from a job.
	 *
	 * @param job
	 *            The job that these will be parameters for.
	 * @param workingDirectory
	 *            The working directory for the job.
	 * @param setupScript
	 *            The setup script.
	 * @param errors
	 *            What errors were found in the process of getting the
	 *            parameters.
	 * @return The parameters, or {@code null} if the parameters can't be
	 *         generated.
	 */
	public static JobParameters getJobParameters(final Job job,
			final File workingDirectory, final String setupScript,
			final Map<String, JobParametersFactoryException> errors) {
		for (final var factory : JOB_PARAMETER_FACTORIES) {
			try {
				final var parameters = factory.getJobParameters(job,
						workingDirectory, setupScript);
				if (nonNull(parameters)) {
					return parameters;
				}
			} catch (final UnsupportedJobException e) {
				// Do Nothing
			} catch (final JobParametersFactoryException e) {
				errors.put(factory.getClass().getSimpleName(), e);
			}
		}
		return null;
	}
}
