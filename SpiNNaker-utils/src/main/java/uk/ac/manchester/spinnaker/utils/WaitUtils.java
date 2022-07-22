/*
 * Copyright (c) 2018-2020 The University of Manchester
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

import static java.lang.System.nanoTime;
import static java.util.concurrent.locks.LockSupport.parkNanos;

import java.util.concurrent.locks.LockSupport;

/**
 * Utilities for waiting very short periods of time.
 *
 * @author Donal Fellows
 */
public abstract class WaitUtils {
	private WaitUtils() {
	}

	/**
	 * Wait until the given time has passed. May decide to not wait at all.
	 *
	 * @param nanoTimestamp
	 *            The first time at which the code may return. If in the past,
	 *            returns immediately.
	 * @see System#nanoTime()
	 * @see LockSupport#parkNanos(long)
	 * @see <a href="https://stackoverflow.com/q/35875117/301832">Stack
	 *      Overflow</a>
	 */
	public static void waitUntil(long nanoTimestamp) {
		// Critical: this is static so JRE can inline this code!
		while (true) {
			long dt = nanoTimestamp - nanoTime();
			if (dt <= 0) {
				break;
			}
			parkNanos(dt);
		}
	}
}
