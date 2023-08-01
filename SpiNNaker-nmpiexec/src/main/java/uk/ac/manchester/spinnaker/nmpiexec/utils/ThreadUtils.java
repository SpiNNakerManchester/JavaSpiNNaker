/*
 * Copyright (c) 2014 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpiexec.utils;

/**
 * Utilities for working with threads.
 */
public abstract class ThreadUtils {

	/**
	 * Avoid instantiation.
	 */
	private ThreadUtils() {
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
}
