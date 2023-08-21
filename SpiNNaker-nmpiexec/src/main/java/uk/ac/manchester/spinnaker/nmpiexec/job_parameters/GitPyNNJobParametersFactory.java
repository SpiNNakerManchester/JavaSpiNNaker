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
import static org.eclipse.jgit.api.Git.cloneRepository;

import java.io.File;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import uk.ac.manchester.spinnaker.nmpi.model.job.JobParameters;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.Job;
import uk.ac.manchester.spinnaker.nmpi.model.job.pynn.PyNNJobParameters;

/**
 * A {@link JobParametersFactory} that downloads a PyNN job from git. The git
 * repository must be world-readable, or sufficient credentials must be present
 * in the URL.
 */
class GitPyNNJobParametersFactory extends JobParametersFactory {
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

		// Try to get the repository
		try {
			return constructParameters(job, workingDirectory, jobCodeLocation,
					setupScript);
		} catch (final InvalidRemoteException e) {
			throw new JobParametersFactoryException("Remote is not valid", e);
		} catch (final TransportException e) {
			throw new JobParametersFactoryException("Transport failed", e);
		} catch (final GitAPIException e) {
			throw new JobParametersFactoryException("Error using Git", e);
		} catch (final Throwable e) {
			throw new JobParametersFactoryException(
					"General error getting git repository", e);
		}
	}

	/**
	 * Constructs the parameters by checking out the git repository.
	 *
	 * @param job
	 *            The job to get the parameters of
	 * @param workingDirectory
	 *            The directory where the job should be run
	 * @param experimentDescription
	 *            The git URL
	 * @param setupScript
	 *            The setup script to run
	 * @return The constructed parameters
	 * @throws GitAPIException
	 *             If there is a general problem using Git
	 * @throws InvalidRemoteException
	 *             If there is a problem with the repository
	 * @throws TransportException
	 *             If there is a problem in communication
	 * @throws URISyntaxException
	 *             If the URI syntax is incorrect
	 */
	private JobParameters constructParameters(final Job job,
			final File workingDirectory, final String experimentDescription,
			final String setupScript)
			throws GitAPIException, InvalidRemoteException, TransportException,
			URISyntaxException {
		final var clone = cloneRepository();
		var urish = new URIish(experimentDescription);
		if (nonNull(urish.getUser())) {
			var pass = urish.getPass();
			if (nonNull(pass)) {
				pass = "";
			}
			clone.setCredentialsProvider(
				new UsernamePasswordCredentialsProvider(urish.getUser(), pass));
		}

		// Clone into a sub-directory of the working directory
		var subdir = urish.getHumanishName();
		if (subdir.equals("")) {
			subdir = "repo";
		}
		final var cloneDir = new File(workingDirectory, subdir);

		clone.setURI(experimentDescription);
		clone.setDirectory(cloneDir);
		clone.setCloneSubmodules(true);
		clone.call();

		var script = DEFAULT_SCRIPT_NAME + SYSTEM_ARG;
		final var command = job.getCommand();
		if (nonNull(command) && !command.isEmpty()) {
			script = command;
		}

		return new PyNNJobParameters(cloneDir.getAbsolutePath(),
				setupScript, script, job.getHardwareConfig());
	}
}
