/*
 * Copyright (c) 2018 The University of Manchester
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
    final int[] recordedRegionIds;

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
