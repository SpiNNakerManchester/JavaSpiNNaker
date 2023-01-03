/*
 * Copyright (c) 2018-2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.front_end.download.request;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
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
