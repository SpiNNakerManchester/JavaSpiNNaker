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
package uk.ac.manchester.spinnaker.messages.boot;

import static uk.ac.manchester.spinnaker.messages.Constants.BYTE_MASK;
import static uk.ac.manchester.spinnaker.messages.Constants.NBBY;
import static uk.ac.manchester.spinnaker.messages.boot.BootMessages.BOOT_MESSAGE_DATA_WORDS;
import static uk.ac.manchester.spinnaker.messages.boot.BootOpCode.FLOOD_FILL_BLOCK;

import java.nio.ByteBuffer;

/**
 * The message giving a block of data for booting.
 *
 * @author Donal Fellows
 */
class BootDataBlock extends BootMessage {
	private static final int BOOT_DATA_OPERAND_1 =
			(BOOT_MESSAGE_DATA_WORDS - 1) << NBBY;

	private static boolean inByteRange(int value) {
		return (value & BYTE_MASK) == value;
	}

	BootDataBlock(int blockID, ByteBuffer buffer) {
		super(FLOOD_FILL_BLOCK, BOOT_DATA_OPERAND_1 | blockID, 0, 0, buffer);

		// Sanity checks
		assert inByteRange(blockID) : "block ID must fit in byte range";
		assert buffer.hasRemaining() : "buffer must have space left";
	}
}
