/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/**
 *
 * @author Christian-B
 */
@JsonFormat(shape = ARRAY)
public class Vertex {

    final String label;
    final int recordingRegionBaseAddress;
    //No reason this can not be a list but int is required
    final int[] recordedRegionIds;

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
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }

    //hashcode

//    int getRegionBufferSize(Integer region) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }

//    boolean isEmpty(Integer region) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }

//    boolean isNextTimestamp(Integer region) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }

//    Integer getNextTimestamp(Integer region) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }

//    boolean isNextKey(Integer region, Integer nextTimestamp) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }

//    int getNextKey(Integer region) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

    // AbstractReceiveBuffersToHost.get_recording_region_base_address
    /**
     * Get the recording region base address.
     *
     * @param transceiver  the SpiNNMan instance
     * @param placement the placement object of the core to find the address of
     * @return the base address of the recording region
     */
    int getRecordingRegionBaseAddress() {
        return recordingRegionBaseAddress;
    }
}
