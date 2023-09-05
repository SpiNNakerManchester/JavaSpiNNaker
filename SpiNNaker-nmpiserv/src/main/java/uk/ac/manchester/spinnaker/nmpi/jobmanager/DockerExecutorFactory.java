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

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.utils.ThreadUtils.waitfor;
import static uk.ac.manchester.spinnaker.nmpi.model.job.JobManagerInterface.JOB_PROCESS_MANAGER;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.nmpi.rest.DockerAPI;
import uk.ac.manchester.spinnaker.nmpi.rest.DockerCreateRequest;
import uk.ac.manchester.spinnaker.nmpi.rest.DockerInspectResponse;

/**
 * Executor factory that uses Docker to run jobs.
 */
public final class DockerExecutorFactory implements JobExecuterFactory {
	/** Time to wait between docker inspects while waiting for finish. */
	private static final int WAIT_SLEEP_TIME_MS = 1000;

	/** The docker image to use. */
	@Value("${docker.image}")
	private String image;

	/**
	 * True if the containers should be deleted on shutdown of the container.
	 */
	@Value("${deleteJobsOnExit}")
	private boolean deleteOnExit;

	/** True if the log of the job should upload as it is output. */
	@Value("${liveUploadOutput}")
	private boolean liveUploadOutput;

	/** True if a SpiNNaker machine should be requested. */
	@Value("${requestSpiNNakerMachine}")
	private boolean requestSpiNNakerMachine;

	@Value("${docker.uri}")
	private String dockerUri;

	/** The maximum number of VMs to create. */
	@Value("${docker.maxVms}")
	private int maxNVirtualMachines;

	/** The current number of VMs. */
	@GuardedBy("lock")
	private int nVirtualMachines = 0;

	/** The docker client. */
	private DockerAPI dockerApi;

	/** The thread group of any threads. */
	private final ThreadGroup threadGroup;

	/** Lock object used for synchronisation. */
	private final Object lock = new Object();

	/** Logging. */
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
	public JobExecuter createJobExecuter(JobManager manager, URL baseUrl)
			throws IOException {
		requireNonNull(manager);
		requireNonNull(baseUrl);
		waitToClaimVM();
		return new Executor(manager, baseUrl);
	}

	/**
	 * Wait for the VM to come up.
	 */
	private void waitToClaimVM() {
		synchronized (lock) {
			logger.info("{} of {} in use", nVirtualMachines,
					maxNVirtualMachines);
			while (nVirtualMachines >= maxNVirtualMachines) {
				logger.debug("Waiting for a VM to become available "
						+ "({} of {} in use)", nVirtualMachines,
						maxNVirtualMachines);
				waitfor(lock);
			}
			nVirtualMachines++;
		}
	}

	/** Callback when the executor is finished. */
	protected void executorFinished() {
		synchronized (lock) {
			nVirtualMachines--;
			logger.info("{} of {} now in use", nVirtualMachines,
					maxNVirtualMachines);
			lock.notifyAll();
		}
	}

	/**
	 * Job executor that uses Docker.
	 */
	protected final class Executor implements JobExecuter {
		private final JobManager manager;

		private final String uuid;

		private final List<String> args = new ArrayList<>();

		private String id;

		private Executor(JobManager jobManager, URL baseUrl)
				throws IOException {
			this.manager = jobManager;
			uuid = randomUUID().toString();
			var jobProcessManagerUrl =
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
			logger.info("Created docker container {}, warnings: {}", id,
					response.getWarnings());
			dockerApi.start(id);
			new Thread(threadGroup, this::waitForExit,
					"Docker Executer (" + uuid + ")").start();
		}

		private void waitForRunning(boolean running) {
			DockerInspectResponse res = null;
			do {
				try {
					Thread.sleep(WAIT_SLEEP_TIME_MS);
					res = dockerApi.inspect(id);
				} catch (NotFoundException e) {
					// If we can't find it, it isn't running!
					if (!running) {
						return;
					}
				} catch (InterruptedException e) {
					return;
				}
			} while (res == null || res.getState().isRunning() != running);
		}

		/**
		 * Monitors the status of the docker container.
		 */
		protected void waitForExit() {
			try {
				waitForRunning(true);
				waitForRunning(false);
				logger.info("Finished execution of {}", id);
				try {
					manager.setExecutorExited(uuid, DockerAPI.readLog(
							dockerApi.getLog(id, true, true)));
				} catch (IOException e) {
					logger.error("Error reading log", e);
				}
				if (deleteOnExit) {
					logger.info("Deleting {}", id);
					dockerApi.delete(id);
				}
			} finally {
				executorFinished();
			}
		}
	}
}
