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

import java.nio.ByteBuffer;

/**
 * Stores information related to a single recording region channel output
 * buffering state, as it is retrieved at the end of a simulation on the
 * SpiNNaker system. Must match {@code recording_channel_t} in
 * {@code recording.c} in SpiNNFrontEndCommon.
 *
 * @see <a href=
 * "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/storage_objects/channel_buffer_state.py">
 *      Python Version</a>
 * @author Donal Fellows
 * @author Christian-B
 */
class ChannelBufferState {
	/**
	 * Size of the state.
	 * <p>
	 * 4 for {@code startAddress}, 4 for {@code currentWrite}, 4 for
	 * {@code currentDmaWrite}, 4 for {@code currentRead}, 4 for
	 * {@code endAddress}, 1 for {@code regionId}, 1 for {@code missingInfo}, 1
	 * for {@code lastBufferOperation},
	 */
	public static final int STATE_SIZE = 24;

	/** The start buffering area memory address. (32 bits; unsigned) */
	public final long start;
	/** The address where data was last written. (32 bits; unsigned) */
	public final long currentWrite;
	/** The address where the DMA write got up to. (32 bits; unsigned) */
	public final long dmaCurrentWrite;
	/** The address where data was last read. (32 bits; unsigned) */
	public final long currentRead;
	/** The address of first byte after the buffer. (32 bits; unsigned) */
	public final long end;
	/** The ID of the region. (8 bits) */
	public final byte regionId;
	/** True if the region overflowed during the simulation. (8 bits) */
	public final boolean missingInfo;
	/** Last operation performed on the buffer. Read or write (8 bits) */
	public final boolean lastOpWasWrite;

	/**
	 * Deserialize an instance of this class.
	 *
	 * @param buffer
	 *            Little-endian buffer to read from. Must be (at least) 24 bytes
	 *            long.
	 */
	ChannelBufferState(ByteBuffer buffer) {
		// _CHANNEL_BUFFER_PATTERN = struct.Struct("<IIIIIBBBx")
		start = Integer.toUnsignedLong(buffer.getInt());
		currentWrite = Integer.toUnsignedLong(buffer.getInt());
		dmaCurrentWrite = Integer.toUnsignedLong(buffer.getInt());
		currentRead = Integer.toUnsignedLong(buffer.getInt());
		end = Integer.toUnsignedLong(buffer.getInt());
		regionId = buffer.get();
		missingInfo = (buffer.get() != 0);
		lastOpWasWrite = (buffer.get() != 0);
		buffer.get(); // padding
	}

	@Override
	public String toString() {
		StringBuilder sb =
				new StringBuilder("Channel #").append(regionId).append(":{");
		sb.append("start:0x").append(Long.toHexString(start))
				.append(",currentWrite:0x")
				.append(Long.toHexString(currentWrite))
				.append(",dmaCurrentWrite:0x")
				.append(Long.toHexString(dmaCurrentWrite))
				.append(",currentRead:0x").append(Long.toHexString(currentRead))
				.append(",end:0x").append(Long.toHexString(end));
		if (missingInfo) {
			sb.append(",missingInfo");
		}
		if (lastOpWasWrite) {
			sb.append(",lastWasWrite");
		}
		return sb.append("}").toString();
	}
}
