/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.jobmanager;

import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.nmpi.model.job.JobManagerInterface.JOB_PROCESS_MANAGER;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.WaitResponse;
import com.github.dockerjava.core.DockerClientBuilder;

public class DockerExecutorFactory implements JobExecuterFactory {

	/**
	 * The docker image to use.
	 */
	@Value("${docker.image}")
	private String image;

	/**
	 * True if the containers should be deleted on shutdown of the container.
	 */
	@Value("${deleteJobsOnExit}")
	private boolean deleteOnExit;

	/**
	 * True if the log of the job should upload as it is output.
	 */
	@Value("${liveUploadOutput}")
	private boolean liveUploadOutput;

	/**
	 * True if a spinnaker machine should be requested.
	 */
	@Value("${requestSpiNNakerMachine}")
	private boolean requestSpiNNakerMachine;

	/**
	 * The docker client.
	 */
	private final DockerClient dockerClient =
			DockerClientBuilder.getInstance().build();

	/**
	 * Logging.
	 */
	private static final Logger logger = getLogger(Executor.class);

	@Override
	public JobExecuter createJobExecuter(final JobManager manager,
			final URL baseUrl) throws IOException {
		return new Executor(manager, baseUrl);
	}

	protected final class Executor implements JobExecuter,
			ResultCallback<WaitResponse> {

		private final JobManager manager;

		private final String uuid;

		private final List<String> args = new ArrayList<>();

		private CreateContainerResponse containerResponse;

		private Executor(final JobManager jobManager, final URL baseUrl)
				throws IOException {
			this.manager = jobManager;
			uuid = randomUUID().toString();
			URL jobProcessManagerUrl =
					new URL(baseUrl, "job/" + JOB_PROCESS_MANAGER);
			args.add(jobProcessManagerUrl.toString());
			args.add("-jar");
			args.add(JOB_PROCESS_MANAGER);
			args.add(" --serverUrl ");
			args.add(baseUrl.toString());
			args.add(" --executerId ");
			args.add(uuid);
			if (liveUploadOutput) {
				args.add(" --liveUploadOutput");
			}
			if (requestSpiNNakerMachine) {
				args.add(" --requestMachine");
			}
		}

		@Override
		public String getExecuterId() {
			return uuid;
		}

		@Override
		public void startExecuter() {
			logger.info("Starting docker with image {}", image);
			containerResponse = dockerClient.createContainerCmd(image)
					.withCmd(args).exec();
			logger.info("Created docker container {}, warnings: {}",
					containerResponse.getId(), containerResponse.getWarnings());
			dockerClient.startContainerCmd(containerResponse.getId()).exec();
			logger.info("Started docker container {}",
					containerResponse.getId());
			dockerClient.waitContainerCmd(containerResponse.getId()).exec(this);
		}

		@Override
		public void close() throws IOException {
			// Does Nothing
		}

		@Override
		public void onStart(Closeable closeable) {
			// Does Nothing
		}

		@Override
		public void onNext(WaitResponse object) {
			logger.info("Finished execution of {}", containerResponse.getId());
			manager.setExecutorExited(uuid, null);
			if (deleteOnExit) {
				dockerClient.removeContainerCmd(containerResponse.getId())
						.exec();
			}
		}

		@Override
		public void onError(Throwable throwable) {
			logger.error("Error on execution of " + containerResponse.getId(),
					throwable);
			manager.setExecutorExited(uuid, throwable.getMessage());
			if (deleteOnExit) {
				dockerClient.removeContainerCmd(containerResponse.getId())
						.exec();
			}
		}

		@Override
		public void onComplete() {
			// Does Nothing
		}
	}

}
