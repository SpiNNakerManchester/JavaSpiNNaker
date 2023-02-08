/*
 * Copyright (c) 2021 The University of Manchester
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

import static java.lang.Integer.toUnsignedLong;
import static java.util.Collections.unmodifiableList;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
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
	public final MemoryLocation data;

	private RecordingRegion(ByteBuffer buffer) {
		space = toUnsignedLong(buffer.getInt());
		long missingAndSize = toUnsignedLong(buffer.getInt());
		missing = (missingAndSize & MISSING_MASK) != 0;
		size = missingAndSize & SIZE_MASK;
		data = new MemoryLocation(buffer.getInt());
	}

	@Override
	public String toString() {
		var sb = new StringBuilder("Recording Channel:{")
				.append("space:").append(space).append(",size:").append(size)
				.append(",data:").append(data);
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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public static List<RecordingRegion> getRecordingRegionDescriptors(
			TransceiverInterface txrx, Placement placement)
			throws IOException, ProcessException, InterruptedException {
		var recordingDataAddress = placement.getVertex().getBase();
		// Get the size of the list of recordings
		int nRegions = txrx.readMemory(placement.getScampCore(),
				recordingDataAddress, WORD_SIZE).getInt();

		// Read all the channels' metadata
		var channelData = txrx.readMemory(placement.getScampCore(),
				recordingDataAddress.add(WORD_SIZE), SIZE * nRegions);

		// Parse the data
		var regions = new ArrayList<RecordingRegion>(nRegions);
		for (int i = 0; i < nRegions; i++) {
			regions.add(new RecordingRegion(channelData));
		}
		return unmodifiableList(regions);
	}
}
