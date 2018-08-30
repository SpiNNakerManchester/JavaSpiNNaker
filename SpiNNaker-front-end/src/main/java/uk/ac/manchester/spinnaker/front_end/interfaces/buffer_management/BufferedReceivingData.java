/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import java.util.HashMap;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 *
 * @author Christian-B
 */
class BufferedReceivingData {

        // # the data to store
        //"_data",

        //# dict of booleans indicating if a region on a core has been flushed
    private final DefaultMap<CoreLocation, Boolean> isFlushed;

        //# dict of last sequence number received by core
        //"_sequence_no",

        //# dict of last packet received by core
        //"_last_packet_received",

        //# dict of last packet sent by core
        //"_last_packet_sent",

    //# dict of end buffer sequence number
    private final HashMap<CoreLocation, Integer> endBufferingSequenceNo;

        //# dict of end state by core
        //"_end_buffering_state"

    BufferedReceivingData(boolean storeToFile) {
        //self._data = None
        //if store_to_file:
        //    self._data = defaultdict(BufferedTempfileDataStorage)
        //else:
        //    self._data = defaultdict(BufferedBytearrayDataStorage)
        isFlushed = new DefaultMap<>(false);
        //self._sequence_no = defaultdict(lambda: 0xFF)
        //self._last_packet_received = defaultdict(lambda: None)
        //self._last_packet_sent = defaultdict(lambda: None)
        endBufferingSequenceNo = new HashMap<>();
        //self._end_buffering_state = dict()

    }

    void resume() {
        //self._end_buffering_state = dict()
        //self._is_flushed = defaultdict(lambda: False)
        //self._sequence_no = defaultdict(lambda: 0xFF)
        //self._last_packet_received = defaultdict(lambda: None)
        //self._last_packet_sent = defaultdict(lambda: None)
        //self._end_buffering_sequence_no = dict()
    }

    boolean isEndBufferingSequenceNumberStored(CoreLocation location) {
        return endBufferingSequenceNo.containsKey(this);
    }

    boolean isEndBufferingSequenceNumberStored(HasCoreLocation location) {
        return isEndBufferingSequenceNumberStored(location.asCoreLocation());
    }

    void storeEndBufferingSequenceNumber(CoreLocation location, Integer lastSequenceNumber) {
        endBufferingSequenceNo.put(location, lastSequenceNumber);
    }

    void storeEndBufferingSequenceNumber(HasCoreLocation location, Integer lastSequenceNumber) {
        storeEndBufferingSequenceNumber(location.asCoreLocation(), lastSequenceNumber);
    }

    boolean isDataFromRegionFlushed(CoreLocation location, Integer recordingRegionId) {
        return isFlushed.get(location);
    }

    boolean isDataFromRegionFlushed(HasCoreLocation location, Integer recordingRegionId) {
        return isDataFromRegionFlushed(location.asCoreLocation(), recordingRegionId);
    }
}
