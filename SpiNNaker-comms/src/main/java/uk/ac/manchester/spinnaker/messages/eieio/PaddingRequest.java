package uk.ac.manchester.spinnaker.messages.eieio;

/**
 * Packet used to pad space in the buffering area, if needed.
 */
public class PaddingRequest extends EIEIOCommandMessage {
	public PaddingRequest() {
		super(EIEIOCommandID.EVENT_PADDING);
	}
}
