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

/** Possible prefixing of keys in EIEIO packets. */
public enum EIEIOPrefix {
	/** Apply prefix on lower half of the word. */
	LOWER_HALF_WORD(0, 0),
	/** Apply prefix on top half of the word. */
	UPPER_HALF_WORD(1, 16);

	private final int value;
	/** How much to shift things by. */
	final int shift;

	/**
	 * @param value the value
	 * @param shift the shift
	 */
	EIEIOPrefix(int value, int shift) {
		this.value = value;
		this.shift = shift;
	}

	/** @return the encoded form. */
	public int getValue() {
		return value;
	}

	/**
	 * Get the prefix encoding given its encoded form.
	 *
	 * @param value
	 *            The encoded prefix encoding.
	 * @return The prefix encoding object
	 * @throws IllegalArgumentException
	 *             if the encoded prefix encoding is unrecognised.
	 */
	public static EIEIOPrefix getByValue(int value) {
		for (EIEIOPrefix p : values()) {
			if (p.value == value) {
				return p;
			}
		}
		throw new IllegalArgumentException("no such prefix");
	}
}
