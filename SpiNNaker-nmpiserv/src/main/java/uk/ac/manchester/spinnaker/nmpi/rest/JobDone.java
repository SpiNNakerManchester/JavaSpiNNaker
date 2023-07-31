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
package uk.ac.manchester.spinnaker.nmpi.rest;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import uk.ac.manchester.spinnaker.nmpi.model.DateTimeSerialiser;
import uk.ac.manchester.spinnaker.nmpi.model.OutputData;

/**
 * A Job where only the parts required for completion are set.
 */
public class JobDone {

	private String status;

	@JsonSerialize(using = DateTimeSerialiser.class)
	private DateTime timestampCompletion;

	private OutputData outputData;

	private ObjectNode provenance;

	/**
	 * Create a job with only a status.
	 *
	 * @param status The status to set.
	 */
	public JobDone(String status) {
		this.status = status;
	}

	/**
	 * Get the status.
	 *
	 * @return The status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Set the status.
	 *
	 * @param status The status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Get the timestampCompletion.
	 *
	 * @return the timestampCompletion
	 */
	public DateTime getTimestampCompletion() {
		return timestampCompletion;
	}

	/**
	 * Sets the timestampCompletion.
	 *
	 * @param timestampCompletionParam the timestampCompletion to set
	 */
	public void setTimestampCompletion(
			final DateTime timestampCompletionParam) {
		this.timestampCompletion = timestampCompletionParam;
	}

	/**
	 * Get the outputData.
	 *
	 * @return the outputData
	 */
	public OutputData getOutputData() {
		return outputData;
	}

	/**
	 * Sets the outputData.
	 *
	 * @param outputDataParam the outputData to set
	 */
	public void setOutputData(final OutputData outputDataParam) {
		this.outputData = outputDataParam;
	}

	/**
	 * Get the provenance.
	 *
	 * @return the provenance
	 */
	public ObjectNode getProvenance() {
		return provenance;
	}

	/**
	 * Sets the provenance.
	 *
	 * @param provenanceParam the provenance to set
	 */
	public void setProvenance(final ObjectNode provenanceParam) {
		this.provenance = provenanceParam;
	}
}
