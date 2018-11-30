/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Prototype with minimum information needed.
 *
 * @author Christian-B
 */
@JsonFormat(shape = OBJECT)
public class Vertex {

    /** Label as received from Python. */
    final String label;

    /** Address at which to start recording. */
    final int recordingRegionBaseAddress;

    /** The IDs of the regions recording. */
    private final int[] recordedRegionIds;

	/**
	 * Create a minimal vertex, possibly using an unmarshaller.
	 *
	 * @param label
	 *            Label as received from Python.
	 * @param recordingRegionBaseAddress
	 *            Address at which to start recording.
	 * @param recordedRegionIds
	 *            The IDs of the regions recording
	 */
    public Vertex(@JsonProperty(value = "label", required = true) String label,
            @JsonProperty(value = "recordingRegionBaseAddress", required = true)
                    int recordingRegionBaseAddress,
            @JsonProperty(value = "recordedRegionIds", required = true)
                    int[] recordedRegionIds) {
        this.label = label;
        this.recordingRegionBaseAddress = recordingRegionBaseAddress;
        this.recordedRegionIds = recordedRegionIds;
    }

//    Iterable<Integer> getRegions() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

    //hashcode

//    int getRegionBufferSize(Integer region) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

//    boolean isEmpty(Integer region) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

//    boolean isNextTimestamp(Integer region) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

//    Integer getNextTimestamp(Integer region) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

//    boolean isNextKey(Integer region, Integer nextTimestamp) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

//    int getNextKey(Integer region) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

    // AbstractReceiveBuffersToHost.get_recorded_region_ids
    /**
     * Get the recording region IDs that have been recorded using buffering.
     *
     * @return The region numbers that have active recording
     */
    int[] getRecordedRegionIds() {
        return recordedRegionIds;
    }

    /**
     * Get the recording region base address.
     * <p>
     * Unlike the python this value is cached here.
     *
     * @return the base address of the recording region
     */
    int getRecordingRegionBaseAddress() {
        return recordingRegionBaseAddress;
    }
}
