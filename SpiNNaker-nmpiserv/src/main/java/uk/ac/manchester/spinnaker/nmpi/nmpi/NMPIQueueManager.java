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

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.DataItem;

/**
 * Manages the NMPI queue, receiving jobs and submitting them to be run.
 */
public interface NMPIQueueManager {

	/**
	 * Register a listener against the manager for new jobs.
	 *
	 * @param listener
	 *            The listener to register
	 */
	public void addListener(final NMPIQueueListener listener);

	public void processResponsesFromQueue();

	/**
	 * Appends log messages to the log.
	 *
	 * @param id
	 *            The ID of the job
	 * @param logToAppend
	 *            The messages to append
	 */
	public void appendJobLog(final int id, final String logToAppend);

	/**
	 * Mark a job as running.
	 *
	 * @param id
	 *            The ID of the job.
	 */
	public void setJobRunning(final int id);

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
			final List<DataItem> outputs, final ObjectNode provenance);

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
			final ObjectNode provenance);

	/**
	 * Close the manager.
	 */
	public void close();
}
