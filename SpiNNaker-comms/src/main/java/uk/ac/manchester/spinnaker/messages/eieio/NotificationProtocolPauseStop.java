package uk.ac.manchester.spinnaker.messages.eieio;

/**
 * Packet which indicates that the toolchain has paused or stopped.
 */
public class NotificationProtocolPauseStop extends EIEIOCommandMessage {
	public NotificationProtocolPauseStop() {
		super(EIEIOCommandID.STOP_PAUSE_NOTIFICATION);
	}
}
