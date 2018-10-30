package uk.ac.manchester.spinnaker.messages.boot;

import static uk.ac.manchester.spinnaker.messages.boot.BootOpCode.FLOOD_FILL_START;

/**
 * The message indicating the start of a flood fill for booting.
 *
 * @author Donal Fellows
 */
class StartOfBootMessages extends BootMessage {
	/**
	 * @param numPackets
	 *            The number of payload packets to be sent.
	 */
	StartOfBootMessages(int numPackets) {
		super(FLOOD_FILL_START, 0, 0, numPackets - 1);
	}
}
