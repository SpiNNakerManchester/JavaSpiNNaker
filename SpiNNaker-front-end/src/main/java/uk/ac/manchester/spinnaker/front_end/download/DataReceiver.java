/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.front_end.download;

import static java.nio.ByteBuffer.allocate;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.download.RecordingRegion.getRecordingRegionDescriptors;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BoardLocalSupport;
import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.RegionLocation;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * Stripped down version of the BufferManager for early testing.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/buffer_manager.py">
 *      Python Version</a>
 *
 * @author Christian-B
 */
public class DataReceiver extends BoardLocalSupport {
	private final BufferedReceivingData receivedData;

	private static final Logger log = getLogger(DataReceiver.class);

	/**
	 * Creates a new mini Buffer Manager.
	 *
	 * @param tranceiver
	 *            Transceiver to get data via.
	 * @param machine
	 *            The SpiNNaker system to get the data from.
	 * @param storage
	 *            How to talk to the database.
	 */
	public DataReceiver(TransceiverInterface tranceiver, Machine machine,
			BufferManagerStorage storage) {
		super(tranceiver, machine);
		// storage area for received data from cores
		receivedData = new BufferedReceivingData(storage);
	}

	private Stream<List<Placement>> partitionByBoard(
			List<Placement> placements) {
		var map = new HashMap<Object, List<Placement>>();
		placements.forEach(
				p -> map.computeIfAbsent(machine.getChipAt(p).nearestEthernet,
						__ -> new ArrayList<>()).add(p));
		return map.values().stream();
	}

	/**
	 * Gets the data for a list of placements in parallel.
	 *
	 * @param placements
	 *            List of placements.
	 * @param parallelFactor
	 *            Number of threads to use.
	 * @throws IOException
	 *             if communications fail
	 * @throws ProcessException
	 *             if SpiNNaker rejects a message
	 * @throws StorageException
	 *             if database access fails
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public void getDataForPlacementsParallel(List<Placement> placements,
			int parallelFactor) throws IOException, StorageException,
			ProcessException, InterruptedException {
		try (var exec = new BasicExecutor(parallelFactor)) {
			// get data on a by-the-board basis
			exec.submitTasks(partitionByBoard(placements), places -> {
				return () -> getDataForPlacements(places);
			}).awaitAndCombineExceptions();
		} catch (IOException | StorageException | ProcessException
				| RuntimeException | InterruptedException e) {
			throw e;
		} catch (Exception e) {
			// CHECKSTYLE:OFF
			// This code should be unreachable
			throw new RuntimeException("unexpected exception", e);
			// CHECKSTYLE:ON
		}
	}

	/**
	 * Gets the data for a list of placements.
	 * <p>
	 * Note: This method is subject to change as best way to pass in placement
	 * data is determined.
	 *
	 * @param placements
	 *            List of placements.
	 * @throws IOException
	 *             if communications fail
	 * @throws ProcessException
	 *             if SpiNNaker rejects a message
	 * @throws StorageException
	 *             if database access fails
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public void getDataForPlacements(List<Placement> placements)
			throws IOException, StorageException, ProcessException,
			InterruptedException {
		// get data
		try (var c = new BoardLocal(placements.get(0))) {
			for (var placement : placements) {
				for (int recordingRegionId : placement.getVertex()
						.getRecordedRegionIds()) {
					getDataForPlacement(placement, recordingRegionId);
				}
			}
		}
	}

	private void getDataForPlacement(Placement placement, int recordingRegionId)
			throws IOException, StorageException, ProcessException,
			InterruptedException {
		// Combine placement.x, placement.y, placement.p, recording_region_id
		var location = new RegionLocation(placement, recordingRegionId);

		// Read the data if not already received
		if (receivedData.isDataFromRegionFlushed(location)) {
			return;
		}

		// Ensure the recording regions are stored
		var coreLocation = location.asCoreLocation();
		if (!receivedData.isRecordingRegionsStored(coreLocation)) {
			var regions = getRecordingRegionDescriptors(txrx, placement);
			receivedData.storeRecordingRegions(coreLocation, regions);
		}

		// Read the data
		var region = receivedData.getRecordingRegion(location);
		readSomeData(location, region.data, region.size);
	}

	private static final long MAX_UINT = 0xFFFFFFFFL;

	private static boolean is32bit(long value) {
		return value >= 0 && value <= MAX_UINT;
	}

	private void readSomeData(RegionLocation location, MemoryLocation address,
			long length) throws IOException, StorageException, ProcessException,
			InterruptedException {
		if (!is32bit(length)) {
			throw new IllegalArgumentException("non-32-bit argument");
		}
		if (log.isDebugEnabled()) {
			log.debug("< Reading {} bytes from {} at {}", length, location,
					address);
		}
		var data = requestData(location, address, (int) length);
		receivedData.flushingDataFromRegion(location, data);
	}

	/**
	 * Read memory from SDRAM on SpiNNaker.
	 *
	 * @param location
	 *            The coords where data is to be extracted from.
	 * @param address
	 *            The memory address to start at
	 * @param length
	 *            The number of bytes to extract
	 * @return data as a byte array
	 * @throws IOException
	 *             if communications fail
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	private ByteBuffer requestData(HasCoreLocation location,
			MemoryLocation address, int length)
			throws IOException, ProcessException, InterruptedException {
		if (length < 1) {
			// Crazy negative lengths get an exception
			return allocate(length);
		}
		return txrx.readMemory(location.getScampCore(), address, length);
	}
}
