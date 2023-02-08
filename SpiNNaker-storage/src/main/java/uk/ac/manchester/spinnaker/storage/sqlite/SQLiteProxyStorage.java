/*
 * Copyright (c) 2022 The University of Manchester
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

import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_PROXY_INFORMATION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.PROXY_AUTH;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.PROXY_URI;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.SPALLOC_URI;

import java.sql.Connection;
import java.sql.SQLException;

import uk.ac.manchester.spinnaker.storage.ConnectionProvider;
import uk.ac.manchester.spinnaker.storage.DatabaseAPI;
import uk.ac.manchester.spinnaker.storage.ProxyAwareStorage;
import uk.ac.manchester.spinnaker.storage.ProxyInformation;
import uk.ac.manchester.spinnaker.storage.StorageException;

abstract class SQLiteProxyStorage<T extends DatabaseAPI>
		extends SQLiteConnectionManager<T> implements ProxyAwareStorage {
	/** Standard prefix for the bearer token header, which will be stripped. */
	private static final String PREFIX = "Bearer ";

	protected SQLiteProxyStorage(ConnectionProvider<T> connProvider) {
		super(connProvider);
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
	 * @throws IllegalStateException
	 *             If a bad row is retrieved; should be unreachable if SQL is
	 *             synched to code.
	 */
	private ProxyInformation getProxyInfo(Connection conn) throws SQLException {
		String spallocUri = null;
		String jobUri = null;
		String bearerToken = null;
		try (var s = conn.prepareStatement(GET_PROXY_INFORMATION);
				var rs = s.executeQuery()) {
			while (rs.next()) {
				var name = rs.getString("name");
				var value = rs.getString("value");
				if (name == null || value == null) {
					continue;
				}
				switch (name) {
				case SPALLOC_URI:
					spallocUri = value;
					break;
				case PROXY_URI:
					jobUri = value;
					break;
				case PROXY_AUTH:
					if (!value.startsWith(PREFIX)) {
						throw new SQLException(
								"Unexpected proxy authentication: " + value);
					}
					bearerToken = value.substring(PREFIX.length());
					break;
				default:
					throw new IllegalStateException("unreachable reached");
				}
			}
		}
		// If we don't have all pieces of info, we can't talk to the proxy
		if (spallocUri == null || jobUri == null || bearerToken == null) {
			return null;
		}
		return new ProxyInformation(spallocUri, jobUri, bearerToken);
	}
}
