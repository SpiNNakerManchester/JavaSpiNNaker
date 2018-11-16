package uk.ac.manchester.spinnaker.utils;

import static java.lang.Math.ceil;

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
	 *            The value to be divided.
	 * @param denominator
	 *            The value to divide by.
	 * @return The value got by dividing the two, and rounding any floating
	 *         remainder <i>up</i>.
	 */
	public static final int ceildiv(int numerator, int denominator) {
		return (int) ceil((float) numerator / (float) denominator);
	}
}
