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
package uk.ac.manchester.spinnaker.messages.model;

import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/** What to move data in units of. */
public enum TransferUnit {
	/** A byte. */
	BYTE(0),
	/** Two bytes. */
	HALF_WORD(1),
	/** Four bytes. */
	WORD(2);

	/**
	 * The encoded transfer unit.
	 */
	public final int value;

	TransferUnit(int value) {
		this.value = value;
	}

	/**
	 * Is a value an odd number?
	 *
	 * @param value
	 *            The value to test
	 * @return True exactly when the value is odd.
	 */
	private static boolean odd(int value) {
		return (value & 1) == 1;
	}

	/**
	 * What is an efficient transfer unit to use, given a starting address and a
	 * size of data to move.
	 *
	 * @param address
	 *            The address.
	 * @param size
	 *            The data size.
	 * @return The preferred transfer unit.
	 */
	public static TransferUnit efficientTransferUnit(MemoryLocation address,
			int size) {
		if (address.isAligned() && size % WORD_SIZE == 0) {
			return WORD;
		} else if (odd(address.address) || odd(size)) {
			return BYTE;
		} else {
			return HALF_WORD;
		}
	}
}
