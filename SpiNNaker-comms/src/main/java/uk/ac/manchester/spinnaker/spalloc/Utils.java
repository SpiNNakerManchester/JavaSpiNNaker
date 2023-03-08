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
package uk.ac.manchester.spinnaker.spalloc;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;

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
		if (timestamp == null) {
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
		return timestamp != null && timestamp < currentTimeMillis();
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
		if (delay == null) {
			return null;
		}
		return currentTimeMillis() + delay;
	}
}
