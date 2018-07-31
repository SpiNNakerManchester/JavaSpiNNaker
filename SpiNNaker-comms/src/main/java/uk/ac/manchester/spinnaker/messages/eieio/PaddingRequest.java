package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.EVENT_PADDING;

/**
 * Packet used to pad space in the buffering area, if needed.
 */
public class PaddingRequest extends EIEIOCommandMessage {
	public PaddingRequest() {
		super(EVENT_PADDING);
	}
}
