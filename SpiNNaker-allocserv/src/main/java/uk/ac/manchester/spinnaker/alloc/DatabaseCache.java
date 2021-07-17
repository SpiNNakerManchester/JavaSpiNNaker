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
package uk.ac.manchester.spinnaker.alloc;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;

/**
 * A thread-aware cache of database connections. This looks after ensuring that
 * connections are closed when their thread is no longer running; ideal for
 * working with thread pools (shut down the pool to shut down the connections).
 *
 * @param <Conn>
 *            The exact type of the connection.
 * @author Donal Fellows
 * @see <a href="https://stackoverflow.com/a/1707173/301832">Stack Overflow</a>
 */
abstract class DatabaseCache<Conn extends Connection> {
	private static final Logger log = getLogger(DatabaseCache.class);

	private ThreadLocal<Conn> connectionCache =
			ThreadLocal.withInitial(this::generateCachedDatabaseConnection);

	/**
	 * Actually open a connection to the database. This should do nothing else.
	 *
	 * @return The database connection.
	 * @throws SQLException
	 *             If anything serious goes wrong.
	 */
	abstract Conn openDatabaseConnection() throws SQLException;

	/**
	 * Sets up a cacheable database connection for a particular thread. Used to
	 * initialise a thread local ({@link #connectionCache}) that does the
	 * caching.
	 *
	 * @return The connection to cache.
	 */
	private Conn generateCachedDatabaseConnection() {
		Conn connection;
		try {
			connection = openDatabaseConnection();
		} catch (SQLException e) {
			throw new RuntimeException("problem opening database connection",
					e);
		}
		new CloserThread(connection);
		return connection;
	}

	/**
	 * A thread that manages closing a resource when the creating thread dies.
	 *
	 * @author Donal Fellows
	 */
	private class CloserThread extends Thread {
		private final Thread owner;

		private final Conn resource;

		CloserThread(Conn c) {
			owner = Thread.currentThread();
			resource = c;
			setName("database closer for " + owner);
			setPriority(Thread.MAX_PRIORITY);
			start();
		}

		@Override
		public void run() {
			try {
				// Tricky point: may have several threads join on one!
				owner.join();
			} catch (InterruptedException e) {
				// thread dying; ignore
			} finally {
				closeDatabaseConnection(resource);
			}
		}
	}

	/**
	 * Does the closing of the database connection.
	 *
	 * @param connection
	 *            The connection to close.
	 */
	void closeDatabaseConnection(Conn connection) {
		try {
			connection.close();
		} catch (SQLException e) {
			log.warn("problem closing database connection", e);
		}
	}

	/**
	 * Get a database connection for the current thread. The database is not
	 * otherwise initialised by this call. This may be called multiple times by
	 * the same thread; the value is cached and will be closed when the thread
	 * that the connection for is no longer running.
	 * <p>
	 * Initialisation of the connection is up to the caller of this method.
	 *
	 * @return A connection to the database.
	 * @throws RuntimeException
	 *             if opening failed
	 */
	final Conn getCachedDatabaseConnection() {
		return connectionCache.get();
	}
}
