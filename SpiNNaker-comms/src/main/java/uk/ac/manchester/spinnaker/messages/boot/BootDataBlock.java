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
final class BootDataBlock extends BootMessage {
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
