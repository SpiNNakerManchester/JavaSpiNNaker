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
 * Describes a recording region. Must match {@code recording_channel_t} in
 * {@code recording.c} in SpiNNFrontEndCommon.
 *
 * @author Donal Fellows
 */
class ChannelBufferState {
	/** Size of this structure in bytes. */
	static final int SIZE = 24;
	/** The start buffering area memory address. (32 bits; unsigned) */
	long start;
	/** The address where data was last written. (32 bits; unsigned) */
	long currentWrite;
	/** The address where the DMA write got up to. (32 bits; unsigned) */
	long dmaCurrentWrite;
	/** The address where data was last read. (32 bits; unsigned) */
	long currentRead;
	/** The address of first byte after the buffer. (32 bits; unsigned) */
	long end;
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
		start = Integer.toUnsignedLong(buffer.getInt());
		currentWrite = Integer.toUnsignedLong(buffer.getInt());
		dmaCurrentWrite = Integer.toUnsignedLong(buffer.getInt());
		currentRead = Integer.toUnsignedLong(buffer.getInt());
		end = Integer.toUnsignedLong(buffer.getInt());
		regionId = buffer.get();
		missingInfo = (buffer.get() != 0);
		lastBufferOperationWasWrite = (buffer.get() != 0);
		buffer.get(); // padding
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Channel #").append(regionId)
				.append(":{");
		sb.append("start:0x").append(Long.toHexString(start))
				.append(",currentWrite:0x")
				.append(Long.toHexString(currentWrite))
				.append(",dmaCurrentWrite:0x")
				.append(Long.toHexString(dmaCurrentWrite))
				.append(",currentRead:0x")
				.append(Long.toHexString(currentRead)).append(",end:0x")
				.append(Long.toHexString(end));
		if (missingInfo) {
			sb.append(",missingInfo");
		}
		if (lastBufferOperationWasWrite) {
			sb.append(",lastWasWrite");
		}
		return sb.append("}").toString();
	}
}
