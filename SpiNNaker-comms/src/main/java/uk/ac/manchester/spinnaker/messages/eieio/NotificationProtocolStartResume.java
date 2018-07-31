package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.START_RESUME_NOTIFICATION;

/**
 * Packet which indicates that the toolchain has started or resumed.
 */
public class NotificationProtocolStartResume extends EIEIOCommandMessage {
	public NotificationProtocolStartResume() {
		super(START_RESUME_NOTIFICATION);
	}
}
