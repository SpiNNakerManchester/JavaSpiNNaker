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
package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.Byte.toUnsignedInt;
import static java.nio.ByteBuffer.wrap;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE3;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FFD;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/** A request to start a flood fill of data. */
public class FloodFillData extends SCPRequest<CheckOKResponse> {
	private static final int MAGIC1 = 0x3f;

	private static final int MAGIC2 = 0x1A;

	private static final int NNP_FORWARD_RETRY =
			(MAGIC1 << BYTE3) | (MAGIC2 << BYTE2);

	/**
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param blockNumber
	 *            Which block this block is, between 0 and 255
	 * @param baseAddress
	 *            The base address where the data is to be loaded
	 * @param data
	 *            The data to load, between 4 and 256 bytes and the size must be
	 *            divisible by 4.
	 */
	public FloodFillData(byte nearestNeighbourID, int blockNumber,
			MemoryLocation baseAddress, byte[] data) {
		this(nearestNeighbourID, blockNumber, baseAddress, data, 0,
				data.length);
	}

	/**
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param blockNumber
	 *            Which block this block is, between 0 and 255
	 * @param baseAddress
	 *            The base address where the data is to be loaded
	 * @param data
	 *            The data to load, between 4 and 256 bytes and the size must be
	 *            divisible by 4
	 * @param offset
	 *            Where in the array the data starts at.
	 * @param length
	 *            The length of the data; must be divisible by 4.
	 */
	public FloodFillData(byte nearestNeighbourID, int blockNumber,
			MemoryLocation baseAddress, byte[] data, int offset, int length) {
		super(BOOT_MONITOR_CORE, CMD_FFD, argument1(nearestNeighbourID),
				argument2(blockNumber, length), baseAddress.address(),
				wrap(data, offset, length));
	}

	/**
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param blockNumber
	 *            Which block this block is, between 0 and 255
	 * @param baseAddress
	 *            The base address where the data is to be loaded
	 * @param data
	 *            The data to load, starting at the <i>position</i> and going to
	 *            the <i>limit</i>. Must be between 4 and 256 bytes and the size
	 *            must be divisible by 4. The position and limit of the buffer
	 *            will not be updated by this constructor.
	 */
	public FloodFillData(byte nearestNeighbourID, int blockNumber,
			MemoryLocation baseAddress, ByteBuffer data) {
		super(BOOT_MONITOR_CORE, CMD_FFD, argument1(nearestNeighbourID),
				argument2(blockNumber, data.remaining()), baseAddress.address(),
				data);
	}

	private static int argument1(byte nearestNeighbourID) {
		return NNP_FORWARD_RETRY | toUnsignedInt(nearestNeighbourID);
	}

	private static int argument2(int blockNumber, int size) {
		return (blockNumber << BYTE2) | ((size / WORD_SIZE - 1) << BYTE1);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Flood Fill", CMD_FFD, buffer);
	}
}
