/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage.sqlite;

import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteErrorCode.SQLITE_BUSY;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.sqlite.SQLiteException;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.storage.ConnectionProvider;
import uk.ac.manchester.spinnaker.storage.DatabaseAPI;
import uk.ac.manchester.spinnaker.storage.StorageException;

/**
 * Wrapper that handles how to perform transactions.
 *
 * @author Donal Fellows
 * @param <APIType>
 *            The type of the connections used inside this class. Probably the
 *            type of the concrete subclass of this class.
 */
abstract class SQLiteConnectionManager<APIType extends DatabaseAPI> {
	private static final Logger log = getLogger(SQLiteConnectionManager.class);

	@GuardedBy("itself")
	private final ConnectionProvider<APIType> connProvider;

	/**
	 * @param connProvider
	 *            The source of database connections.
	 * @see Connection
	 */
	protected SQLiteConnectionManager(
			ConnectionProvider<APIType> connProvider) {
		this.connProvider = connProvider;
	}

	/**
	 * A wrapped piece of code that produces a result.
	 *
	 * @param <T>
	 *            The type of the result of the call
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	interface CallWithResult<T> {
		/**
		 * The wrapped code.
		 *
		 * @param conn
		 *            The connection that has a running transaction on it.
		 * @return The result of the code.
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		T call(Connection conn) throws SQLException;
	}

	/**
	 * A wrapped piece of code that doesn't produce a result.
	 *
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	interface CallWithoutResult {
		/**
		 * The wrapped code.
		 *
		 * @param conn
		 *            The connection that has a running transaction on it.
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		void call(Connection conn) throws SQLException;
	}

	/**
	 * Wrapper for applying a transaction correctly.
	 *
	 * @param <T>
	 *            The type of the result of the wrapped call.
	 * @param call
	 *            What is wrapped. Produces a result.
	 * @param actionDescription
	 *            Extra message to use with wrapping exception.
	 * @return The value returned by the call
	 * @throws StorageException
	 *             If anything goes wrong
	 */
	final <T> T callR(CallWithResult<T> call, String actionDescription)
			throws StorageException {
		synchronized (connProvider) {
			try (var conn = connProvider.getConnection()) {
				startTransaction(conn);
				try {
					var result = call.call(conn);
					conn.commit();
					return result;
				} catch (Exception e) {
					conn.rollback();
					throw e;
				}
			} catch (SQLException | IllegalStateException e) {
				throw new StorageException("while " + actionDescription, e);
			}
		}
	}

	/**
	 * Wrapper for applying a transaction correctly.
	 *
	 * @param call
	 *            What is wrapped. Produces no result.
	 * @param actionDescription
	 *            Extra message to use with wrapping exception.
	 * @throws StorageException
	 *             If anything goes wrong
	 */
	final void callV(CallWithoutResult call, String actionDescription)
			throws StorageException {
		synchronized (connProvider) {
			try (var conn = connProvider.getConnection()) {
				startTransaction(conn);
				try {
					call.call(conn);
					conn.commit();
					return;
				} catch (Exception e) {
					conn.rollback();
					throw e;
				}
			} catch (SQLException | IllegalStateException e) {
				throw new StorageException("while " + actionDescription, e);
			}
		}
	}

	private static final int TRIES = 50;

	private void startTransaction(Connection conn) throws SQLException {
		var code = SQLITE_BUSY;
		for (int i = 0; i < TRIES; i++) {
			try {
				conn.setAutoCommit(false);
				return;
			} catch (SQLiteException e) {
				switch (e.getResultCode()) {
				case SQLITE_BUSY:
				case SQLITE_BUSY_RECOVERY:
				case SQLITE_BUSY_SNAPSHOT:
				case SQLITE_BUSY_TIMEOUT:
					if (log.isDebugEnabled()) {
						log.debug("database busy; trying to relock");
					}
					code = e.getResultCode();
					continue;
				default:
					throw e;
				}
			}
		}
		throw new SQLiteException("database very busy", code);
	}
}
