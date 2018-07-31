package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.STOP_SENDING_REQUESTS;

/**
 * Packet used in the context of buffering input for the host computer to signal
 * to the SpiNNaker system that to stop sending "SpinnakerRequestBuffers"
 * packet.
 */
public class StopRequests extends EIEIOCommandMessage {
	public StopRequests() {
		super(STOP_SENDING_REQUESTS);
	}
}
