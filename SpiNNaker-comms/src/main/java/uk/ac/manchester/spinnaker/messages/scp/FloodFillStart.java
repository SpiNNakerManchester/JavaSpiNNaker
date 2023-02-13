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
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE3;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.TOP_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.FloodFillConstants.ADD_ID;
import static uk.ac.manchester.spinnaker.messages.scp.FloodFillConstants.DELAY;
import static uk.ac.manchester.spinnaker.messages.scp.FloodFillConstants.FORWARD_LINKS;
import static uk.ac.manchester.spinnaker.messages.scp.FloodFillConstants.INIT_LEVEL;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_NNP;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to start a flood fill of data. There is no response payload.
 * <p>
 * Calls {@code nn_cmd_ffs()} in {@code scamp-nn.c} on all cores via
 * {@code ff_nn_send()} in {@code scamp-nn.c}.
 */
public final class FloodFillStart extends SCPRequest<EmptyResponse> {
	// See nn_rcv_pkt()
	private static final int NN_CMD_FFS = 6;

	private static final int NNP_FORWARD_RETRY =
			(ADD_ID << TOP_BIT) | (FORWARD_LINKS << BYTE1) | (DELAY << BYTE0);

	private static final int NO_CHIP = 0xFFFF;

	private static final int LOW_BITS_MASK = 0b00000011;

	private static final int HIGH_BITS_MASK = 0b11111100;

	/**
	 * Flood fill onto all chips.
	 *
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param numBlocks
	 *            The number of blocks of data that will be sent, between 0 and
	 *            255
	 */
	public FloodFillStart(byte nearestNeighbourID, int numBlocks) {
		super(BOOT_MONITOR_CORE, CMD_NNP, key(nearestNeighbourID, numBlocks),
				NO_CHIP, NNP_FORWARD_RETRY);
	}

	/**
	 * Flood fill on a specific chip.
	 *
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param numBlocks
	 *            The number of blocks of data that will be sent, between 0 and
	 *            255
	 * @param chip
	 *            The chip to load the data on to.
	 */
	public FloodFillStart(byte nearestNeighbourID, int numBlocks,
			HasChipLocation chip) {
		super(BOOT_MONITOR_CORE, CMD_NNP, key(nearestNeighbourID, numBlocks),
				data(chip), NNP_FORWARD_RETRY);
	}

	private static int key(byte nearestNeighbourID, int numBlocks) {
		if (numBlocks != toUnsignedInt((byte) numBlocks)) {
			throw new IllegalArgumentException(
					"number of blocks must be representable in 8 bits");
		}
		return (NN_CMD_FFS << BYTE3)
				| (toUnsignedInt(nearestNeighbourID) << BYTE2)
				| (numBlocks << BYTE1);
	}

	private static int data(HasChipLocation chip) {
		int mask = 1 << (((chip.getY() & LOW_BITS_MASK) << 2)
				+ (chip.getX() & LOW_BITS_MASK));
		int region = ((chip.getX() & HIGH_BITS_MASK) << BYTE1)
				+ (chip.getY() & HIGH_BITS_MASK);
		return (region << BYTE2) + (INIT_LEVEL << BYTE2) + (mask << BYTE0);
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("Flood Fill", CMD_NNP, buffer);
	}
}
