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

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.messages.Constants.NBBY;

import java.util.HashMap;
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

	private static final Map<Integer, EIEIOType> MAP;

	static {
		MAP = new HashMap<>();
		for (EIEIOType v : values()) {
			MAP.put(v.value, v);
		}
	}

	private final int value;

	/** The number of bytes used by each key element. */
	public final int keyBytes;

	/** The number of bytes used by each payload element. */
	public final int payloadBytes;

	/** The maximum value of the key or payload (if there is a payload). */
	public final long maxValue;

	/**
	 * @param value the value
	 * @param keyBytes the number of bytes per key
	 * @param payloadBytes the number of bytes per value
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
