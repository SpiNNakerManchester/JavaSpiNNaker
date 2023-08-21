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
package uk.ac.manchester.spinnaker.nmpi.nmpi;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.joda.time.DateTimeZone.UTC;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.nmpi.ThreadUtils.sleep;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.node.ObjectNode;

import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.DataItem;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.Job;
import uk.ac.manchester.spinnaker.nmpi.model.NMPILog;
import uk.ac.manchester.spinnaker.nmpi.model.QueueEmpty;
import uk.ac.manchester.spinnaker.nmpi.model.QueueNextResponse;
import uk.ac.manchester.spinnaker.nmpi.rest.JobDoneCompat;
import uk.ac.manchester.spinnaker.nmpi.rest.JobListCompat;
import uk.ac.manchester.spinnaker.nmpi.rest.JobStatusOnlyCompat;
import uk.ac.manchester.spinnaker.nmpi.rest.NMPIQueueCompat;

/**
 * Manages the NMPI queue, receiving jobs and submitting them to be run.
 */
public class NMPIQueueManagerCompat implements NMPIQueueManager {
	/** Job status when finished. */
	public static final String STATUS_FINISHED = "finished";

	/** Job status when in the queue but the executer hasn't started. */
	public static final String STATUS_VALIDATED = "validated";

	/** Job status when running. */
	public static final String STATUS_RUNNING = "running";

	/** Job status when in error. */
	public static final String STATUS_ERROR = "error";

	/** The amount of time to sleep when an empty queue is detected. */
	private static final int EMPTY_QUEUE_SLEEP_MS = 10000;

	/** Header for APIKey authentication. */
	private static final String NMPI_AUTH = "ApiKey";

	/** The queue to get jobs from. */
	private Queue queue;

	/** Marker to indicate if the manager is done or not. */
	private boolean done = false;

	/** The set of listeners for this queue. */
	private final Set<NMPIQueueListener> listeners = new HashSet<>();

	/** A cache of jobs that have been received. */
	private final Map<Integer, Job> jobCache = new HashMap<>();

	/** The log of the job so far. */
	private final Map<Integer, StringBuilder> jobLog = new HashMap<>();

	/** Logger. */
	private static final Logger logger = getLogger(NMPIQueueManager.class);

	/** The hardware identifier for the queue. */
	@Value("${nmpi.hardware}")
	private String hardware;

	/** The URL from which to load the data. */
	@Value("${nmpi.url}")
	private URL nmpiUrl;

	/** The API key to authenticate against the server. */
	@Value("${nmpi.apiKey}")
	private String nmpiApiKey;

	/** The API username to authenticate against the server. */
	@Value("${nmpi.username}")
	private String nmpiUsername;

	/**
	 * Wraps the constant values associated with the NMPI queue API.
	 */
	private static final class Queue {
		/** The queue to get jobs from. */
		private final NMPIQueueCompat queue;

		/** The authorization header to use. **/
		private final String authHeader;

		/** The hardware identifier for the queue. */
		private final String hardware;

		Queue(URL url, String username, String apiKey, String hardware) {
			queue = NMPIQueueCompat.createClient(url.toString());
			authHeader = NMPI_AUTH + " " + username + ":" + apiKey;
			this.hardware = hardware;
		}

		JobListCompat getJobs(String status) {
			return queue.getJobs(authHeader, hardware, status);
		}

		QueueNextResponse getNextJob() {
			return queue.getNextJob(authHeader, hardware);
		}

		void updateJobStatus(int jobId, String status) {
			queue.updateJobStatus(authHeader, jobId,
					new JobStatusOnlyCompat(jobId, status));
		}

		void updateJobLog(int jobId, StringBuilder log) {
			queue.updateJobLog(authHeader, jobId, new NMPILog(log));
		}

		void finishJob(int jobId, String status, List<DataItem> outputs,
				ObjectNode provenance) {
			var jobDone = new JobDoneCompat(jobId, status);
			jobDone.setOutputData(outputs);
			jobDone.setProvenance(provenance);
			jobDone.setTimestampCompletion(new DateTime(UTC));
			queue.finishJob(authHeader, jobId, jobDone);
		}
	}

	/**
	 * Initialise the client.
	 */
	@PostConstruct
	private void initAPIClient() {
		queue = new Queue(nmpiUrl, nmpiUsername, nmpiApiKey, hardware);
	}

	@Override
	public List<? extends Job> getJobs() {
		var val = queue.getJobs(STATUS_VALIDATED);
		var run = queue.getJobs(STATUS_RUNNING);
		return Stream
				.concat(val.getObjects().stream(), run.getObjects().stream())
				.collect(toList());
	}

	@Override
	public void addListener(NMPIQueueListener listener) {
		listeners.add(listener);
	}

	private void handleWebAppError(WebApplicationException e, String action) {
		var body = e.getResponse().readEntity(String.class);
		logger.error("Error {} ({}), continuing: {}", action, e.getMessage(),
				body);
	}

	@Override
	public void processResponsesFromQueue() {
		while (!done) {
			try {
				logger.debug("Getting next job");
				QueueNextResponse response;
				try {
					response = queue.getNextJob();
				} catch (NotFoundException e) {
					response = new QueueEmpty();
				}
				processResponse(response);
			} catch (WebApplicationException e) {
				handleWebAppError(e, "getting next job");
				sleep(EMPTY_QUEUE_SLEEP_MS);
			} catch (Exception e) {
				logger.error("Error in getting next job", e);
				sleep(EMPTY_QUEUE_SLEEP_MS);
			}
		}
	}

	/**
	 * Process the response from the service.
	 *
	 * @param response
	 *            The response to process
	 */
	private void processResponse(QueueNextResponse response) {
		if (response instanceof QueueEmpty) {
			sleep(EMPTY_QUEUE_SLEEP_MS);
		} else if (response instanceof Job j) {
			processResponse(j);
		} else {
			throw new IllegalStateException();
		}
	}

	/**
	 * Process the response of a Job.
	 *
	 * @param job
	 *            The job to process
	 */
	private void processResponse(Job job) {
		synchronized (jobCache) {
			jobCache.put(job.getId(), job);
		}
		logger.debug("Job {} received", job.getId());
		try {
			for (var listener : listeners) {
				listener.addJob(job);
			}
			logger.debug("Setting job status");
			logger.debug("Updating job status on server");
			queue.updateJobStatus(job.getId(), STATUS_VALIDATED);
		} catch (WebApplicationException e) {
			handleWebAppError(e, "updating job");
			setJobError(job.getId(), null, null, e, null);
		} catch (IOException e) {
			logger.error("Error in updating job", e);
			setJobError(job.getId(), null, null, e, null);
		}
	}

	@Override
	public void appendJobLog(int id, String logToAppend) {
		var existingLog =
				jobLog.computeIfAbsent(id, ignored -> new StringBuilder());
		existingLog.append(logToAppend);
		logger.debug("Job {} log is being updated", id);
		try {
			queue.updateJobLog(id, existingLog);
		} catch (WebApplicationException e) {
			handleWebAppError(e, "updating job log");
		}
	}

	@Override
	public void setJobRunning(int id) {
		logger.debug("Job {} is running", id);
		logger.debug("Updating job status on server");
		try {
			queue.updateJobStatus(id, STATUS_RUNNING);
		} catch (WebApplicationException e) {
			handleWebAppError(e, "setting job to running");
		}
	}

	@Override
	public void setJobFinished(int id, String logToAppend,
			List<DataItem> outputs, ObjectNode provenance) {
		logger.debug("Job {} is finished", id);

		if (nonNull(logToAppend)) {
			appendJobLog(id, logToAppend);
		}

		try {
			logger.debug("Updating job status on server");
			queue.finishJob(id, STATUS_FINISHED, outputs, provenance);
		} catch (WebApplicationException e) {
			handleWebAppError(e, "finishing job");
		}
		jobLog.remove(id);
		jobCache.remove(id);
	}

	@Override
	public void setJobError(int id, String logToAppend, List<DataItem> outputs,
			Throwable error, ObjectNode provenance) {
		logger.debug("Job {} finished with an error", id);
		var errors = new StringWriter();
		error.printStackTrace(new PrintWriter(errors));
		var logMessage = new StringBuilder();
		if (nonNull(logToAppend)) {
			logMessage.append(logToAppend);
		}
		if (jobLog.containsKey(id) || (logMessage.length() > 0)) {
			logMessage.append("\n\n==================\n");
		}
		logMessage.append("Error:\n");
		logMessage.append(errors.toString());
		appendJobLog(id, logMessage.toString());

		try {
			logger.debug("Updating job on server");
			queue.finishJob(id, STATUS_ERROR, outputs, provenance);
		} catch (WebApplicationException e) {
			handleWebAppError(e, "finishing job on error");
		}

		jobLog.remove(id);
		jobCache.remove(id);
	}

	@Override
	public void close() {
		done = true;
	}
}
