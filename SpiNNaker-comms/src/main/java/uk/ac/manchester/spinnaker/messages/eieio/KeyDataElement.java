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

/** A data element that contains just a key. */
public class KeyDataElement implements AbstractDataElement {
	private final int key;

	/**
	 * Create a data element.
	 * @param key The key in the element.
	 */
	public KeyDataElement(int key) {
		this.key = key;
	}

	@Override
	public final void addToBuffer(ByteBuffer buffer, EIEIOType eieioType) {
		if (eieioType.payloadBytes != 0) {
			throw new IllegalArgumentException(
					"The type specifies a payload, but this element has no"
							+ " payload");
		}
		switch (eieioType) {
		case KEY_16_BIT:
			buffer.putShort((short) key);
			return;
		case KEY_32_BIT:
			buffer.putInt(key);
			return;
		default:
			throw new IllegalArgumentException("Unknown type");
		}
	}
}
