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
import static org.joda.time.DateTimeZone.UTC;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.utils.ThreadUtils.sleep;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

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
	private NMPIQueueCompat queue;

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

	/** The authorization header to use. **/
	private String nmpiAuthHeader;

	/**
	 * Initialise the client.
	 */
	@PostConstruct
	private void initAPIClient() {
		queue = NMPIQueueCompat.createClient(nmpiUrl.toString());
		nmpiAuthHeader = NMPI_AUTH + " " + nmpiUsername + ":" + nmpiApiKey;
	}

	@Override
	public List<? extends Job> getJobs() {
		var val = queue.getJobs(nmpiAuthHeader, hardware, STATUS_VALIDATED);
		var run = queue.getJobs(nmpiAuthHeader, hardware, STATUS_RUNNING);
		return Stream
				.concat(val.getObjects().stream(), run.getObjects().stream())
				.collect(Collectors.toList());
	}

	/**
	 * Register a listener against the manager for new jobs.
	 *
	 * @param listener
	 *            The listener to register
	 */
	@Override
	public void addListener(final NMPIQueueListener listener) {
		listeners.add(listener);
	}

	private void handleWebAppError(final WebApplicationException e,
			final String action) {
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
					response = queue.getNextJob(nmpiAuthHeader, hardware);
				} catch (final NotFoundException e) {
					response = new QueueEmpty();
				}
				processResponse(response);
			} catch (final WebApplicationException e) {
				handleWebAppError(e, "getting next job");
				sleep(EMPTY_QUEUE_SLEEP_MS);
			} catch (final Exception e) {
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
	private void processResponse(final QueueNextResponse response) {
		if (response instanceof QueueEmpty) {
			sleep(EMPTY_QUEUE_SLEEP_MS);
		} else if (response instanceof Job) {
			processResponse((Job) response);
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
	private void processResponse(final Job job) {
		synchronized (jobCache) {
			jobCache.put(job.getId(), job);
		}
		logger.debug("Job {} received", job.getId());
		try {
			for (final var listener : listeners) {
				listener.addJob(job);
			}
			logger.debug("Setting job status");
			logger.debug("Updating job status on server");
			queue.updateJobStatus(nmpiAuthHeader, job.getId(),
					new JobStatusOnlyCompat(job.getId(), STATUS_VALIDATED));
		} catch (final WebApplicationException e) {
			handleWebAppError(e, "updating job");
			setJobError(job.getId(), null, null, e, null);
		} catch (final IOException e) {
			logger.error("Error in updating job", e);
			setJobError(job.getId(), null, null, e, null);
		}
	}

	/**
	 * Appends log messages to the log.
	 *
	 * @param id
	 *            The ID of the job
	 * @param logToAppend
	 *            The messages to append
	 */
	@Override
	public void appendJobLog(final int id, final String logToAppend) {
		var existingLog = jobLog.computeIfAbsent(
				id, ignored -> new StringBuilder());
		existingLog.append(logToAppend);
		logger.debug("Job {} log is being updated", id);
		try {
			queue.updateJobLog(nmpiAuthHeader, id,
					new NMPILog(existingLog));
		} catch (WebApplicationException e) {
			handleWebAppError(e, "updating job log");
		}
	}

	/**
	 * Mark a job as running.
	 *
	 * @param id
	 *            The ID of the job.
	 */
	@Override
	public void setJobRunning(final int id) {
		logger.debug("Job {} is running", id);
		logger.debug("Updating job status on server");
		try {
			queue.updateJobStatus(nmpiAuthHeader, id,
					new JobStatusOnlyCompat(id, STATUS_RUNNING));
		} catch (WebApplicationException e) {
			handleWebAppError(e, "setting job to running");
		}
	}

	/**
	 * Marks a job as finished successfully.
	 *
	 * @param id
	 *            The ID of the job
	 * @param logToAppend
	 *            Any additional log messages to append to the existing log
	 *            (null if none)
	 * @param outputs
	 *            The outputs of the job (null if none)
	 * @param provenance
	 *            JSON provenance information
	 */
	@Override
	public void setJobFinished(final int id, final String logToAppend,
			final List<DataItem> outputs, final ObjectNode provenance) {
		logger.debug("Job {} is finished", id);

		if (nonNull(logToAppend)) {
			appendJobLog(id, logToAppend);
		}

		final var job = new JobDoneCompat(id, STATUS_FINISHED);
		job.setOutputData(outputs);
		job.setTimestampCompletion(new DateTime(UTC));
		job.setProvenance(provenance);

		try {
			logger.debug("Updating job status on server");
			queue.finishJob(nmpiAuthHeader, id, job);
		} catch (WebApplicationException e) {
			handleWebAppError(e, "finishing job");
		}
		jobLog.remove(id);
		jobCache.remove(id);
	}

	/**
	 * Marks a job as finished with an error.
	 *
	 * @param id
	 *            The ID of the job
	 * @param logToAppend
	 *            Any additional log messages to append to the existing log
	 *            (null if none)
	 * @param outputs
	 *            Any outputs generated, or null if none
	 * @param error
	 *            The error details
	 * @param provenance
	 *            JSON provenance information
	 */
	@Override
	public void setJobError(final int id, final String logToAppend,
			final List<DataItem> outputs, final Throwable error,
			final ObjectNode provenance) {
		logger.debug("Job {} finished with an error", id);
		final var errors = new StringWriter();
		error.printStackTrace(new PrintWriter(errors));
		final var logMessage = new StringBuilder();
		if (nonNull(logToAppend)) {
			logMessage.append(logToAppend);
		}
		if (jobLog.containsKey(id) || (logMessage.length() > 0)) {
			logMessage.append("\n\n==================\n");
		}
		logMessage.append("Error:\n");
		logMessage.append(errors.toString());
		appendJobLog(id, logMessage.toString());

		final var job = new JobDoneCompat(id, STATUS_ERROR);
		job.setTimestampCompletion(new DateTime(UTC));
		job.setOutputData(outputs);
		job.setProvenance(provenance);

		try {
			logger.debug("Updating job on server");
			queue.finishJob(nmpiAuthHeader, id, job);
		} catch (WebApplicationException e) {
			handleWebAppError(e, "finishing job on error");
		}

		jobLog.remove(id);
		jobCache.remove(id);
	}

	/**
	 * Close the manager.
	 */
	@Override
	public void close() {
		done = true;
	}
}
