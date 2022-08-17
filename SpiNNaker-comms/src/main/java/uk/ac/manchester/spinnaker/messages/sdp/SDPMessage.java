/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
