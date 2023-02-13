/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.sdp;

import static java.util.Objects.requireNonNull;
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

	/**
	 * @param sdpHeader The SDP header of the message.
	 */
	protected SpinnakerRequest(SDPHeader sdpHeader) {
		this.sdpHeader = requireNonNull(sdpHeader);
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
	 * @throws IllegalStateException
	 *             If a message is prepared for sending a second time.
	 */
	public final ByteBuffer getMessageData(HasChipLocation originatingChip) {
		if (sdpHeader.getSource() != null) {
			throw new IllegalStateException(
					"can only prepare request for sending once");
		}

		// Set ready for sending
		sdpHeader.setTag(SDP_TAG);
		sdpHeader.setSourcePort(SDP_SOURCE_PORT);
		sdpHeader.setSource(new SDPSource(originatingChip));

		// Serialize
		var buffer = newMessageBuffer();
		// First two bytes must be zero for SDP or SCP send
		buffer.putShort((short) 0);
		addToBuffer(buffer);
		return buffer.flip();
	}

	/**
	 * A special source location that is the source for an SDP packet. <b>Note
	 * that this is not a real core location!</b>
	 *
	 * @author Donal Fellows
	 */
	private static class SDPSource implements HasCoreLocation {
		private final int x;

		private final int y;

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
