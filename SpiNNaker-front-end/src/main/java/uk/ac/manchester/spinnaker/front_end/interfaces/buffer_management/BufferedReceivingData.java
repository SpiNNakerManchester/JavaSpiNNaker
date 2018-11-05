/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.storage_objects.ChannelBufferState;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.RegionLocation;
import uk.ac.manchester.spinnaker.messages.eieio.HostDataRead;
import uk.ac.manchester.spinnaker.storage.ConnectionProvider;
import uk.ac.manchester.spinnaker.storage.DatabaseEngine;
import uk.ac.manchester.spinnaker.storage.SQLiteStorage;
import uk.ac.manchester.spinnaker.storage.Storage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 *
 * @author Christian-B
 */
class BufferedReceivingData {

        // # the data to store
    private final Storage storage;

        //# dict of booleans indicating if a region on a core has been flushed
    private final Map<RegionLocation, Boolean> isFlushed;

    // dict of last sequence number received by core
    private final Map<CoreLocation, Integer> sequenceNo;

        //# dict of last packet received by core
        //"_last_packet_received",

        //# dict of last packet sent by core
    private final Map<CoreLocation, HostDataRead> lastPacketSent;

    //# dict of end buffer sequence number
    private final Map<CoreLocation, Integer> endBufferingSequenceNo;

        //# dict of end state by core
    private final Map<RegionLocation, ChannelBufferState> endBufferingState;

    public BufferedReceivingData(String databasePath) {
        File databaseFile = new File(databasePath);
		ConnectionProvider engine = new DatabaseEngine(databaseFile);
		storage = new SQLiteStorage(engine);
        //if store_to_file:
        //    self._data = defaultdict(BufferedTempfileDataStorage)
        //else:
        //    self._data = defaultdict(BufferedBytearrayDataStorage)
        isFlushed =  new DefaultMap<>(false);
        sequenceNo = new DefaultMap<>(0xFF);
        //self._last_packet_received = defaultdict(lambda: None)
        lastPacketSent = new HashMap<>();
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

    public boolean isDataFromRegionFlushed(RegionLocation location) {
        return isFlushed.containsKey(location);
    }

    private void setFlushed(RegionLocation location, Boolean newValue) {
        isFlushed.put(location, newValue);
    }

    public boolean isEndBufferingStateRecovered(RegionLocation location) {
        return endBufferingState.containsKey(location);
    }

    public void storeEndBufferingState(
            RegionLocation location, ChannelBufferState state) {
        endBufferingState.put(location, state);
    }

    public ChannelBufferState getEndBufferingState(RegionLocation location) {
        if (endBufferingState.containsKey(location)) {
            return endBufferingState.get(location);
        } else {
            throw new IllegalArgumentException("no state know for " + location);
        }
    }

    public Integer lastSequenceNoForCore(CoreLocation location) {
        return this.sequenceNo.get(location);
    }

    public Integer lastSequenceNoForCore(HasCoreLocation location) {
        return this.sequenceNo.get(location.asCoreLocation());
    }

    public void storeDataInRegionBuffer(RegionLocation location, ByteBuffer data) throws StorageException {
        storage.appendRegionContents(location.asCoreLocation(), location.region, data);
    }

    public void flushingDataFromRegion(RegionLocation location, ByteBuffer data) throws StorageException {
        storeDataInRegionBuffer(location, data);
        setFlushed(location, true);
    }

    public HostDataRead lastSentPacketToCore(CoreLocation location) {
        return lastPacketSent.get(location);
    }

    public HostDataRead lastSentPacketToCore(HasCoreLocation location) {
        return lastPacketSent.get(location.asCoreLocation());
    }

    BufferedDataStorage getRegionDataPointer(RegionLocation location) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
 }
