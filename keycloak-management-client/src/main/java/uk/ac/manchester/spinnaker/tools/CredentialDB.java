/*
 * Copyright (c) 2021-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.tools;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;

public class CredentialDB implements AutoCloseable {
	private static final Logger log = getLogger(CredentialDB.class);

	private final SQLiteConnection db;

	public CredentialDB(File databaseFile) throws SQLException, IOException {
		SQLiteConfig config = new SQLiteConfig();
		config.enforceForeignKeys(true);
		db = (SQLiteConnection) config.createConnection(
				"jdbc:sqlite:" + databaseFile.getCanonicalPath());
		try (Statement s = db.createStatement()) {
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

	final class DBContainedCredentials implements EBRAINSDevCredentials {
		private final String name;

		private final String pass;

		DBContainedCredentials() throws SQLException, IllegalStateException {
			try (Statement s = db.createStatement();
					ResultSet rs = s.executeQuery(
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

	void saveCredentials(EBRAINSDevCredentials creds) throws SQLException {
		try (PreparedStatement s = db.prepareStatement(
				"INSERT OR REPLACE INTO users(name, pass) VALUES (?, ?)")) {
			s.setString(1, creds.getUser());
			s.setString(2, creds.getPass());
			s.executeUpdate();
		}
	}

	String getToken(String clientId) {
		try (PreparedStatement s = db.prepareStatement(
				"SELECT token FROM tokens WHERE client = ? LIMIT 1")) {
			s.setString(1, requireNonNull(clientId));
			try (ResultSet rs = s.executeQuery()) {
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

	void saveToken(String clientId, String registrationAccessToken)
			throws SQLException {
		try (PreparedStatement s = db.prepareStatement(
				"INSERT OR REPLACE INTO tokens(client, token) "
						+ "VALUES (?, ?)")) {
			s.setString(1, clientId);
			s.setString(2, registrationAccessToken);
			s.executeUpdate();
		}
	}
}
