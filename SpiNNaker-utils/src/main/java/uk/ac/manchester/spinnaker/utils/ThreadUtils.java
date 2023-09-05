/*
 * Copyright (c) 2023 The University of Manchester
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

import static java.lang.management.ManagementFactory.getThreadMXBean;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.lang.management.ThreadInfo;

/**
 * Utilities for working with threads.
 */
public abstract class ThreadUtils {
	/** Avoid creation. */
	private ThreadUtils() {
	}

	/**
	 * Produce a dump of what all threads are doing. Useful for debugging as it
	 * means you can get a dump of threads programmatically at the time when the
	 * problem is likely to be on some thread's stack.
	 *
	 * @return The dump, as a multi-line string.
	 */
	public static String threadDump() {
		return stream(getThreadMXBean().dumpAllThreads(true, true))
				.map(ThreadInfo::toString).collect(joining());
	}

	/**
	 * Recommended way of doing "quiet" sleeps.
	 *
	 * @param delay
	 *            How long to sleep for, in milliseconds.
	 * @see <a href="https://stackoverflow.com/q/1087475/301832">Stack Overflow
	 *      Question: When does Java's Thread.sleep throw
	 *      InterruptedException?</a>
	 */
	public static void sleep(final long delay) {
		try {
			Thread.sleep(delay);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Wait for the given object.
	 *
	 * @param obj
	 *            The object to wait for
	 * @return True if the wait was interrupted, false otherwise
	 */
	public static boolean waitfor(final Object obj) {
		try {
			obj.wait();
			return false;
		} catch (final InterruptedException e) {
			return true;
		}
	}

	/**
	 * Wait for the given object.
	 *
	 * @param obj
	 *            The object to wait for
	 * @param timeout
	 *            The maximum time to wait, in milliseconds
	 * @return True if the wait was interrupted, false otherwise
	 */
	public static boolean waitfor(final Object obj, final long timeout) {
		try {
			obj.wait(timeout);
			return false;
		} catch (final InterruptedException e) {
			return true;
		}
	}
}
