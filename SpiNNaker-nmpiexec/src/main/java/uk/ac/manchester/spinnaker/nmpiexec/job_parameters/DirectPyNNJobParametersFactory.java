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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import uk.ac.manchester.spinnaker.nmpi.model.job.JobParameters;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.Job;
import uk.ac.manchester.spinnaker.nmpi.model.job.pynn.PyNNJobParameters;

/**
 * A {@link JobParametersFactory} that uses the {@code experimentDescription}
 * itself as a PyNN script.
 */
class DirectPyNNJobParametersFactory extends JobParametersFactory {
	@Override
	public JobParameters getJobParameters(final Job job,
			final File workingDirectory, final String setupScript)
			throws UnsupportedJobException, JobParametersFactoryException {
		if (!job.getCode().contains("import")) {
			throw new UnsupportedJobException();
		}

		try {
			return constructParameters(job, workingDirectory, setupScript);
		} catch (final IOException e) {
			throw new JobParametersFactoryException("Error storing script", e);
		} catch (final Throwable e) {
			throw new JobParametersFactoryException(
					"General error with PyNN Script", e);
		}
	}

/**
	* Constructs the parameters by writing the script into a local file.
	*
	* @param job The job to construct parameters for
	* @param workingDirectory The directory where the job should be started
	* @param setupScript The setup script to run
	* @return The parameters created
	* @throws IOException If the file can't be found to write
	*/
	private JobParameters constructParameters(final Job job,
			final File workingDirectory, final String setupScript)
			throws IOException {
		final var scriptFile = new File(workingDirectory, DEFAULT_SCRIPT_NAME);
		try (var writer = new PrintWriter(scriptFile, UTF_8)) {
			writer.print(job.getCode());
		}

		return new PyNNJobParameters(workingDirectory.getAbsolutePath(),
				setupScript, DEFAULT_SCRIPT_NAME + SYSTEM_ARG,
				job.getHardwareConfig());
	}
}
