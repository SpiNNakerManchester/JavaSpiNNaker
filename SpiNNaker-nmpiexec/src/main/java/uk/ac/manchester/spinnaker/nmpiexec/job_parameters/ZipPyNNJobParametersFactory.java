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
import static org.rauschig.jarchivelib.ArchiverFactory.createArchiver;
import static org.rauschig.jarchivelib.CompressionType.BZIP2;
import static org.rauschig.jarchivelib.CompressionType.GZIP;
import static uk.ac.manchester.spinnaker.nmpiexec.utils.FileDownloader.downloadFile;
import static uk.ac.manchester.spinnaker.nmpiexec.utils.Log.log;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.CompressionType;

import uk.ac.manchester.spinnaker.nmpi.model.job.JobParameters;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.Job;
import uk.ac.manchester.spinnaker.nmpi.model.job.pynn.PyNNJobParameters;

/**
 * A {@link JobParametersFactory} that downloads a PyNN job as a zip or tar.gz
 * file. The URL must refer to a world-readable URL or the credentials must be
 * present in the URL.
 */
final class ZipPyNNJobParametersFactory extends JobParametersFactory {
	@Override
	public JobParameters getJobParameters(Job job, File workingDirectory,
			String setupScript)
			throws UnsupportedJobException, JobParametersFactoryException {
		// Test that there is a URL
		var jobCodeLocation = job.getCode().trim();
		if (!jobCodeLocation.startsWith("http://")
				&& !jobCodeLocation.startsWith("https://")) {
			throw new UnsupportedJobException();
		}

		// Test that the URL is well formed
		URL url;
		try {
			url = new URL(jobCodeLocation);
		} catch (MalformedURLException e) {
			throw new JobParametersFactoryException("The URL is malformed", e);
		}

		// Try to get the file and extract it
		try {
			return constructParameters(job, workingDirectory, url, setupScript);
		} catch (IOException e) {
			log(e);
			throw new JobParametersFactoryException(
					"Error in communication or extraction", e);
		} catch (Throwable e) {
			log(e);
			throw new JobParametersFactoryException(
					"General error with zip extraction", e);
		}
	}

	/**
	 * The supported compression types.
	 */
	private static final List<CompressionType> SUPPORTED_TYPES =
			List.of(BZIP2, GZIP);

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
	 */
	private boolean extractAutodetectedArchive(File output,
			File workingDirectory) throws IOException {
		try {
			var archiver = createArchiver(output);
			archiver.extract(output, workingDirectory);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Extract an archive by trying known archive types.
	 *
	 * @param workingDirectory
	 *            The directory to extract into
	 * @param output
	 *            The archive to extract
	 * @return True if the archive was extracted, False otherwise
	 */
	private boolean extractArchiveUsingKnownFormats(File workingDirectory,
			File output) {
		for (var format : ArchiveFormat.values()) {
			try {
				var archiver = createArchiver(format);
				archiver.extract(output, workingDirectory);
				return true;
			} catch (IOException e) {
				// Ignore - try the next
			}
		}
		return false;
	}

	/**
	 * Extract an archive by trying internal list of archive types.
	 *
	 * @param workingDirectory
	 *            The directory to extract into
	 * @param output
	 *            The archive to extract
	 * @return True if the archive was extracted, False otherwise
	 */
	private boolean extractTypedArchive(File workingDirectory, File output) {
		for (var format : ArchiveFormat.values()) {
			for (var type : SUPPORTED_TYPES) {
				try {
					var archiver = createArchiver(format, type);
					archiver.extract(output, workingDirectory);
					return true;
				} catch (IOException e) {
					// Ignore - try the next
				}
			}
		}
		return false;
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
	 * @throws JobParametersFactoryException
	 *             If no way to uncompress the file could be found
	 */
	private JobParameters constructParameters(Job job, File workingDirectory,
			URL url, String setupScript)
			throws IOException, JobParametersFactoryException {
		var output = downloadFile(url, workingDirectory, null);

		/* Test if there is a recognised archive */
		boolean archiveExtracted =
				extractAutodetectedArchive(output, workingDirectory);

		/*
		 * If the archive wasn't extracted by the last line, try the known
		 * formats
		 */
		if (!archiveExtracted) {
			archiveExtracted =
					extractArchiveUsingKnownFormats(workingDirectory, output);
		}

		/*
		 * If the archive was still not extracted, try again with different
		 * compression types
		 */
		if (!archiveExtracted) {
			archiveExtracted = extractTypedArchive(workingDirectory, output);
		}

		// Delete the archive
		if (!output.delete()) {
			log("Warning, could not delete file " + output);
		}

		// If the archive wasn't extracted, throw an error
		if (!archiveExtracted) {
			throw new JobParametersFactoryException(
					"The URL could not be decompressed with any known method");
		}

		var script = DEFAULT_SCRIPT_NAME + SYSTEM_ARG;
		var command = job.getCommand();
		if (nonNull(command) && !command.isEmpty()) {
			script = command;
		}

		return new PyNNJobParameters(workingDirectory.getAbsolutePath(),
				setupScript, script, job.getHardwareConfig());
	}
}
