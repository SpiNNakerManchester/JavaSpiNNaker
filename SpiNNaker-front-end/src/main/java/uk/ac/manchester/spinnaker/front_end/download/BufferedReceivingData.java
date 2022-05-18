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

import static java.util.Collections.synchronizedMap;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.RegionLocation;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 * Stores the information received through the buffering output technique from
 * the SpiNNaker system.
 * <p>
 * The data kept includes the data retrieved, a flag to identify if the data
 * from a core has been flushed and the recording region sizes and states.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/storage_objects/buffered_receiving_data.py">
 *      Python Version</a>
 * @author Christian-B
 */
class BufferedReceivingData {
	private static final Logger log = getLogger(BufferedReceivingData.class);

	/** The physical storage of the data. */
	private final BufferManagerStorage storage;

	/** Map of booleans indicating if a region on a core has been flushed. */
	private final Map<RegionLocation, Boolean> isFlushed;

	/** Map of recording regions by core. */
	private final Map<CoreLocation, List<RecordingRegion>> recordingRegions;

	/**
	 * Stores the information received through the buffering output technique
	 * from the SpiNNaker system.
	 *
	 * @param storage
	 *            How to talk to the database.
	 */
	BufferedReceivingData(BufferManagerStorage storage) {
		this.storage = storage;
		isFlushed = new DefaultMap<>(false);
		recordingRegions = synchronizedMap(new HashMap<>());
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
	 * Determine if the recording regions have been stored.
	 *
	 * @param location
	 *            The X, Y and P
	 * @return True if the region information has been stored.
	 */
	public boolean isRecordingRegionsStored(CoreLocation location) {
		return recordingRegions.containsKey(location);
	}

	/**
	 * Store the recording region information.
	 *
	 * @param location
	 *            The X, Y and P
	 * @param regions
	 *            The recording region information
	 */
	void storeRecordingRegions(CoreLocation location,
			List<RecordingRegion> regions) {
		recordingRegions.put(location, regions);
	}

	/**
	 * Get the end state of the buffering.
	 *
	 * @param location
	 *            The X, Y, P and Region
	 * @return The end state
	 * @throws IllegalArgumentException
	 *             If the location doesn't have recording regions.
	 */
	public RecordingRegion getRecordingRegion(RegionLocation location) {
		var coreLocation = location.asCoreLocation();
		var value = recordingRegions.get(coreLocation);
		if (value == null) {
			throw new IllegalArgumentException(
					"no regions known for " + coreLocation);
		}
		if (value.size() < location.region || location.region < 0) {
			throw new IllegalArgumentException(
					"no region known for " + location);
		}
		return value.get(location.region);
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
