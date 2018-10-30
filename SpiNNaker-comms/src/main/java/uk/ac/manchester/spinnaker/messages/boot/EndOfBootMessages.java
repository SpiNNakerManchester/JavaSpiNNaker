package uk.ac.manchester.spinnaker.messages.boot;

import static uk.ac.manchester.spinnaker.messages.boot.BootOpCode.FLOOD_FILL_CONTROL;

/**
 * The message indicating the end of a flood fill for booting.
 *
 * @author Donal Fellows
 */
class EndOfBootMessages extends BootMessage {
	EndOfBootMessages() {
		super(FLOOD_FILL_CONTROL, 1, 0, 0);
	}
}
