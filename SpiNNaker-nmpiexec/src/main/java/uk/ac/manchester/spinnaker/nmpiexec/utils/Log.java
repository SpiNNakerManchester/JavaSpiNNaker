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
 * A very simple-minded logger.
 */
public abstract class Log {
	/**
	 * Avoid instantiation.
	 */
	private Log() {
		// Does Nothing
	}

	/**
	 * Write a message to the log.
	 *
	 * @param message
	 *            The message to write.
	 */
	public static void log(String message) {
		System.err.println(message);
	}

	/**
	 * Write an exception to the log.
	 *
	 * @param exception
	 *            The exception to write.
	 */
	public static void log(Throwable exception) {
		exception.printStackTrace(System.err);
	}
}
