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
package uk.ac.manchester.spinnaker.front_end.download;

import static org.slf4j.LoggerFactory.getLogger;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.RegionLocation;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
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
	private static final Logger log = getLogger(BufferedReceivingData.class);

	/** The physical storage of the data. */
	private final BufferManagerStorage storage;

	/** Map of booleans indicating if a region on a core has been flushed. */
	private final Map<RegionLocation, Boolean> isFlushed;

	/** Map of last sequence number received by core. */
	private final Map<CoreLocation, Integer> sequenceNo;

	/** Map of end buffer sequence number. */
	private final Map<CoreLocation, Integer> endBufferingSequenceNo;

	/** Map of end state by core. */
	private final Map<RegionLocation, ChannelBufferState> endBufferingState;

	private static final int DEFAULT_SEQUENCE_NUMBER = 0xFF;

	/**
	 * Stores the information received through the buffering output technique
	 * from the SpiNNaker system.
	 *
	 * @param storage
	 *            How to talk to the database.
	 */
	public BufferedReceivingData(BufferManagerStorage storage) {
		this.storage = storage;
		isFlushed = new DefaultMap<>(false);
		sequenceNo = new DefaultMap<>(DEFAULT_SEQUENCE_NUMBER);
		endBufferingSequenceNo = new HashMap<>();
		endBufferingState = new HashMap<>();
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
		return endBufferingSequenceNo.containsKey(location.asCoreLocation());
	}

	/**
	 * Store the last sequence number sent by the core.
	 *
	 * @param location
	 *            The core retrieved from.
	 * @param lastSequenceNumber
	 *            The last sequence number
	 */
	public void storeEndBufferingSequenceNumber(HasCoreLocation location,
			int lastSequenceNumber) {
		synchronized (endBufferingSequenceNo) {
			endBufferingSequenceNo.put(location.asCoreLocation(),
					lastSequenceNumber);
		}
	}

	/**
	 * Get the last sequence number sent by the core.
	 *
	 * @param location
	 *            The core
	 * @return The last sequence number.
	 */
	public int getEndBufferingSequenceNumber(CoreLocation location) {
		Integer value = endBufferingSequenceNo.get(location);
		if (value == null) {
			throw new IllegalArgumentException(
					"no squence number known for " + location);
		}
		return value;
	}

	/**
	 * Get the last sequence number sent by the core.
	 *
	 * @param location
	 *            The core
	 * @return The last sequence number.
	 */
	public int getEndBufferingSequenceNumber(HasCoreLocation location) {
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
		synchronized (endBufferingState) {
			endBufferingState.put(location, state);
		}
	}

	/**
	 * Get the end state of the buffering.
	 *
	 * @param location
	 *            The X, Y, P and Region
	 * @return The end state
	 */
	public ChannelBufferState getEndBufferingState(RegionLocation location) {
		ChannelBufferState value = endBufferingState.get(location);
		if (value == null) {
			throw new IllegalArgumentException(
					"no state known for " + location);
		}
		return value;
	}

	/**
	 * Get the last sequence number for a core.
	 *
	 * @param location
	 *            The Core
	 * @return Last sequence number used
	 */
	public Integer lastSequenceNoForCore(CoreLocation location) {
		return sequenceNo.get(location);
	}

	/**
	 * Get the last sequence number for a core.
	 *
	 * @param location
	 *            The Core
	 * @return Last sequence number used
	 */
	public Integer lastSequenceNoForCore(HasCoreLocation location) {
		return sequenceNo.get(location.asCoreLocation());
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
		if (log.isInfoEnabled()) {
			log.info("retrieved {} bytes from region {} of {}",
					data.remaining(), location.region,
					location.asCoreLocation());
		}
		storage.appendRecordingContents(
				new Region(location, location.region, 0, 0), data);
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
}
