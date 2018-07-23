package uk.ac.manchester.spinnaker.messages.sdp;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.SerializableMessage;

public abstract class SpinnakerRequest implements SerializableMessage {
	private static final int SDP_SOURCE_PORT = 7;
	private static final int SDP_SOURCE_CPU = 31;
	private static final byte SDP_TAG = (byte) 0xFF;
	/** The SDP header of the message */
	public final SDPHeader sdpHeader;

	protected SpinnakerRequest(SDPHeader sdpHeader) {
		this.sdpHeader = sdpHeader;
	}

	public final void updateSDPHeaderForUDPSend(HasChipLocation chip) {
		if (sdpHeader.getSource() != null) {
			throw new IllegalStateException(
					"can only prepare request for sending once");
		}
		sdpHeader.setTag(SDP_TAG);
		sdpHeader.setSourcePort(SDP_SOURCE_PORT);
		sdpHeader.setSource(
				new CoreLocation(chip.getX(), chip.getY(), SDP_SOURCE_CPU));
	}
}
