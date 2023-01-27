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

/**
 * An SCP request to start a flood fill of data. There is no response payload.
 * <p>
 * Calls {@code nn_cmd_ffs()} in {@code scamp-nn.c} on all cores via
 * {@code ff_nn_send()} in {@code scamp-nn.c}.
 */
public final class FloodFillStart extends SCPRequest<CheckOKResponse> {
	// See nn_rcv_pkt()
	private static final int NN_CMD_FFS = 6;

	private static final int NNP_FORWARD_RETRY = (ADD_ID << TOP_BIT)
			| (FORWARD_LINKS << BYTE1) | (DELAY << BYTE0);

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
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Flood Fill", CMD_NNP, buffer);
	}
}
