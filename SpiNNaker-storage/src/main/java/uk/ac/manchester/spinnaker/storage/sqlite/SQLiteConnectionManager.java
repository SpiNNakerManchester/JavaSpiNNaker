/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage.sqlite;

import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteErrorCode.SQLITE_BUSY;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.sqlite.SQLiteException;

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
			try (Connection conn = connProvider.getConnection()) {
				startTransaction(conn);
				try {
					T result = call.call(conn);
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
			try (Connection conn = connProvider.getConnection()) {
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
		for (int i = 0; i < TRIES; i++) {
			try {
				conn.setAutoCommit(false);
				return;
			} catch (SQLiteException e) {
				if (e.getResultCode() == SQLITE_BUSY) {
                    if (log.isDebugEnabled()) {
    					log.debug("database busy; trying to relock");
                    }
					continue;
				}
				throw e;
			}
		}
		throw new SQLiteException("database very busy", SQLITE_BUSY);
	}
}
