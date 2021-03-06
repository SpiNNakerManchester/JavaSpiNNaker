/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.utils;

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
		return Integer.toHexString(Byte.toUnsignedInt(value));
	}
}
