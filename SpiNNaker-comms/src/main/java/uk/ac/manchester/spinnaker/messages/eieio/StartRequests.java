package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.START_SENDING_REQUESTS;

/**
 * Packet used in the context of buffering input for the host computer to signal
 * to the SpiNNaker system that, if needed, it is possible to send more
 * "SpinnakerRequestBuffers" packet.
 */
public class StartRequests extends EIEIOCommandMessage {
	public StartRequests() {
		super(START_SENDING_REQUESTS);
	}
}
