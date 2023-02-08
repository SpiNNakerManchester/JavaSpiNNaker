/*
 * Copyright (c) 2018-2019 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.front_end.download.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 * Vertex recording region information.
 *
 * @author Christian-B
 */
@JsonFormat(shape = OBJECT)
public class Vertex {

	/** Label as received from Python. */
	private final String label;

	/**
	 * Address at which to find recording metadata. This was allocated during
	 * data specification execution, and contains a description of <i>all</i>
	 * the recording regions owned by a vertex (it may well have several, e.g.,
	 * spikes and voltages). This points to a structure that includes the
	 * addresses of the per-region metadata, and those point in turn to the
	 * actual buffers used to do the recording (which are <i>circular</i>
	 * buffers).
	 */
	private final long recordingRegionBaseAddress;

	/**
	 * Address at which to find recording metadata. This was allocated during
	 * data specification execution, and contains a description of <i>all</i>
	 * the recording regions owned by a vertex (it may well have several, e.g.,
	 * spikes and voltages). This points to a structure that includes the
	 * addresses of the per-region metadata, and those point in turn to the
	 * actual buffers used to do the recording (which are <i>circular</i>
	 * buffers).
	 */
	@NotNull
	private final MemoryLocation base;

	/** The IDs of the regions recording. */
	@NotNull
	private final int[] recordedRegionIds;

	/**
	 * Create a minimal vertex, possibly using an unmarshaller.
	 *
	 * @param label
	 *            Label as received from Python.
	 * @param recordingRegionBaseAddress
	 *            Address at which to find recording region metadata. This will
	 *            have been originally allocated by the data specification
	 *            execution process.
	 * @param recordedRegionIds
	 *            The IDs of the regions doing recording.
	 */
	public Vertex(@JsonProperty(value = "label", required = true) String label,
			@JsonProperty(value = "recordingRegionBaseAddress", required = true)
			long recordingRegionBaseAddress,
			@JsonProperty(value = "recordedRegionIds", required = true)
			int[] recordedRegionIds) {
		this.label = label;
		this.recordingRegionBaseAddress = recordingRegionBaseAddress;
		this.recordedRegionIds = recordedRegionIds;
		this.base = new MemoryLocation(recordingRegionBaseAddress);
	}

	/**
	 * Get the recording region IDs that have been recorded using buffering.
	 *
	 * @return The region numbers that have active recording
	 */
	public int[] getRecordedRegionIds() {
		return recordedRegionIds;
	}

	/**
	 * Get the recording region base address.
	 * <p>
	 * Unlike the python this value is cached here.
	 *
	 * @return the base address of the recording region
	 */
	public long getBaseAddress() {
		return recordingRegionBaseAddress;
	}

	/**
	 * Get the recording region base address.
	 *
	 * @return the base address of the recording region
	 */
	@JsonIgnore
	public MemoryLocation getBase() {
		return base;
	}

	/**
	 * @return The label, as received from Python.
	 */
	public String getLabel() {
		return label;
	}
}
