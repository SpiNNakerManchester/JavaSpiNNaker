package uk.ac.manchester.spinnaker.messages.eieio;

/**
 * Packet used for the buffering input technique which causes the parser of the
 * input packet to terminate its execution.
 */
public class EventStopRequest extends EIEIOCommandMessage {
	public EventStopRequest() {
		super(EIEIOCommandID.EVENT_STOP);
	}
}
