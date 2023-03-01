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
package uk.ac.manchester.spinnaker.alloc.db;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.ThreadLocal.withInitial;
import static java.util.Collections.synchronizedList;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.springframework.dao.DataAccessResourceFailureException;

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

	private final ThreadLocal<Conn> connectionCache =
			withInitial(this::generateCachedDatabaseConnection);

	private final List<CloserThread> closerThreads =
			synchronizedList(new ArrayList<>());

	/**
	 * Actually open a connection to the database. This should do nothing that
	 * is not strictly required per connection.
	 *
	 * @return The database connection.
	 * @throws SQLException
	 *             If anything serious goes wrong.
	 */
	abstract Conn openDatabaseConnection() throws SQLException;

	/**
	 * Some threads persist a long time and need special treatment in order to
	 * clean up at the end. Identify them.
	 *
	 * @return Whether the current thread needs a full shutdown hook.
	 */
	protected static boolean isLongTermThread() {
		// Special case for the main thread
		return currentThread().getName().equals("main");
	}

	/**
	 * Sets up a cacheable database connection for a particular thread. Used to
	 * initialise a thread local ({@link #connectionCache}) that does the
	 * caching.
	 *
	 * @return The connection to cache.
	 * @throws DataAccessResourceFailureException
	 *             If the connection can't be opened.
	 */
	private Conn generateCachedDatabaseConnection() {
		Conn connection;
		try {
			connection = openDatabaseConnection();
		} catch (SQLException e) {
			throw new DataAccessResourceFailureException(
					"problem opening database connection", e);
		}
		if (isLongTermThread()) {
			// Special case for the main thread
			var t = currentThread();
			getRuntime().addShutdownHook(new Thread(() -> {
				closeDatabaseConnection(connection, t);
			}, "database closer for " + t));
		} else {
			closerThreads.add(new CloserThread(connection));
		}
		return connection;
	}

	/**
	 * A thread that manages closing a resource when the creating thread dies.
	 *
	 * @author Donal Fellows
	 */
	private final class CloserThread extends Thread {
		private final Thread owner;

		private final Conn resource;

		private boolean stopped;

		@SuppressWarnings("ThreadPriorityCheck")
		CloserThread(Conn c) {
			owner = currentThread();
			resource = c;
			setName("database closer for " + owner);
			setPriority(MAX_PRIORITY);
			setUncaughtExceptionHandler(this::logExn);
			start();
		}

		@Override
		public void run() {
			try {
				// Tricky point: may have several threads join on one!
				owner.join();
			} catch (InterruptedException e) {
				log.trace("interrupted when thread already dying", e);
			} finally {
				stopped = true;
				closeDatabaseConnection(resource, owner);
			}
		}

		private void logExn(Thread thread, Throwable ex) {
			log.warn("unexpected exception in thread terminate watcher for {}",
					owner, ex);
		}
	}

	/**
	 * Does the closing of the database connection.
	 *
	 * @param connection
	 *            The connection to close.
	 */
	private void closeDatabaseConnection(Conn connection, Thread owner) {
		try {
			log.debug("closing connection for {}", owner);
			connection.close();
		} catch (SQLException e) {
			log.warn("problem closing database connection", e);
		}
	}

	/**
	 * Wait for all made threads to terminate.
	 */
	@PreDestroy
	@SuppressWarnings("ThreadJoinLoop")
	private void shutdown() {
		log.info("waiting for all database connections to close");
		long before = currentTimeMillis();
		for (var t : List.copyOf(closerThreads)) {
			try {
				if (!t.stopped) {
					t.interrupt();
					t.join();
				}
			} catch (InterruptedException e) {
				log.trace("interrupted when waiting for "
						+ "database connections to close", e);
			}
		}
		long after = currentTimeMillis();
		log.info("waited for {} milliseconds", after - before);
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
