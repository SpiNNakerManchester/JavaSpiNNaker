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
import static uk.ac.manchester.spinnaker.nmpiexec.utils.FileDownloader.downloadFile;
import static uk.ac.manchester.spinnaker.nmpiexec.utils.Log.log;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.examples.Expander;

import uk.ac.manchester.spinnaker.nmpi.model.job.JobParameters;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.Job;
import uk.ac.manchester.spinnaker.nmpi.model.job.pynn.PyNNJobParameters;

/**
 * A {@link JobParametersFactory} that downloads a PyNN job as a zip or tar.gz
 * file. The URL must refer to a world-readable URL or the credentials must be
 * present in the URL.
 */
class ZipPyNNJobParametersFactory extends JobParametersFactory {
	@Override
	public JobParameters getJobParameters(final Job job,
			final File workingDirectory, final String setupScript)
			throws UnsupportedJobException, JobParametersFactoryException {
		// Test that there is a URL
		final var jobCodeLocation = job.getCode().trim();
		if (!jobCodeLocation.startsWith("http://")
				&& !jobCodeLocation.startsWith("https://")) {
			throw new UnsupportedJobException();
		}

		// Test that the URL is well formed
		URL url;
		try {
			url = new URL(jobCodeLocation);
		} catch (final MalformedURLException e) {
			throw new JobParametersFactoryException("The URL is malformed", e);
		}

		// Try to get the file and extract it
		try {
			return constructParameters(job, workingDirectory, url, setupScript);
		} catch (final Throwable e) {
			log(e);
			throw new JobParametersFactoryException(
					"Error with zip extraction", e);
		}
	}

	/**
	 * Extract an archive using auto-detection for the format.
	 *
	 * @param output
	 *            The archive to extract
	 * @param workingDirectory
	 *            The directory to extract into
	 * @return True if extracted, False if failed
	 * @throws IOException
	 *             If there is a general error in extraction
	 * @throws ArchiveException
	 */
	private void extractAutodetectedArchive(final File output,
			final File workingDirectory) throws IOException, ArchiveException {
		new Expander().expand(output, workingDirectory);
	}

	/**
	 * Build the job parameters.
	 *
	 * @param job
	 *            The job to build the parameters for
	 * @param workingDirectory
	 *            The directory where the job should be run
	 * @param url
	 *            The URL of the archive to use
	 * @param setupScript
	 *            The setup script
	 * @return The constructed parameters
	 * @throws IOException
	 *             If there is an error with the file
	 * @throws ArchiveException
	 *            If there is an error with the archive
	 */
	private JobParameters constructParameters(final Job job,
			final File workingDirectory, final URL url,
			final String setupScript)
			throws IOException, ArchiveException {
		final var output = downloadFile(url, workingDirectory, null);

		/* Test if there is a recognised archive */
		extractAutodetectedArchive(output, workingDirectory);

		var script = DEFAULT_SCRIPT_NAME + SYSTEM_ARG;
		final var command = job.getCommand();
		if (nonNull(command) && !command.isEmpty()) {
			script = command;
		}

		return new PyNNJobParameters(workingDirectory.getAbsolutePath(),
				setupScript, script, job.getHardwareConfig());
	}
}
