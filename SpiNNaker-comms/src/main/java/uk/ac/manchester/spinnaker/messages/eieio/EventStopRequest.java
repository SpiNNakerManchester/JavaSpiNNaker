package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.EVENT_STOP;

/**
 * Packet used for the buffering input technique which causes the parser of the
 * input packet to terminate its execution.
 */
public class EventStopRequest extends EIEIOCommandMessage {
	public EventStopRequest() {
		super(EVENT_STOP);
	}
}
