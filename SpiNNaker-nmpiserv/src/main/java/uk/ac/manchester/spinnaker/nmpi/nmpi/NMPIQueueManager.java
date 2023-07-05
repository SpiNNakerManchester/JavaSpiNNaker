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

import javax.annotation.PostConstruct;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.node.ObjectNode;

import uk.ac.manchester.spinnaker.nmpi.model.NMPILog;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.DataItem;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.Job;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.OutputData;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.QueueEmpty;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.QueueNextResponse;
import uk.ac.manchester.spinnaker.nmpi.rest.NMPIQueue;

/**
 * Manages the NMPI queue, receiving jobs and submitting them to be run.
 */
public class NMPIQueueManager {

	/**
	 * Job status when finished.
	 */
	public static final String STATUS_FINISHED = "finished";

	/**
	 * Job status when in the queue but the executer hasn't started.
	 */
	public static final String STATUS_VALIDATED = "validated";

	/**
	 * Job status when running.
	 */
	public static final String STATUS_RUNNING = "running";

	/**
	 * Job status when in error.
	 */
	public static final String STATUS_ERROR = "error";

	/**
	 * The name of the repository for the service.
	 */
	private static final String REPOSITORY =
			"SpiNNaker Manchester temporary storage";

	/**
	 * The amount of time to sleep when an empty queue is detected.
	 */
	private static final int EMPTY_QUEUE_SLEEP_MS = 10000;

	/** The queue to get jobs from. */
	private NMPIQueue queue;

	/** Marker to indicate if the manager is done or not. */
	private boolean done = false;

	/** The set of listeners for this queue. */
	private final Set<NMPIQueueListener> listeners = new HashSet<>();

	/** A cache of jobs that have been received. */
	private final Map<Integer, Job> jobCache = new HashMap<>();

	/** The log of the job so far. */
	private final Map<Integer, NMPILog> jobLog = new HashMap<>();

	/**
	 * Logger.
	 */
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

	/**
	 * Initialise the client.
	 */
	@PostConstruct
	private void initAPIClient() {
		queue = NMPIQueue.createClient(nmpiUrl.toString());
	}

	/**
	 * Gets a job from the server.
	 *
	 * @param id The ID of the job.
	 * @return The job
	 */
	private Job getJobFromQueue(final int id) {
		return queue.getJob(nmpiApiKey, id);
	}

	/**
	 * Gets a job from the cache, or from the server if the job is not in the
	 * cache.
	 *
	 * @param id
	 *            The ID of the job
	 * @return The job
	 */
	private Job getJob(final int id) {
		synchronized (jobCache) {
			return jobCache.computeIfAbsent(id, this::getJobFromQueue);
		}
	}

	/**
	 * Register a listener against the manager for new jobs.
	 *
	 * @param listener
	 *            The listener to register
	 */
	public void addListener(final NMPIQueueListener listener) {
		listeners.add(listener);
	}

	public void processResponsesFromQueue() {
		while (!done) {
			try {
				logger.debug("Getting next job");
				QueueNextResponse response;
				try {
					response = queue.getNextJob(nmpiApiKey, hardware);
				} catch (final NotFoundException e) {
					response = new QueueEmpty();
				}
				processResponse(response);
			} catch (final WebApplicationException e) {
				var body = e.getResponse().readEntity(String.class);
				logger.error("Error in getting next job: " + body, e);
			} catch (final Exception e) {
				logger.error("Error in getting next job", e);
				sleep(EMPTY_QUEUE_SLEEP_MS);
			}
		}
	}

	/**
	 * Process the response from the service.
	 *
	 * @param response The response to process
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
	 * @param job The job to process
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
			job.setTimestampSubmission(
					job.getTimestampSubmission().withZoneRetainFields(UTC));
			job.setTimestampCompletion(null);
			job.setStatus(STATUS_VALIDATED);
			logger.debug("Updating job status on server");
			queue.updateJob(nmpiApiKey, job.getId(), job);
		} catch (final WebApplicationException e) {
			var body = e.getResponse().readEntity(String.class);
			logger.error("Error in updating job: " + body, e);
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
	public void appendJobLog(final int id, final String logToAppend) {
		var existingLog = jobLog.computeIfAbsent(id, ignored -> new NMPILog());
		existingLog.appendContent(logToAppend);
		logger.debug("Job {} log is being updated", id);
		queue.updateLog(nmpiApiKey, id, existingLog);
	}

	/**
	 * Mark a job as running.
	 *
	 * @param id
	 *            The ID of the job.
	 */
	public void setJobRunning(final int id) {
		logger.debug("Job {} is running", id);
		final var job = getJob(id);
		job.setStatus(STATUS_RUNNING);
		logger.debug("Updating job status on server");
		queue.updateJob(nmpiApiKey, id, job);
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
	public void setJobFinished(final int id, final String logToAppend,
			final List<DataItem> outputs, final ObjectNode provenance) {
		logger.debug("Job {} is finished", id);

		if (nonNull(logToAppend)) {
			appendJobLog(id, logToAppend);
		}

		final var job = getJob(id);
		final var outputData = new OutputData(REPOSITORY);
		outputData.setFiles(outputs);
		job.setStatus(STATUS_FINISHED);
		job.setOutputData(outputData);
		job.setTimestampCompletion(new DateTime(UTC));
		job.setProvenance(provenance);

		logger.debug("Updating job status on server");
		queue.updateJob(nmpiApiKey, id, job);

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

		final var job = getJob(id);
		final var outputData = new OutputData(REPOSITORY);
		outputData.setFiles(outputs);
		job.setStatus(STATUS_ERROR);
		job.setTimestampCompletion(new DateTime(UTC));
		job.setOutputData(outputData);
		job.setProvenance(provenance);

		logger.debug("Updating job on server");
		queue.updateJob(nmpiApiKey, id, job);

		jobLog.remove(id);
		jobCache.remove(id);
	}

	/**
	 * Close the manager.
	 */
	public void close() {
		done = true;
	}
}
