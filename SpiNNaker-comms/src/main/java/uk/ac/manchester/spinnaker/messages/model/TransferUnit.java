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
