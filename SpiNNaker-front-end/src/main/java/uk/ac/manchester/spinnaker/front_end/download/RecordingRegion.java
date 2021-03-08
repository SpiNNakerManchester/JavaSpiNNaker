/*
 * Copyright (c) 2021 The University of Manchester
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

import static java.lang.Integer.toUnsignedLong;
import static java.lang.Long.toHexString;
import static java.util.Collections.unmodifiableList;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * A descriptor about a channel of recording data.
 * <p>
 * Must match {@code recording_region_t} in {@code recording.h} in
 * SpiNNFrontEndCommon.
 * <p>
 * Do not use as a hash key.
 */
public final class RecordingRegion {
	/**
	 * Size of the channel data on the machine.
	 * <p>
	 * 4 for {@code space}, 4 for {@code missing_flag + size}, 4 for
	 * {@code data}
	 */
	public static final int SIZE = 12;

	/**
	 * Mask to get flag to indicate missing data.
	 */
	private static final long MISSING_MASK = 0x80000000L;

	/**
	 * Mask to get size.
	 */
	private static final long SIZE_MASK = 0x7FFFFFFFL;

	/**
	 * The size of the space available. (32-bits; unsigned)
	 * <p>
	 * Not currently used significantly by the Java code
	 */
	public final long space;

	/**
	 * If there is any missing information. (1-bit)
	 */
	public final boolean missing;

	/**
	 * The size of the recording in bytes. (31-bits; unsigned)
	 */
	public final long size;

	/**
	 * The data memory address. (32-bits; unsigned)
	 */
	public final long data;

	private RecordingRegion(ByteBuffer buffer) {
		space = toUnsignedLong(buffer.getInt());
		long missingAndSize = toUnsignedLong(buffer.getInt());
		missing = (missingAndSize & MISSING_MASK) != 0;
		size = missingAndSize & SIZE_MASK;
		data = toUnsignedLong(buffer.getInt());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Recording Channel:{")
				.append("space:").append(space).append(",size:").append(size)
				.append(",data:0x").append(toHexString(data));
		if (missing) {
			sb.append(",missing");
		}
		return sb.append("}").toString();
	}

	/**
	 * Reads the recording regions from the machine.
	 *
	 * @param txrx
	 *            Transceiver to read with
	 * @param placement
	 *            The core to read the data from
	 * @return A list of recording region descriptors read from the machine, one
	 *         per channel that the recording subsystem there knows about.
	 * @throws IOException
	 *             If the reading goes wrong
	 * @throws ProcessException
	 *             If the data in the read goes wrong
	 */
	public static List<RecordingRegion> getRecordingRegionDescriptors(
			TransceiverInterface txrx, Placement placement)
			throws IOException, ProcessException {
		long recordingDataAddress = placement.getVertex().getBaseAddress();
		// Get the size of the list of recordings
		int nRegions = txrx.readMemory(placement.getScampCore(),
				recordingDataAddress, WORD_SIZE).getInt();

		// Read all the channels' metadata
		ByteBuffer channelData = txrx.readMemory(placement.getScampCore(),
				recordingDataAddress + WORD_SIZE, SIZE * nRegions);

		// Parse the data
		List<RecordingRegion> regions = new ArrayList<>(nRegions);
		for (int i = 0; i < nRegions; i++) {
			regions.add(new RecordingRegion(channelData));
		}
		return unmodifiableList(regions);
	}
}
