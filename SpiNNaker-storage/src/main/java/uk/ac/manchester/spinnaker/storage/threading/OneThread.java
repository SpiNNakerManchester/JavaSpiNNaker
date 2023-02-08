/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage.threading;

import static java.lang.Thread.currentThread;

import java.sql.Connection;

/**
 * A wrapper for an SQL connection that enforces that it is only used from its
 * creating thread.
 *
 * @author Donal Fellows
 */
public final class OneThread {
	/** The thread to which a connection (and related classes) are bound. */
	final Thread thread;

	/**
	 * Create the wrapper.
	 *
	 * @param conn
	 *            The connection that is being protected.
	 * @return The wrapped connection.
	 */
	public static Connection threadBound(Connection conn) {
		var ot = new OneThread();
		return new OTConnection(ot, conn, true);
	}

	/**
	 * Create the wrapper. This wrapper cannot be closed (the
	 * {@link Connection#close()} method fails silently); it is up to the caller
	 * to save the wrapped connection and close it at the right time.
	 *
	 * @param conn
	 *            The connection that is being protected.
	 * @return The wrapped connection.
	 */
	public static Connection uncloseableThreadBound(Connection conn) {
		var ot = new OneThread();
		return new OTConnection(ot, conn, false);
	}

	/** <em>Only</em> the factory methods can make this. */
	private OneThread() {
		this.thread = currentThread();
	}
}
