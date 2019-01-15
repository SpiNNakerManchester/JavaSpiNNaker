/*
 * Copyright (c) 2018 The University of Manchester
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

import java.sql.Connection;
import java.sql.SQLException;

import org.sqlite.SQLiteException;

import uk.ac.manchester.spinnaker.storage.ConnectionProvider;
import uk.ac.manchester.spinnaker.storage.StorageException;

/**
 * Wrapper that handles how to perform transactions.
 *
 * @author Donal Fellows
 */
abstract class SQLiteConnectionManager {
	private final ConnectionProvider connProvider;

	/**
	 * @param connProvider
	 *            The source of database connections.
	 * @see Connection
	 */
	protected SQLiteConnectionManager(ConnectionProvider connProvider) {
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
		try (Connection conn = connProvider.getConnection()) {
			conn.setAutoCommit(false);
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

	private static final int ATTEMPTS = 5;

	private void startTransaction(Connection conn) throws SQLException {
		int tries = ATTEMPTS;
		while (tries > 0) {
			try {
				conn.setAutoCommit(false);
				conn.createStatement().execute("BEGIN IMMEDIATE TRANSACTION");
				return;
			} catch (SQLiteException e) {
				switch (e.getResultCode()) {
				case SQLITE_BUSY:
				case SQLITE_LOCKED:
					tries--;
					break;
				default:
					throw e;
				}
			}
		}
	}
}
