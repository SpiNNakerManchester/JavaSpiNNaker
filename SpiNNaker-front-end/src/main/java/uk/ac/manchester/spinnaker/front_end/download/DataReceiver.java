/*
 * Copyright (c) 2018-2019 The University of Manchester
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
import static uk.ac.manchester.spinnaker.front_end.download.storage_objects.BufferingOperation.BUFFER_WRITE;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor.Tasks;
import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.front_end.download.request.Vertex;
import uk.ac.manchester.spinnaker.front_end.download.storage_objects.BufferedReceivingData;
import uk.ac.manchester.spinnaker.front_end.download.storage_objects.BufferingOperation;
import uk.ac.manchester.spinnaker.front_end.download.storage_objects.ChannelBufferState;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.RegionLocation;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * Stripped down version of the BufferManager for early testing.
 *
 * @see <a href=
 * "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/buffer_manager.py">
 * Python Version</a>
 *
 * @author Christian-B
 */
public class DataReceiver {
	// found in SpiNNFrontEndCommon/spinn_front_end_common/interface/
	// buffer_management/recording_utilities.py
	/** The offset of the last sequence number field in bytes. */
	private static final int LAST_SEQUENCE_NUMBER_OFFSET = WORD_SIZE * 6;

	// found in SpiNNFrontEndCommon/spinn_front_end_common/interface/
	// buffer_management/recording_utilities.py
	/** The offset of the memory addresses in bytes. */
	private static final int FIRST_REGION_ADDRESS_OFFSET = WORD_SIZE * 7;

	private final Transceiver txrx;
	private final BufferedReceivingData receivedData;
	private final Machine machine;

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
	public DataReceiver(Transceiver tranceiver, Machine machine,
			BufferManagerStorage storage) {
		txrx = tranceiver;
		// storage area for received data from cores
		receivedData = new BufferedReceivingData(storage);
		this.machine = machine;
	}

	private Stream<List<Placement>> partitionByBoard(
			List<Placement> placements) {
		Map<ChipLocation, List<Placement>> map = new HashMap<>();
		for (Placement p : placements) {
			map.computeIfAbsent(
					machine.getChipAt(p).nearestEthernet.asChipLocation(),
					cl -> new ArrayList<>()).add(p);
		}
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
	 */
	public void getDataForPlacementsParallel(List<Placement> placements,
			int parallelFactor)
			throws IOException, StorageException, ProcessException {
		BasicExecutor exec = new BasicExecutor(parallelFactor);
		// Checkstyle gets the indentation rules wrong for the next statement.
		// CHECKSTYLE:OFF
		Tasks tasks = exec.submitTasks(
				// get data on a by-the-board basis
				partitionByBoard(placements)
						.map(places -> () -> getDataForPlacements(places)));
		// CHECKSTYLE:ON
		try {
			tasks.awaitAndCombineExceptions();
		} catch (IOException | StorageException | ProcessException
				| RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
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
	 */
	public void getDataForPlacements(List<Placement> placements)
			throws IOException, StorageException, ProcessException {
		// get data
		for (Placement placement : placements) {
			for (int recordingRegionId : placement.getVertex()
					.getRecordedRegionIds()) {
				getDataForPlacement(placement, recordingRegionId);
			}
		}
	}

	private void getDataForPlacement(Placement placement, int recordingRegionId)
			throws IOException, StorageException, ProcessException {
		Vertex vertex = placement.getVertex();
		int recordingDataAddress = vertex.getBaseAddress();
		// Combine placement.x, placement.y, placement.p, recording_region_id
		RegionLocation location =
				new RegionLocation(placement, recordingRegionId);

		// Ensure the last sequence number sent has been retrieved
		if (!receivedData.isEndBufferingSequenceNumberStored(placement)) {
			receivedData.storeEndBufferingSequenceNumber(placement,
					getLastSequenceNumber(placement, recordingDataAddress));
		}

		// Read the data if not already received
		if (receivedData.isDataFromRegionFlushed(location)) {
			return;
		}

		// Read the end state of the recording for this region
		ChannelBufferState endState;
		if (!receivedData.isEndBufferingStateRecovered(location)) {
			int regionPointer = getRegionPointer(placement,
					recordingDataAddress, recordingRegionId);
			endState = generateEndBufferingStateFromMachine(placement,
					regionPointer);
			receivedData.storeEndBufferingState(location, endState);
		} else {
			endState = receivedData.getEndBufferingState(location);
		}

		/*
		 * Current read needs to be adjusted in case the last portion of the
		 * memory has already been read, but the HostDataRead packet has not
		 * been processed by the chip before simulation finished. This situation
		 * is identified by the sequence number of the last packet sent to this
		 * core and the core internal state of the output buffering finite state
		 * machine.
		 */
		Integer seqNoLastAckPacket =
				receivedData.lastSequenceNoForCore(placement);

		// get the sequence number the core was expecting to see next
		int coreNextSequenceNumber =
				receivedData.getEndBufferingSequenceNumber(placement);

		/*
		 * if the core was expecting to see our last sent sequence, it must not
		 * have received it
		 */
		if (coreNextSequenceNumber == seqNoLastAckPacket) {
			throw new UnsupportedOperationException("Not supported yet.");
			// processLastAck(location, endState);
		}

		/*
		 * now state is updated, read back values for read pointer and last
		 * operation performed
		 */
		BufferingOperation lastOperation = endState.getLastBufferOperation();

		if (endState.getCurrentRead() < endState.currentWrite) {
			int length = endState.currentWrite - endState.getCurrentRead();
			readSomeData(location, endState.getCurrentRead(), length);
		} else if (endState.getCurrentRead() > endState.currentWrite
				|| lastOperation == BUFFER_WRITE) {
			int length = endState.endAddress - endState.getCurrentRead();
			if (length < 0) {
				throw new IOException(
						"The amount of data to read is negative!");
			}
			readSomeData(location, endState.getCurrentRead(), length);
			length = endState.currentWrite - endState.startAddress;
			readSomeData(location, endState.startAddress, length);
		} else {
			ByteBuffer data = ByteBuffer.allocate(0);
			receivedData.flushingDataFromRegion(location, data);
		}
	}

	private void readSomeData(RegionLocation location, int address, int length)
			throws IOException, StorageException, ProcessException {
		log.debug("< Reading " + length + " bytes from " + location + " at "
				+ address);
		ByteBuffer data = requestData(location, address, length);
		receivedData.flushingDataFromRegion(location, data);
	}

	// Found in SpiNNFrontEndCommon/spinn_front_end_common/interface/
	// buffer_management/recording_utilities.py
	private int getLastSequenceNumber(Placement placement,
			int recordingDataAddress) throws IOException, ProcessException {
		int addr = recordingDataAddress + LAST_SEQUENCE_NUMBER_OFFSET;
		return requestData(placement, addr, WORD_SIZE).getInt();
	}

	// Found in SpiNNFrontEndCommon/spinn_front_end_common/interface/
	// buffer_management/recording_utilities.py
	/**
	 * Get a pointer to a recording region.
	 *
	 * @param placement
	 *            The placement from which to read the pointer
	 * @param recording_data_address
	 *            The address of the recording data from which to read the
	 *            pointer
	 * @param region
	 *            The index of the region to get the pointer of
	 * @return The index of the region to get the pointer of.
	 * @throws IOException
	 *             if communications fail
	 */
	private int getRegionPointer(Placement placement, int recordingDataAddress,
			int region) throws IOException, ProcessException {
		int addr = recordingDataAddress + FIRST_REGION_ADDRESS_OFFSET
				+ (region * WORD_SIZE);
		return requestData(placement, addr, WORD_SIZE).getInt();
	}

	private ChannelBufferState generateEndBufferingStateFromMachine(
			Placement placement, int stateRegionBaseAddress)
			throws IOException, ProcessException {
		// retrieve channel state memory area
		ByteBuffer channelStateData = requestData(placement,
				stateRegionBaseAddress, ChannelBufferState.STATE_SIZE);
		return new ChannelBufferState(channelStateData);
	}

	/**
	 * Read memory from SDRAM on SpiNNaker.
	 *
	 * @param placement
	 *            The coords where data is to be extracted from.
	 * @param address
	 *            The memory address to start at
	 * @param length
	 *            The number of bytes to extract
	 * @return data as a byte array
	 * @throws IOException
	 *             if communications fail
	 */
	private ByteBuffer requestData(HasCoreLocation location, int address,
			int length) throws IOException, ProcessException {
		return txrx.readMemory(location.getScampCore(), address, length);
	}
}
