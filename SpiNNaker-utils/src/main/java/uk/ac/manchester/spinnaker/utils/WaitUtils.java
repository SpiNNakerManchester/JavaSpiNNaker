/*
 * Copyright (c) 2018-2020 The University of Manchester
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
package uk.ac.manchester.spinnaker.utils;

import static java.lang.System.nanoTime;
import static java.lang.Thread.interrupted;
import static java.util.concurrent.locks.LockSupport.parkNanos;

import java.util.concurrent.locks.LockSupport;

import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Utilities for waiting very short periods of time.
 *
 * @author Donal Fellows
 */
public abstract class WaitUtils {
	@UsedInJavadocOnly(LockSupport.class)
	private WaitUtils() {
	}

	/**
	 * Wait until the given time has passed. May decide to not wait at all.
	 *
	 * @param nanoTimestamp
	 *            The first time at which the code may return. If in the past,
	 *            returns immediately.
	 * @return Whether the wait stopped because the thread was interrupted. The
	 *         caller <em>must</em> decide what to do in that case.
	 * @see System#nanoTime()
	 * @see LockSupport#parkNanos(long)
	 * @see <a href="https://stackoverflow.com/q/35875117/301832">Stack
	 *      Overflow</a>
	 */
	@CheckReturnValue
	public static boolean waitUntil(long nanoTimestamp) {
		// Critical: this is static so JVM can inline this code!
		while (true) {
			long dt = nanoTimestamp - nanoTime();
			if (dt <= 0) {
				return interrupted();
			} else if (interrupted()) {
				return true;
			}
			parkNanos(dt);
		}
	}
}
