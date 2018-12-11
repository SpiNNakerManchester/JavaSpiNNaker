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
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.storage_objects;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.RegionLocation;
import uk.ac.manchester.spinnaker.storage.BufferManagerDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.ConnectionProvider;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.storage.sqlite.SQLiteStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 * Stores the information received through the buffering output technique from
 * the SpiNNaker system.
 * <p>
 * The data kept includes the last sent packet and last received packet, their
 * correspondent sequence numbers, the data retrieved, a flag to identify if the
 * data from a core has been flushed and the final state of the buffering output
 * state machine.
 *
 * @see <a href=
 * "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/storage_objects/buffered_receiving_data.py">
 *      Python Version</a>
 * @author Christian-B
 */
public class BufferedReceivingData {

    /** The physical storage of the data. */
    private final BufferManagerStorage storage;

    /** Map of booleans indicating if a region on a core has been flushed. */
    private final Map<RegionLocation, Boolean> isFlushed;

    /** Map of last sequence number received by core. */
    private final Map<CoreLocation, Integer> sequenceNo;

        // In python but unused in this prototype
        //# dict of last packet received by core
        //"_last_packet_received",

    /** Map of last packet sent by core. */
    //private final Map<CoreLocation, HostDataRead> lastPacketSent;

    /** Map of end buffer sequence number. */
    private final Map<CoreLocation, Integer> endBufferingSequenceNo;

    /** Map of end state by core. */
    private final Map<RegionLocation, ChannelBufferState> endBufferingState;

	private static final int DEFAULT_SEQUENCE_NUMBER = 0xFF;

	/**
	 * Stores the information received through the buffering output technique
	 * from the SpiNNaker system.
	 *
	 * @param databasePath
	 *            The path of a file that contains an SQLite database holding
	 *            the data.
	 */
    public BufferedReceivingData(String databasePath) {
        File databaseFile = new File(databasePath);
		ConnectionProvider engine =
				new BufferManagerDatabaseEngine(databaseFile);
		storage = new SQLiteStorage(engine);
        isFlushed =  new DefaultMap<>(false);
        sequenceNo = new DefaultMap<>(DEFAULT_SEQUENCE_NUMBER);
        //self._last_packet_received = defaultdict(lambda: None)
        //lastPacketSent = new HashMap<>();
        endBufferingSequenceNo = new HashMap<>();
        endBufferingState = new HashMap<>();
    }

    // Resets states so that it can behave in a resumed mode.
    //public void resume() {
        //self._end_buffering_state = dict()
        //self._is_flushed = defaultdict(lambda: False)
        //self._sequence_no = defaultdict(lambda: 0xFF)
        //self._last_packet_received = defaultdict(lambda: None)
        //self._last_packet_sent = defaultdict(lambda: None)
        //self._end_buffering_sequence_no = dict()
    //}

	/**
	 * Determine if the last sequence number has been retrieved.
	 *
	 * @param location
	 *            Location to check retrieved from.
	 * @return True if the number has been retrieved
	 */
	public boolean isEndBufferingSequenceNumberStored(CoreLocation location) {
		return endBufferingSequenceNo.containsKey(location);
	}

	/**
	 * Determine if the last sequence number has been retrieved.
	 *
	 * @param location
	 *            Location to check retrieved from.
	 * @return True if the number has been retrieved
	 */
	public boolean isEndBufferingSequenceNumberStored(
			HasCoreLocation location) {
		return isEndBufferingSequenceNumberStored(location.asCoreLocation());
	}

	/**
	 * Store the last sequence number sent by the core.
	 *
	 * @param location
	 *            The core retrieved from.
	 * @param lastSequenceNumber
	 *            he last sequence number
	 */
	public void storeEndBufferingSequenceNumber(CoreLocation location,
			Integer lastSequenceNumber) {
		endBufferingSequenceNo.put(location, lastSequenceNumber);
	}

	/**
	 * Store the last sequence number sent by the core.
	 *
	 * @param location
	 *            The core retrieved from.
	 * @param lastSequenceNumber
	 *            he last sequence number
	 */
	public void storeEndBufferingSequenceNumber(HasCoreLocation location,
			Integer lastSequenceNumber) {
		storeEndBufferingSequenceNumber(location.asCoreLocation(),
				lastSequenceNumber);
	}

	/**
	 * Get the last sequence number sent by the core.
	 *
	 * @param location
	 *            The core
	 * @return The last sequence number.
	 */
	public Integer getEndBufferingSequenceNumber(CoreLocation location) {
        if (endBufferingSequenceNo.containsKey(location)) {
            return endBufferingSequenceNo.get(location);
        }
        throw new IllegalArgumentException(
                "no squence number know for " + location);
    }

	/**
	 * Get the last sequence number sent by the core.
	 *
	 * @param location
	 *            The core
	 * @return The last sequence number.
	 */
	public Integer getEndBufferingSequenceNumber(HasCoreLocation location) {
		return getEndBufferingSequenceNumber(location.asCoreLocation());
	}

	/**
	 * Check if the data region has been flushed.
	 *
	 * @param location
	 *            The X, Y, P and Region
	 * @return True if the region has been flushed. False otherwise.
	 */
	public boolean isDataFromRegionFlushed(RegionLocation location) {
		return isFlushed.containsKey(location);
	}

	/**
	 * Determine if the end state has been stored.
	 *
	 * @param location
	 *            The X, Y, P and Region
	 * @return True if the state has been stored.
	 */
	public boolean isEndBufferingStateRecovered(RegionLocation location) {
		return endBufferingState.containsKey(location);
	}

	/**
	 * Store the end state of buffering.
	 *
	 * @param location
	 *            The X, Y, P and Region
	 * @param state
	 *            The end state
	 */
	public void storeEndBufferingState(RegionLocation location,
			ChannelBufferState state) {
		endBufferingState.put(location, state);
	}

	/**
	 * Get the end state of the buffering.
	 *
	 * @param location
	 *            The X, Y, P and Region
	 * @return The end state
	 */
	public ChannelBufferState getEndBufferingState(RegionLocation location) {
		if (endBufferingState.containsKey(location)) {
			return endBufferingState.get(location);
		}
		throw new IllegalArgumentException("no state know for " + location);
	}

	/**
	 * Get the last sequence number for a core.
	 *
	 * @param location
	 *            The Core
	 * @return Last sequence number used
	 */
	public Integer lastSequenceNoForCore(CoreLocation location) {
		return this.sequenceNo.get(location);
	}

	/**
	 * Get the last sequence number for a core.
	 *
	 * @param location
	 *            The Core
	 * @return Last sequence number used
	 */
	public Integer lastSequenceNoForCore(HasCoreLocation location) {
		return this.sequenceNo.get(location.asCoreLocation());
	}

	/**
	 * Store some information in the correspondent buffer class for a specific
	 * chip, core and region.
	 *
	 * @param location
	 *            The X, Y, P and Region
	 * @param data
	 *            data to be stored
	 * @throws StorageException
	 *             If there is a problem storing the data.
	 */
	public void storeDataInRegionBuffer(RegionLocation location,
			ByteBuffer data) throws StorageException {
		storage.storeRegionContents(
				new Region(location.asCoreLocation(), location.region, 0, 0),
				data);
	}

	/**
	 * Store flushed data from a region of a core on a chip, and mark it as
	 * being flushed.
	 *
	 * @param location
	 *            The X, Y, P and Region
	 * @param data
	 *            data to be stored
	 * @throws StorageException
	 *             If there is a problem storing the data.
	 */
	public void flushingDataFromRegion(RegionLocation location, ByteBuffer data)
			throws StorageException {
		storeDataInRegionBuffer(location, data);
		isFlushed.put(location, true);
	}

    /**
     * Get the last packet received for a given core.
     *
     * @param location The Core
     * @return last HostDataRead packet sent
     * /
    public HostDataRead lastSentPacketToCore(CoreLocation location) {
        return lastPacketSent.get(location);
    }*/

    /**
     * Get the last packet received for a given core.
     *
     * @param location The Core
     * @return last HostDataRead packet sent
     * /
    public HostDataRead lastSentPacketToCore(HasCoreLocation location) {
        return lastPacketSent.get(location.asCoreLocation());
    }*/

    //BufferedDataStorage getRegionDataPointer(RegionLocation location) {
    //    throw new UnsupportedOperationException("Not supported yet.");
    //}
}
