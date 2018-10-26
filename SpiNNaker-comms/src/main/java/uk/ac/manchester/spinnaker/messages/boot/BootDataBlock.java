package uk.ac.manchester.spinnaker.messages.boot;

import static uk.ac.manchester.spinnaker.messages.boot.BootMessages.BOOT_MESSAGE_DATA_WORDS;
import static uk.ac.manchester.spinnaker.messages.boot.BootOpCode.FLOOD_FILL_BLOCK;

import java.nio.ByteBuffer;

/**
 * The message giving a block of data for booting.
 *
 * @author Donal Fellows
 */
class BootDataBlock extends BootMessage {
	private static final int BYTE_MASK = 0xFF;
	private static final int BYTE_SIZE = 8;
	private static final int BOOT_DATA_OPERAND_1 =
			(BOOT_MESSAGE_DATA_WORDS - 1) << BYTE_SIZE;

	private static final boolean inByteRange(int value) {
		return (value & BYTE_MASK) == value;
	}

	BootDataBlock(int blockID, ByteBuffer buffer) {
		super(FLOOD_FILL_BLOCK, BOOT_DATA_OPERAND_1 | blockID, 0, 0, buffer);

		// Sanity checks
		assert inByteRange(blockID) : "block ID must fit in byte range";
		assert buffer.hasRemaining() : "buffer must have space left";
	}
}
