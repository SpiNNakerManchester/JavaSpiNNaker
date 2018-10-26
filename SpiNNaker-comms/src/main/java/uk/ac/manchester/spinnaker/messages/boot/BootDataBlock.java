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
	private static final int BOOT_DATA_OPERAND_1 =
			(BOOT_MESSAGE_DATA_WORDS - 1) << 8;
	private static final int BYTE_MASK = 0xFF;

	BootDataBlock(int blockID, ByteBuffer buffer) {
		super(FLOOD_FILL_BLOCK, BOOT_DATA_OPERAND_1 | blockID, 0, 0, buffer);

		// Sanity checks
		assert (blockID
				& BYTE_MASK) == blockID : "block ID must fit in byte range";
		assert buffer.hasRemaining() : "buffer must have space left";
	}
}
