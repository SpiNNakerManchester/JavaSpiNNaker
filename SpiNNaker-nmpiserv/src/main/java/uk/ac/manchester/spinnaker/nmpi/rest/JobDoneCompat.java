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

import java.util.List;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import uk.ac.manchester.spinnaker.nmpi.model.DateTimeSerialiser;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.DataItem;

/**
 * A Job where only the parts required for completion are set.
 */
public class JobDoneCompat {
	private int id;

	private String status;

	@JsonSerialize(using = DateTimeSerialiser.class)
	private DateTime timestampCompletion;

	private List<DataItem> outputData;

	private ObjectNode provenance;

	/**
	 * Create a job with only a status.
	 *
	 * @param id
	 *            The job id.
	 * @param status
	 *            The status to set.
	 */
	public JobDoneCompat(int id, String status) {
		this.id = id;
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
	 * @param status
	 *            The status to set
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
	 * @param timestampCompletion
	 *            the timestampCompletion to set
	 */
	public void setTimestampCompletion(DateTime timestampCompletion) {
		this.timestampCompletion = timestampCompletion;
	}

	/**
	 * Get the output data items.
	 *
	 * @return the outputData
	 */
	public List<DataItem> getOutputData() {
		return outputData;
	}

	/**
	 * Sets the outputData.
	 *
	 * @param outputData
	 *            the outputData to set
	 */
	public void setOutputData(List<DataItem> outputData) {
		this.outputData = outputData;
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
	 * @param provenance
	 *            the provenance to set
	 */
	public void setProvenance(ObjectNode provenance) {
		this.provenance = provenance;
	}

	/**
	 * Get the id.
	 *
	 * @return The id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the id.
	 *
	 * @param id
	 *            The id
	 */
	public void setId(int id) {
		this.id = id;
	}
}
