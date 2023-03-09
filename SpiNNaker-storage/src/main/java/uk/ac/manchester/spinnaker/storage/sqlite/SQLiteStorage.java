/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage.sqlite;

import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteErrorCode.SQLITE_BUSY;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.COOKIE;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_PROXY_INFORMATION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.HEADER;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.PROXY_URI;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.SPALLOC;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.SPALLOC_URI;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.sqlite.SQLiteException;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.storage.DatabaseAPI;
import uk.ac.manchester.spinnaker.storage.DatabaseEngine;
import uk.ac.manchester.spinnaker.storage.ProxyInformation;
import uk.ac.manchester.spinnaker.storage.StorageException;

/**
 * Wrapper that handles how to perform transactions.
 *
 * @author Donal Fellows
 * @param <APIType>
 *            The type of the connections used inside this class. Probably the
 *            type of the concrete subclass of this class.
 */
abstract sealed class SQLiteStorage<APIType extends DatabaseAPI>
		implements DatabaseAPI
		permits SQLiteBufferStorage, SQLiteDataSpecStorage {
	private static final Logger log = getLogger(SQLiteStorage.class);

	@GuardedBy("itself")
	private final DatabaseEngine<APIType> db;

	/**
	 * @param db
	 *            The source of database connections.
	 * @see Connection
	 */
	protected SQLiteStorage(DatabaseEngine<APIType> db) {
		this.db = db;
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
		synchronized (db) {
			try (var conn = db.getConnection()) {
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
		synchronized (db) {
			try (var conn = db.getConnection()) {
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
				case SQLITE_BUSY, SQLITE_BUSY_RECOVERY, SQLITE_BUSY_SNAPSHOT,
						SQLITE_BUSY_TIMEOUT -> {
					log.debug("database busy; trying to relock");
					code = e.getResultCode();
				}
				default -> throw e;
				}
			}
		}
		throw new SQLiteException("database very busy", code);
	}

	@Override
	public ProxyInformation getProxyInformation() throws StorageException {
		return callR(conn -> getProxyInfo(conn), "get proxy");
	}

	/**
	 * Get the proxy information from a database.
	 *
	 * @param conn
	 *            The connection to read the data from.
	 * @return The proxy information.
	 * @throws SQLException
	 *             If there is an error reading the database.
	 * @throws Unreachable
	 *             If a bad row is retrieved; should be unreachable if SQL is
	 *             synched to code.
	 */
	@SuppressWarnings("checkstyle:InnerAssignment") // Rule is misapplying
	private ProxyInformation getProxyInfo(Connection conn) throws SQLException {
		String spallocUri = null;
		String jobUri = null;
		var headers = new HashMap<String, String>();
		var cookies = new HashMap<String, String>();

		try (var s = conn.prepareStatement(GET_PROXY_INFORMATION);
				var rs = s.executeQuery()) {
			while (rs.next()) {
				var kind = rs.getString("kind");
				var name = rs.getString("name");
				var value = rs.getString("value");
				if (kind == null || name == null || value == null) {
					continue;
				}
				switch (kind) {
				case SPALLOC -> {
					switch (name) {
					case SPALLOC_URI -> spallocUri = value;
					case PROXY_URI -> jobUri = value;
					default -> throw new Unreachable();
					}
				}

				case COOKIE -> cookies.put(name, value);
				case HEADER -> headers.put(name, value);
				default -> throw new Unreachable();
				}
			}
		}
		// If we don't have all pieces of info, we can't talk to the proxy
		if (spallocUri == null || jobUri == null) {
			return null;
		}
		return new ProxyInformation(spallocUri, jobUri, headers, cookies);
	}

	/** Thrown when an unreachable state is reached. */
	private static class Unreachable extends IllegalStateException {
		private static final long serialVersionUID = 1L;

		Unreachable() {
			super("unreachable reached");
		}
	}
}
