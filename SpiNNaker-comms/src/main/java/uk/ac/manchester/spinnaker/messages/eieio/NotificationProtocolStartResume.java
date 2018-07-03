package uk.ac.manchester.spinnaker.messages.eieio;

/**
 * Packet which indicates that the toolchain has started or resumed.
 */
public class NotificationProtocolStartResume extends EIEIOCommandMessage {
	public NotificationProtocolStartResume() {
		super(EIEIOCommandID.START_RESUME_NOTIFICATION);
	}
}
