/*
 * Copyright (c) 2018 The University of Manchester
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
