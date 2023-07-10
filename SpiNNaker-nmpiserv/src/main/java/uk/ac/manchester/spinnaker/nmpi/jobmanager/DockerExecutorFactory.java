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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import uk.ac.manchester.spinnaker.nmpi.rest.DockerAPI;
import uk.ac.manchester.spinnaker.nmpi.rest.DockerCreateRequest;

/**
 * Executor factory that uses Docker to run jobs.
 */
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

	@Value("${docker.uri}")
	private String dockerUri;

	/**
	 * The docker client.
	 */
	private DockerAPI dockerApi;

	/**
	 * The thread group of any threads.
	 */
	private final ThreadGroup threadGroup;

	/**
	 * Logging.
	 */
	private static final Logger logger = getLogger(Executor.class);

	/**
	 * Create a new docker factory.
	 */
	public DockerExecutorFactory() {
		this.threadGroup = new ThreadGroup("Docker");
	}

	@PostConstruct
	private void init() {
		dockerApi = DockerAPI.createClient(dockerUri);
	}

	@Override
	public JobExecuter createJobExecuter(final JobManager manager,
			final URL baseUrl) throws IOException {
		return new Executor(manager, baseUrl);
	}

	protected final class Executor implements JobExecuter {

		private final JobManager manager;

		private final String uuid;

		private final List<String> args = new ArrayList<>();

		private String id;

		private Executor(final JobManager jobManager, final URL baseUrl)
				throws IOException {
			this.manager = jobManager;
			uuid = randomUUID().toString();
			args.add("/home/spinnaker/start_simulation.sh");
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
			var response = dockerApi.create(
					new DockerCreateRequest(image, args));
			id = response.getId();
			logger.info("Created docker container {}, warnings: {}", id);
			dockerApi.start(id);
			new Thread(threadGroup, this::waitForExit,
					"Docker Executer (" + uuid + ")").start();
		}

		public void waitForExit() {
			dockerApi.wait(id);
			logger.info("Finished execution of {}", id);
			try {
				manager.setExecutorExited(uuid, DockerAPI.readLog(
						dockerApi.getLog(id, true, true)));
			} catch (IOException e) {
				logger.error("Error reading log", e);
			}
			if (deleteOnExit) {
				dockerApi.delete(id);
			}
		}
	}

}
