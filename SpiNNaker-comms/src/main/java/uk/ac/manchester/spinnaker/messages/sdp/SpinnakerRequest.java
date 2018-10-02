package uk.ac.manchester.spinnaker.messages.sdp;

import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.transceiver.Utils.newMessageBuffer;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/**
 * Base class for sendable SDP-based messages. SDP-based messages are those
 * messages that have an SDP header.
 *
 * @author Donal Fellows
 */
public abstract class SpinnakerRequest implements SerializableMessage {
	private static final ChipLocation ONE_WAY_SOURCE = new ChipLocation(0, 0);
	private static final int SDP_SOURCE_PORT = 7;
	private static final int SDP_SOURCE_CPU = 31;
	private static final byte SDP_TAG = (byte) 0xFF;
	/** The SDP header of the message. */
	public final SDPHeader sdpHeader;

	protected SpinnakerRequest(SDPHeader sdpHeader) {
		this.sdpHeader = sdpHeader;
	}

	/**
	 * Prepares this message to be actually sent. This involves setting the tag
	 * and source of the header to special marker values.
	 *
	 * @param chip
	 *            The notional originating chip location.
	 */
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
	 * Get a buffer holding the actual bytes of the message, ready to send.
	 *
	 * @param originatingChip
	 *            Where the message notionally originates from.
	 * @return The byte buffer.
	 */
	public final ByteBuffer getMessageData(HasChipLocation originatingChip) {
		ByteBuffer buffer = newMessageBuffer();
		if (sdpHeader.getFlags() == REPLY_EXPECTED) {
			updateSDPHeaderForUDPSend(originatingChip);
		} else {
			updateSDPHeaderForUDPSend(ONE_WAY_SOURCE);
		}
		// First two bytes must be zero for SCP send
		buffer.putShort((short) 0);
		addToBuffer(buffer);
		buffer.flip();
		return buffer;
	}

	/**
	 * A special source location that is the source for an SDP packet. <b>Note
	 * that this is not a real core location!</b>
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

		@Override
		public CoreLocation asCoreLocation() {
			throw new UnsupportedOperationException();
		}
	}
}
