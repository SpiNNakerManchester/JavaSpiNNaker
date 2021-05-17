/*
 * Copyright (c) 2021 The University of Manchester
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
		OneThread ot = new OneThread();
		return new OTConnection(ot, conn);
	}

	/** <em>Only</em> the factory can make this. */
	private OneThread() {
		this.thread = currentThread();
	}
}
