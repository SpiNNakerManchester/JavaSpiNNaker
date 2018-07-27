package uk.ac.manchester.spinnaker.messages.sdp;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
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
		sdpHeader.setSource(new SDPSource(chip));
	}

	/**
	 * A special source location that is the source for an SDP packet. <b>Note
	 * that this is not a real core location!</b> It will cause failures if
	 * converted into a {@link CoreLocation}!.
	 *
	 * @author Donal Fellows
	 */
	private static class SDPSource implements HasCoreLocation {
		private final HasChipLocation chip;

		SDPSource(HasChipLocation chip) {
			this.chip = chip;
		}

		@Override
		public int getX() {
			return chip.getX();
		}

		@Override
		public int getY() {
			return chip.getY();
		}

		@Override
		public int getP() {
			return SDP_SOURCE_CPU;
		}
	}
}
