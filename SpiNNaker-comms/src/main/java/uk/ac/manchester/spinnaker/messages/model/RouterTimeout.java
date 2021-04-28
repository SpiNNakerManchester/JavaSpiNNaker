/*
 * Copyright (c) 2019 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus.MASK;
import static uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus.SHIFT;

/**
 * A router timeout, originally stored as an 8-bit unsigned float.
 *
 * @author Donal Fellows
 */
public final class RouterTimeout {
	private static final int MANTISSA_OFFSET = 16;

	private static final int EXPONENT_OFFSET = 4;

	/** An infinite timeout. */
	public static final RouterTimeout INF = new RouterTimeout(15, 15);

	private static final double CLOCK_INTERVAL = 1e9 / 133e6;

	/** The mantissa of the timeout. */
	public final int mantissa;

	/** The exponent of the timeout. */
	public final int exponent;

	RouterTimeout(int encodedValue) {
		mantissa = encodedValue & MASK;
		exponent = (encodedValue >> SHIFT) & MASK;
	}

	/**
	 * @param mantissa
	 *            The mantissa of the value; only low 4 bits used.
	 * @param exponent
	 *            The exponent of the value; only low 4 bits used.
	 */
	public RouterTimeout(int mantissa, int exponent) {
		this.mantissa = mantissa & MASK;
		this.exponent = exponent & MASK;
	}

	/**
	 * @return the timeout value of a router in ticks.
	 */
	public int getValue() {
		int m = mantissa + MANTISSA_OFFSET;
		if (exponent <= EXPONENT_OFFSET) {
			return (m - (1 << (EXPONENT_OFFSET - exponent))) * (1 << exponent);
		}
		return m * (1 << exponent);
	}

	@Override
	public String toString() {
		if (mantissa == INF.mantissa && exponent == INF.exponent) {
			return "INF";
		}
		return "" + (getValue() * CLOCK_INTERVAL) + " ns";
	}
}
