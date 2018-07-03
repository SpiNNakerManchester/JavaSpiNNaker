package uk.ac.manchester.spinnaker.messages.eieio;

/**
 * Packet used in the context of buffering input for the host computer to signal
 * to the SpiNNaker system that to stop sending "SpinnakerRequestBuffers"
 * packet.
 */
public class StopRequests extends EIEIOCommandMessage {
	public StopRequests() {
		super(EIEIOCommandID.STOP_SENDING_REQUESTS);
	}
}
