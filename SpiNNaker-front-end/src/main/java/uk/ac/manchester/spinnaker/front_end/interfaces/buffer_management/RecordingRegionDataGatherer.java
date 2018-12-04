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
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.storage.Storage;
import uk.ac.manchester.spinnaker.storage.Storage.Region;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * A data gatherer that pulls the data from a recording region.
 *
 * @author Donal Fellows
 */
public class RecordingRegionDataGatherer extends DataGatherer {
	private final Transceiver txrx;
	private final Storage database;

	/**
	 * Create a data gatherer.
	 *
	 * @param transceiver
	 *            How to talk to the machine.
	 * @param database
	 *            Where to put the retrieved data.
	 * @throws ProcessException
	 *             If we can't discover the machine details due to SpiNNaker
	 *             rejecting messages
	 * @throws IOException
	 *             If we can't discover the machine details due to I/O problems
	 */
	public RecordingRegionDataGatherer(Transceiver transceiver,
			Storage database) throws IOException, ProcessException {
		super(transceiver);
		this.txrx = transceiver;
		this.database = database;
	}

	/**
	 * Describes locations in a recording region. Must match
	 * {@code recording_data_t} in {@code recording.c} in SpiNNFrontEndCommon.
	 * If the correct branch is merged there. Otherwise, it is defined by
	 * pseudo-structure done with offsets into an array of integers. Yay.
	 *
	 * @author Donal Fellows
	 */
	static final class RecordingRegionsDescriptor {
		/** Number of recording regions. */
		int numRegions;
		/** Tag for sending buffered output. */
		int tag;
		/** Destination for sending buffered output. */
		int tagDestination;
		/** SDP port for receiving buffering messages. */
		int sdpPort;
		/** Minimum size of data to start buffering out at. */
		int bufferSizeBeforeRequest;
		/** Minimum interval between buffering out actions. */
		int timeBetweenTriggers;
		/** Last sequence number used when buffering out. */
		int lastSequenceNumber;
		/**
		 * Array of addresses of recording regions. This actually points to the
		 * channel's buffer state. Size, {@link #numRegions} entries.
		 */
		int[] regionPointers;
		/**
		 * Array of sizes of recording regions. Size, {@link #numRegions}
		 * entries.
		 */
		int[] regionSizes;

		private RecordingRegionsDescriptor(int numRegions, ByteBuffer buffer) {
			this.numRegions = numRegions;
			tag = buffer.getInt();
			tagDestination = buffer.getInt();
			sdpPort = buffer.getInt();
			bufferSizeBeforeRequest = buffer.getInt();
			lastSequenceNumber = buffer.getInt();
			regionPointers = new int[numRegions];
			buffer.asIntBuffer().get(regionPointers);
			regionSizes = new int[numRegions];
			buffer.asIntBuffer().get(regionSizes);
		}

		private static final int REGION_POINTERS_START = 7;

		/**
		 * Get an instance of this descriptor from SpiNNaker.
		 *
		 * @param txrx
		 *            The transceiver to use to do the reading.
		 * @param chip
		 *            The chip to read from.
		 * @param address
		 *            Where on the chip to read the region data from.
		 * @return The descriptor. Not validated.
		 * @throws IOException
		 *             If I/O fails.
		 * @throws ProcessException
		 *             If SpiNNaker rejects a message.
		 */
		static RecordingRegionsDescriptor get(Transceiver txrx,
				HasChipLocation chip, int address)
				throws IOException, ProcessException {
			int numRegions = txrx.readMemory(chip, address, WORD_SIZE).getInt();
			int size = WORD_SIZE * (REGION_POINTERS_START + 2 * numRegions);
			return new RecordingRegionsDescriptor(numRegions, txrx
					.readMemory(chip, address + WORD_SIZE, size - WORD_SIZE));
		}
	}

	/**
	 * Describes a recording region. Must match {@code recording_channel_t} in
	 * {@code recording.c} in SpiNNFrontEndCommon.
	 *
	 * @author Donal Fellows
	 */
	@SuppressWarnings("unused")
	private static class ChannelBufferState {
		/** Size of this structure in bytes. */
		static final int SIZE = 24;
		/** The start buffering area memory address. (32 bits) */
		int start;
		/** The address where data was last written. (32 bits) */
		int currentWrite;
		/** The address where the DMA write got up to. (32 bits) */
		int dmaCurrentWrite;
		/** The address where data was last read. (32 bits) */
		int currentRead;
		/** The address of first byte after the buffer. (32 bits) */
		int end;
		/** The ID of the region. (8 bits) */
		byte regionId;
		/** True if the region overflowed during the simulation. (8 bits) */
		boolean missingInfo;
		/** Last operation performed on the buffer. Read or write (8 bits) */
		boolean lastBufferOperationWasWrite;

		/**
		 * Deserialize an instance of this class.
		 *
		 * @param buffer
		 *            Little-endian buffer to read from. Must be (at least) 24
		 *            bytes long.
		 */
		ChannelBufferState(ByteBuffer buffer) {
			start = buffer.getInt();
			currentWrite = buffer.getInt();
			dmaCurrentWrite = buffer.getInt();
			currentRead = buffer.getInt();
			end = buffer.getInt();
			regionId = buffer.get();
			missingInfo = (buffer.get() != 0);
			lastBufferOperationWasWrite = (buffer.get() != 0);
			buffer.get(); // padding
		}
	}

	private Map<ChipLocation, RecordingRegionsDescriptor> descriptors =
			new HashMap<>();

	private synchronized RecordingRegionsDescriptor getDescriptor(
			ChipLocation chip, int baseAddress)
			throws IOException, ProcessException {
		RecordingRegionsDescriptor rrd = descriptors.get(chip);
		if (rrd == null) {
			rrd = RecordingRegionsDescriptor.get(txrx, chip, baseAddress);
			descriptors.put(chip, rrd);
		}
		return rrd;
	}

	private ChannelBufferState getState(Placement placement,
			int recordingRegionIndex) throws IOException, ProcessException {
		ChipLocation chip = placement.asChipLocation();
		RecordingRegionsDescriptor descriptor = getDescriptor(chip,
				placement.getVertex().recordingRegionBaseAddress);
		return new ChannelBufferState(txrx.readMemory(chip,
				descriptor.regionPointers[recordingRegionIndex],
				ChannelBufferState.SIZE));
	}

	private static class RecordingRegion extends Region {
		RecordingRegion(HasCoreLocation core, int regionIndex,
				ChannelBufferState state) {
			super(core, regionIndex, state.start,
					state.currentWrite - state.start);
		}
	}

	@Override
	protected Region getRegion(Placement placement, int recordingRegionIndex)
			throws IOException, ProcessException, StorageException {
		ChannelBufferState state = getState(placement, recordingRegionIndex);
		return new RecordingRegion(placement, recordingRegionIndex, state);
	}

	@Override
	protected void storeData(Region r, ByteBuffer data)
			throws StorageException {
		database.appendRecordingContents(r, data);
	}
}
