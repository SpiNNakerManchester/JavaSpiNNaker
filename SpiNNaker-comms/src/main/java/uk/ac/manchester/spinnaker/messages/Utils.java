/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.nio.ByteBuffer;

/**
 * Utilities for working with messages.
 */
public abstract class Utils {
	private Utils() {
	}

	/**
	 * Convert a word to a buffer that could form part of a message understood
	 * by SpiNNaker.
	 *
	 * @param value
	 *            The value to put in the buffer as a single 32-bit word.
	 * @return The buffer, flipped. The buffer is writable and has a backing
	 *         array.
	 */
	public static ByteBuffer wordAsBuffer(int value) {
		ByteBuffer b = allocate(WORD_SIZE).order(LITTLE_ENDIAN);
		b.putInt(value).flip();
		return b;
	}
}
