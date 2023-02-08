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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 * Vertex recording region information.
 *
 * @author Christian-B
 * @param label
 *            Label as received from Python.
 * @param base
 *            Address at which to find recording metadata. This was allocated
 *            during data specification execution, and contains a description of
 *            <i>all</i> the recording regions owned by a vertex (it may well
 *            have several, e.g., spikes and voltages). This points to a
 *            structure that includes the addresses of the per-region metadata,
 *            and those point in turn to the actual buffers used to do the
 *            recording (which are <i>circular</i> buffers).
 * @param recordedRegionIds
 *            The recording region IDs that have been actually recorded using
 *            buffering.
 */
@JsonFormat(shape = OBJECT)
public record Vertex(String label, @NotNull MemoryLocation base,
		@NotNull int[] recordedRegionIds) {
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
	@JsonCreator
	public Vertex(//
			@JsonProperty(value = "label", required = true) String label,
			@JsonProperty(value = "recordingRegionBaseAddress", //
					required = true) long recordingRegionBaseAddress,
			@JsonProperty(value = "recordedRegionIds", required = true) //
			int[] recordedRegionIds) {
		this(label, new MemoryLocation(recordingRegionBaseAddress),
				recordedRegionIds);
	}
}
