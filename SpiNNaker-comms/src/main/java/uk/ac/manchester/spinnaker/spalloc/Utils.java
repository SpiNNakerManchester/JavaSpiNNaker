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
package uk.ac.manchester.spinnaker.spalloc;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Utility operations for working with timestamps.
 *
 * @author Donal Fellows
 */
abstract class Utils {
	private Utils() {
	}

	/**
	 * Convert a timestamp into how long to wait for it.
	 *
	 * @param timestamp
	 *            the time of expiry, or {@code null} for a timestamp that is
	 *            never expired.
	 * @return the number of milliseconds remaining to wait for the timestamp to
	 *         expire, or {@code null} for "wait indefinitely".
	 */
	static Integer timeLeft(Long timestamp) {
		if (isNull(timestamp)) {
			return null;
		}
		return max(0, (int) (timestamp - currentTimeMillis()));
	}

	/**
	 * Check if a timestamp has been reached.
	 *
	 * @param timestamp
	 *            the time of expiry, or {@code null} for a timestamp that is
	 *            never expired.
	 * @return true if the timestamp has been passed.
	 */
	static boolean timedOut(Long timestamp) {
		return nonNull(timestamp) && timestamp < currentTimeMillis();
	}

	/**
	 * Convert a delay (in milliseconds) into a timestamp.
	 *
	 * @param delay
	 *            how long the timeout is, in milliseconds, or {@code null} for
	 *            infinite.
	 * @return when the timeout expires, or {@code null} for "never".
	 */
	static Long makeTimeout(Integer delay) {
		if (isNull(delay)) {
			return null;
		}
		return currentTimeMillis() + delay;
	}
}
