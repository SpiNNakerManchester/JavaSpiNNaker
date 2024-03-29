/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.eieio;

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.messages.Constants.NBBY;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

import java.util.Map;

/** Possible types of EIEIO packets. */
public enum EIEIOType {
	/** Indicates that data is keys which are 16 bits. */
	KEY_16_BIT(0, 2, 0),
	/** Indicates that data is keys and payloads of 16 bits. */
	KEY_PAYLOAD_16_BIT(1, 2, 2),
	/** Indicates that data is keys of 32 bits. */
	KEY_32_BIT(2, 4, 0),
	/** Indicates that data is keys and payloads of 32 bits. */
	KEY_PAYLOAD_32_BIT(3, 4, 4);

	private static final Map<Integer, EIEIOType> MAP =
			makeEnumBackingMap(values(), v -> v.value);

	private final int value;

	/** The number of bytes used by each key element. */
	public final int keyBytes;

	/** The number of bytes used by each payload element. */
	public final int payloadBytes;

	/** The maximum value of the key or payload (if there is a payload). */
	public final long maxValue;

	/**
	 * @param value
	 *            the value
	 * @param keyBytes
	 *            the number of bytes per key
	 * @param payloadBytes
	 *            the number of bytes per value
	 */
	EIEIOType(int value, int keyBytes, int payloadBytes) {
		this.value = value;
		this.keyBytes = keyBytes;
		this.payloadBytes = payloadBytes;
		this.maxValue = (1L << (keyBytes * NBBY)) - 1;
	}

	/** @return The encoded type. */
	public int getValue() {
		return value;
	}

	/**
	 * Get the type given its encoded form.
	 *
	 * @param value
	 *            The encoded type.
	 * @return The type object
	 * @throws IllegalArgumentException
	 *             if the encoded type is unrecognised.
	 */
	public static EIEIOType getByValue(int value) {
		return requireNonNull(MAP.get(value), "no such type");
	}
}
