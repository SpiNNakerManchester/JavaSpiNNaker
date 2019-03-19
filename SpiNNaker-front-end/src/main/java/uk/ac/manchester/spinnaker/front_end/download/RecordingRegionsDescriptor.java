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

import static java.lang.Integer.toHexString;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * Describes locations in a recording region. Must match
 * {@code recording_data_t} in {@code recording.c} in SpiNNFrontEndCommon. If
 * the correct branch is merged there. Otherwise, it is defined by
 * pseudo-structure done with offsets into an array of integers. Yay.
 *
 * @author Donal Fellows
 */
final class RecordingRegionsDescriptor {
	/** Number of recording regions. */
	final int numRegions;
	/** Tag for sending buffered output. */
	final int tag;
	/** Destination for sending buffered output. */
	final int tagDestination;
	/** SDP port for receiving buffering messages. */
	final int sdpPort;
	/** Minimum size of data to start buffering out at. */
	final int bufferSizeBeforeRequest;
	/** Minimum interval between buffering out actions. */
	final int timeBetweenTriggers;
	/** Last sequence number used when buffering out. */
	final int lastSequenceNumber;
	/**
	 * Array of addresses of recording regions. This actually points to the
	 * channel's buffer state. Size, {@link #numRegions} entries.
	 */
	final int[] regionPointers;
	/**
	 * Array of sizes of recording regions. Size, {@link #numRegions} entries.
	 */
	final int[] regionSizes;
	private final ChipLocation chip;
	private final int addr;

	/**
	 * Get an instance of this descriptor from SpiNNaker.
	 *
	 * @param txtx
	 *            The transceiver to use for communications.
	 * @param chip
	 *            The chip to read from.
	 * @param address
	 *            Where on the chip to read the region data from.
	 * @throws IOException
	 *             If I/O fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	RecordingRegionsDescriptor(Transceiver txrx, HasChipLocation chip,
			int address) throws IOException, ProcessException {
		// Read the descriptor from SpiNNaker
		int nr = txrx.readMemory(chip, address, WORD_SIZE).getInt();
		RecordingRegionDataGatherer.log.info("{} recording regions at {} of {}",
				nr, address, chip);
		int size = WORD_SIZE * (REGION_POINTERS_START + 2 * nr);
		ByteBuffer buffer = txrx.readMemory(chip, address, size);
		this.chip = chip.asChipLocation();
		this.addr = address;

		// Unpack the contents of the descriptor
		numRegions = buffer.getInt();
		tag = buffer.getInt();
		tagDestination = buffer.getInt();
		sdpPort = buffer.getInt();
		bufferSizeBeforeRequest = buffer.getInt();
		timeBetweenTriggers = buffer.getInt();
		lastSequenceNumber = buffer.getInt();
		regionPointers = new int[numRegions];
		regionSizes = new int[numRegions];
		IntBuffer intBuffer = buffer.asIntBuffer();
		intBuffer.get(regionPointers);
		intBuffer.get(regionSizes);
	}

	private static final int REGION_POINTERS_START = 7;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("RecordingRegions:").append(chip)
				.append("@0x").append(toHexString(addr)).append("{");
		sb.append("#").append(numRegions);
		sb.append(",tag=").append(tag);
		sb.append(",dst=0x").append(toHexString(tagDestination));
		sb.append(",sdp=0x").append(toHexString(sdpPort));
		sb.append(",min=0x").append(toHexString(bufferSizeBeforeRequest));
		sb.append(",trg=").append(timeBetweenTriggers);
		sb.append(",seq=").append(lastSequenceNumber);
		sb.append("}:[0x");
		String sep = "";
		for (int i = 0; i < numRegions; i++) {
			sb.append(sep).append(toHexString(regionPointers[i])).append(":0x")
					.append(toHexString(regionSizes[i]));
			sep = ", 0x";
		}
		return sb.append("]").toString();
	}
}
