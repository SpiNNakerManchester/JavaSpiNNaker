package uk.ac.manchester.spinnaker.messages.sdp;

import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.transceiver.Utils.newMessageBuffer;

import java.nio.ByteBuffer;

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
	private static final int SDP_SOURCE_PORT = 7;
	private static final int SDP_SOURCE_CPU = 31;
	private static final byte SDP_TAG = (byte) 0xFF;
	/** The SDP header of the message. */
	public final SDPHeader sdpHeader;

	protected SpinnakerRequest(SDPHeader sdpHeader) {
		this.sdpHeader = sdpHeader;
	}

	/**
	 * Get a buffer holding the actual bytes of the message, ready to send. This
	 * also prepares this message to be actually sent, which involves setting
	 * the tag and source of the header to special marker values. <em>This can
	 * only be called once per connection!</em>
	 *
	 * @param originatingChip
	 *            Where the message notionally originates from.
	 * @return The byte buffer.
	 */
	public final ByteBuffer getMessageData(HasChipLocation originatingChip) {
		if (sdpHeader.getSource() != null) {
			throw new IllegalStateException(
					"can only prepare request for sending once");
		}

		// Set ready for sending
		sdpHeader.setTag(SDP_TAG);
		sdpHeader.setSourcePort(SDP_SOURCE_PORT);
		if (sdpHeader.getFlags() == REPLY_EXPECTED) {
			sdpHeader.setSource(new SDPSource(originatingChip));
		} else {
			sdpHeader.setSource(new SDPSource());
		}

		// Serialize
		ByteBuffer buffer = newMessageBuffer();
		// First two bytes must be zero for SDP or SCP send
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
		private final int x, y;

		/** Source for one-way sending. */
		SDPSource() {
			x = 0;
			y = 0;
		}

		/** Source for nominated location sending, needed for replies. */
		SDPSource(HasChipLocation chip) {
			this.x = chip.getX();
			this.y = chip.getY();
		}

		@Override
		public int getX() {
			return x;
		}

		@Override
		public int getY() {
			return y;
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
