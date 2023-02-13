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

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.nio.ByteBuffer;

/** Wraps up an SDP message with a header and optional data. */
public class SDPMessage extends SpinnakerRequest {
	private final ByteBuffer databuf;

	/**
	 * Create an SDP message with no payload.
	 *
	 * @param header
	 *            the message header.
	 */
	public SDPMessage(SDPHeader header) {
		this(header, null, 0, 0);
	}

	/**
	 * Create an SDP message with a payload.
	 *
	 * @param header
	 *            the message header.
	 * @param data
	 *            the message payload.
	 */
	public SDPMessage(SDPHeader header, byte[] data) {
		this(header, data, 0, data == null ? 0 : data.length);
	}

	/**
	 * Create an SDP message with a payload.
	 *
	 * @param header
	 *            the message header.
	 * @param data
	 *            the message payload.
	 * @param offset
	 *            where in the array the payload starts.
	 */
	public SDPMessage(SDPHeader header, byte[] data, int offset) {
		this(header, data, offset, data == null ? 0 : data.length - offset);
	}

	/**
	 * Create an SDP message with a payload.
	 *
	 * @param header
	 *            the message header.
	 * @param data
	 *            the message payload.
	 * @param offset
	 *            where in the array the payload starts.
	 * @param length
	 *            the length of the payload in the array.
	 */
	public SDPMessage(SDPHeader header, byte[] data, int offset, int length) {
		super(header);
		if (data == null) {
			databuf = null;
		} else {
			databuf = wrap(data, offset, length);
		}
	}

	/**
	 * Create an SDP message with a payload.
	 *
	 * @param header
	 *            the message header.
	 * @param data
	 *            the message payload, in the region from the <i>position</i> to
	 *            the <i>limit</i>.
	 */
	public SDPMessage(SDPHeader header, ByteBuffer data) {
		super(header);
		if (data == null) {
			databuf = null;
		} else {
			databuf = data.duplicate();
		}
	}

	/**
	 * Deserialises an SDP message from a buffer.
	 *
	 * @param buffer
	 *            The buffer holding the message.
	 * @param isBMP
	 *            Whether we're really talking to a BMP
	 */
	public SDPMessage(ByteBuffer buffer, boolean isBMP) {
		super(new SDPHeader(buffer, isBMP));
		databuf = buffer.duplicate();
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		sdpHeader.addToBuffer(buffer);
		if (databuf != null) {
			buffer.put(databuf);
		}
	}

	/**
	 * @return The payload of the message, as a read-only little-endian buffer.
	 */
	public final ByteBuffer getData() {
		ByteBuffer buffer;
		if (databuf != null) {
			buffer = databuf;
		} else {
			buffer = allocate(0);
		}
		return buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
	}
}
