/*
 * Copyright (c) 2021-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.tools;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;

/**
 * Database access code. Essential to allow us to avoid needing to query the
 * user a lot and to keep the registration access token so we can update the
 * client in the future (as those are deeply magical and only issued once).
 *
 * @author Donal Fellows
 */
public class CredentialDB implements AutoCloseable {
	private static final Logger log = getLogger(CredentialDB.class);

	private final SQLiteConnection db;

	/**
	 * @param databaseFile
	 *            Where the database is.
	 * @throws SQLException
	 *             If we can't open the DB or set it up.
	 * @throws IOException
	 *             If we can't resolve the database filename properly.
	 */
	public CredentialDB(File databaseFile) throws SQLException, IOException {
		var config = new SQLiteConfig();
		config.enforceForeignKeys(true);
		db = (SQLiteConnection) config.createConnection(
				"jdbc:sqlite:" + databaseFile.getCanonicalPath());
		try (var s = db.createStatement()) {
			s.execute("CREATE TABLE IF NOT EXISTS tokens("
					+ "client TEXT UNIQUE NOT NULL, token TEXT NOT NULL)");
			s.execute("CREATE TABLE IF NOT EXISTS users("
					+ "name TEXT UNIQUE NOT NULL, pass TEXT NOT NULL)");
		}
	}

	@Override
	public void close() throws SQLException {
		db.close();
	}

	/**
	 * Credentials loaded from the database. This assumes that there's only ever
	 * one set of developer credentials in the DB, which is deeply simplistic,
	 * but true in practice.
	 *
	 * @author Donal Fellows
	 */
	final class DBContainedCredentials implements EBRAINSDevCredentials {
		private final String name;

		private final String pass;

		/** Make an instance. */
		DBContainedCredentials() throws SQLException, IllegalStateException {
			try (var s = db.createStatement();
					var rs = s.executeQuery(
							"SELECT name, pass FROM users LIMIT 1")) {
				if (rs.next()) {
					name = rs.getString("name");
					pass = rs.getString("pass");
				} else {
					throw new IllegalStateException("no user in DB");
				}
			}
		}

		@Override
		public String getUser() {
			return name;
		}

		@Override
		public String getPass() {
			return pass;
		}
	}

	/**
	 * Save some credentials in the database. These probably shouldn't be
	 * {@link DBContainedCredentials}.
	 *
	 * @param creds
	 *            The credentials to save.
	 * @throws SQLException
	 *             If database access fails.
	 */
	final void saveCredentials(EBRAINSDevCredentials creds)
			throws SQLException {
		try (var s = db.prepareStatement(
				"INSERT OR REPLACE INTO users(name, pass) VALUES (?, ?)")) {
			s.setString(1, creds.getUser());
			s.setString(2, creds.getPass());
			s.executeUpdate();
		}
	}

	/**
	 * Get the registration access token for a particular client.
	 *
	 * @param clientId
	 *            The client to get the token of.
	 * @return The token, or {@code null} if no token is available.
	 */
	final String getToken(String clientId) {
		try (var s = db.prepareStatement(
				"SELECT token FROM tokens WHERE client = ? LIMIT 1")) {
			s.setString(1, requireNonNull(clientId));
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getString(1);
				}
			}
			log.error("failed to find client {} registration access token",
					clientId);
			return null;
		} catch (SQLException e) {
			log.error("failed to find client {} registration access token",
					clientId, e);
			return null;
		}
	}

	/**
	 * Save the registration access token for a client.
	 *
	 * @param clientId
	 *            The client to save the token of.
	 * @param token
	 *            The registration access token to save.
	 * @throws SQLException
	 *             If database access fails.
	 */
	final void saveToken(String clientId, String token) throws SQLException {
		try (var s = db.prepareStatement(
				"INSERT OR REPLACE INTO tokens(client, token) "
						+ "VALUES (?, ?)")) {
			s.setString(1, clientId);
			s.setString(2, token);
			s.executeUpdate();
		}
	}
}
