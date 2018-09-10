/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import java.nio.ByteBuffer;
import java.util.HashMap;
import uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.storage_objects.ChannelBufferState;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 *
 * @author Christian-B
 */
class BufferedReceivingData {

        // # the data to store
    private final HashMap<CoreLocation, HashMap<Integer, BufferedDataStorage>> data;

        //# dict of booleans indicating if a region on a core has been flushed
    private final DefaultMap<CoreLocation, Boolean> isFlushed;

    // dict of last sequence number received by core
    private final DefaultMap<CoreLocation, Integer> sequenceNo;

        //# dict of last packet received by core
        //"_last_packet_received",

        //# dict of last packet sent by core
        //"_last_packet_sent",

    //# dict of end buffer sequence number
    private final HashMap<CoreLocation, Integer> endBufferingSequenceNo;

        //# dict of end state by core
    private final HashMap<CoreLocation, HashMap<Integer, ChannelBufferState>> endBufferingState;

    public BufferedReceivingData(boolean storeToFile) {
        data = new HashMap<>();
        //if store_to_file:
        //    self._data = defaultdict(BufferedTempfileDataStorage)
        //else:
        //    self._data = defaultdict(BufferedBytearrayDataStorage)
        isFlushed = new DefaultMap<>(false);
        sequenceNo = new DefaultMap<>(0xFF);
        //self._last_packet_received = defaultdict(lambda: None)
        //self._last_packet_sent = defaultdict(lambda: None)
        endBufferingSequenceNo = new HashMap<>();
        endBufferingState = new HashMap<>();
    }

    public void resume() {
        //self._end_buffering_state = dict()
        //self._is_flushed = defaultdict(lambda: False)
        //self._sequence_no = defaultdict(lambda: 0xFF)
        //self._last_packet_received = defaultdict(lambda: None)
        //self._last_packet_sent = defaultdict(lambda: None)
        //self._end_buffering_sequence_no = dict()
    }

    public boolean isEndBufferingSequenceNumberStored(CoreLocation location) {
        return endBufferingSequenceNo.containsKey(this);
    }

    public boolean isEndBufferingSequenceNumberStored(HasCoreLocation location) {
        return isEndBufferingSequenceNumberStored(location.asCoreLocation());
    }

    public void storeEndBufferingSequenceNumber(CoreLocation location, Integer lastSequenceNumber) {
        endBufferingSequenceNo.put(location, lastSequenceNumber);
    }

    public void storeEndBufferingSequenceNumber(HasCoreLocation location, Integer lastSequenceNumber) {
        storeEndBufferingSequenceNumber(location.asCoreLocation(), lastSequenceNumber);
    }

    public Integer getEndBufferingSequenceNumber(CoreLocation location) {
        if (endBufferingSequenceNo.containsKey(location)) {
            return endBufferingSequenceNo.get(location);
        }
        throw new IllegalArgumentException("no squence number know for " + location);
    }

    public Integer getEndBufferingSequenceNumber(HasCoreLocation location) {
        return getEndBufferingSequenceNumber(location.asCoreLocation());
    }

    public boolean isDataFromRegionFlushed(CoreLocation location, Integer recordingRegionId) {
        return isFlushed.get(location);
    }

    public boolean isDataFromRegionFlushed(HasCoreLocation location, Integer recordingRegionId) {
        return isDataFromRegionFlushed(location.asCoreLocation(), recordingRegionId);
    }

    public boolean isEndBufferingStateRecovered(HasCoreLocation location, int recordingRegionId) {
        return isEndBufferingStateRecovered(location.asCoreLocation(), recordingRegionId);
    }

    public boolean isEndBufferingStateRecovered(CoreLocation location, int recordingRegionId) {
        if (endBufferingState.containsKey(location)) {
            HashMap inner = endBufferingState.get(location);
            return inner.containsKey(recordingRegionId);
        } else {
            return false;
        }
    }

    public void storeEndBufferingState(CoreLocation location,
            int recordingRegionId, ChannelBufferState state) {
        HashMap<Integer, ChannelBufferState> inner;
        if (endBufferingState.containsKey(location)) {
            inner = endBufferingState.get(location);
        } else {
            inner = new HashMap<>();
            endBufferingState.put(location, inner);
        }
        inner.put(recordingRegionId, state);
    }

    public void storeEndBufferingState(HasCoreLocation location,
           int recordingRegionId, ChannelBufferState state) {
        storeEndBufferingState(location.asCoreLocation(),
                recordingRegionId, state);
    }

    public ChannelBufferState getEndBufferingState(CoreLocation location,
            int recordingRegionId) {
        HashMap<Integer, ChannelBufferState> inner;
        if (endBufferingState.containsKey(location)) {
            inner = endBufferingState.get(location);
        } else {
            throw new IllegalArgumentException("no state know for " + location);
        }
        if (inner.containsKey(recordingRegionId)) {
            return inner.get(recordingRegionId);
        } else {
            throw new IllegalArgumentException("no state know for " + location
                    + " and region " + recordingRegionId);
        }
    }

    public ChannelBufferState getEndBufferingState(HasCoreLocation location,
            int recordingRegionId) {
        return getEndBufferingState(location.asCoreLocation(), recordingRegionId);
    }

    public Integer lastSequenceNoForCore(CoreLocation location) {
        return this.sequenceNo.get(location);
    }

    public Integer lastSequenceNoForCore(HasCoreLocation location) {
        return this.sequenceNo.get(location.asCoreLocation());
    }

    private BufferedDataStorage getRegionBuffer(CoreLocation location, Integer recordingRegionId) {
        HashMap<Integer, BufferedDataStorage> inner;
        if (data.containsKey(location)) {
            inner = data.get(location);
        } else {
            inner = new HashMap<>();
            data.put(location, inner);
        }
        BufferedDataStorage store;
        if (inner.containsKey(recordingRegionId)) {
            store = inner.get(recordingRegionId);
        } else {
            store = new BufferedDataStorage();
            inner.put(recordingRegionId, store);
        }
        return store;
    }

    public void storeDataInRegionBuffer(
            CoreLocation location, int recordingRegionId, ByteBuffer data) {
        BufferedDataStorage store = getRegionBuffer(location, recordingRegionId);
        store.write(data);
    }

    public void flushingDataFromRegion(CoreLocation location, int recordingRegionId, ByteBuffer data) {

    }

 }
