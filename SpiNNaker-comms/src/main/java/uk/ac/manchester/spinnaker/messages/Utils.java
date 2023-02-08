/*
 * Copyright (c) 2023 The University of Manchester
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
