/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.Byte.toUnsignedInt;
import static java.nio.ByteBuffer.wrap;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE3;
import static uk.ac.manchester.spinnaker.messages.scp.FloodFillConstants.DATA_RESEND;
import static uk.ac.manchester.spinnaker.messages.scp.FloodFillConstants.DELAY;
import static uk.ac.manchester.spinnaker.messages.scp.FloodFillConstants.FORWARD_LINKS;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FFD;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 * An SCP request to flood fill some data. There is no response payload.
 * <p>
 * Handled by {@code cmd_ffd()} in {@code scamp-cmd.c} (which sends many
 * messages to actually move the data, handled by {@code nn_cmd_fbs()},
 * {@code nn_cmd_fbd()} and {@code nn_cmd_fbe()} on their receiving cores).
 *
 * @see FloodFillStart
 * @see FloodFillEnd
 */
public class FloodFillData extends SCPRequest<CheckOKResponse> {
	private static final int FFD_NNP_FORWARD_RETRY =
			(FORWARD_LINKS << BYTE3) | ((DELAY | DATA_RESEND) << BYTE2);

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
		super(BOOT_MONITOR_CORE, CMD_FFD, idFwdRty(nearestNeighbourID),
				keyBase(blockNumber, length), baseAddress.address,
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
		super(BOOT_MONITOR_CORE, CMD_FFD, idFwdRty(nearestNeighbourID),
				keyBase(blockNumber, data.remaining()), baseAddress.address,
				data);
	}

	private static int idFwdRty(byte nearestNeighbourID) {
		return FFD_NNP_FORWARD_RETRY | toUnsignedInt(nearestNeighbourID);
	}

	private static int keyBase(int blockNumber, int size) {
		// This is the base for NN keys used
		return (blockNumber << BYTE2) | ((size / WORD_SIZE - 1) << BYTE1);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Flood Fill", CMD_FFD, buffer);
	}
}
