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
package uk.ac.manchester.spinnaker.utils;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Integer.toHexString;

/**
 * Miscellaneous mathematical functions.
 *
 * @author Donal Fellows
 */
public abstract class MathUtils {
	private MathUtils() {
	}

	/**
	 * Divide one integer by another with rounding up. For example,
	 * {@code ceildiv(5,3) == 2}
	 *
	 * @param numerator
	 *            The value to be divided. Must be non-negative.
	 * @param denominator
	 *            The value to divide by. Must be positive.
	 * @return The value got by dividing the two, and rounding any floating
	 *         remainder <i>up</i>.
	 */
	public static final int ceildiv(int numerator, int denominator) {
		/*
		 * Measured as faster than:
		 *
		 * return (int) Math.ceil((float) numerator / (float) denominator);
		 *
		 * and also as faster than:
		 *
		 * return (numerator / denominator)
		 * 		+ (numerator % denominator != 0 ? 1 : 0);
		 */

		return (denominator - 1 + numerator) / denominator;
	}

	/**
	 * Converts a byte to its hexadecimal representation.
	 *
	 * @param value
	 *            The byte to convert.
	 * @return The (unsigned) hexadecimal representation of the byte.
	 */
	public static String hexbyte(byte value) {
		return toHexString(toUnsignedInt(value));
	}
}
