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
package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/** A data element that contains a key and a payload. */
public class KeyPayloadDataElement implements AbstractDataElement {
	private final int key;

	private final int payload;

	private final boolean timestamp;

	/**
	 * Create a data element.
	 *
	 * @param key
	 *            The key in the element.
	 * @param payload
	 *            The payload in the element.
	 * @param isTimestamp
	 *            Whether this is a timestamp.
	 */
	public KeyPayloadDataElement(int key, int payload, boolean isTimestamp) {
		this.key = key;
		this.payload = payload;
		this.timestamp = isTimestamp;
	}

	/** @return Whether this represents a timestamp. */
	public boolean isTimestamp() {
		return timestamp;
	}

	@Override
	public final void addToBuffer(ByteBuffer buffer, EIEIOType eieioType) {
		if (eieioType.payloadBytes == 0) {
			throw new IllegalArgumentException(
					"The type specifies no payload, but this element has a"
							+ " payload");
		}
		switch (eieioType) {
		case KEY_PAYLOAD_16_BIT -> {
			buffer.putShort((short) key);
			buffer.putShort((short) payload);
		}
		case KEY_PAYLOAD_32_BIT -> {
			buffer.putInt(key);
			buffer.putInt(payload);
		}
		default -> throw new IllegalArgumentException("Unknown type");
		}
	}
}
