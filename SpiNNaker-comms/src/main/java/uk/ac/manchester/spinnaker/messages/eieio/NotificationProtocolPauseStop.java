package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.STOP_PAUSE_NOTIFICATION;

/**
 * Packet which indicates that the toolchain has paused or stopped.
 */
public class NotificationProtocolPauseStop extends EIEIOCommandMessage {
	public NotificationProtocolPauseStop() {
		super(STOP_PAUSE_NOTIFICATION);
	}
}
