package uk.ac.manchester.spinnaker.storage.sqlite;

import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.FIRST;
import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.SECOND;
import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.SEVENTH;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_PROXY_INFORMATION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.PROXY_AUTH;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.PROXY_URI;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.SPALLOC_URI;

import java.sql.Connection;
import java.sql.SQLException;

import uk.ac.manchester.spinnaker.storage.ProxyInformation;

public final class SQLiteProxyInformation {

	private SQLiteProxyInformation() {
	}

	/**
	 * Get the proxy information from a database.
	 *
	 * @param conn The connection to read the data from.
	 * @return The proxy information.
	 * @throws SQLException If there is an error reading the database.
	 */
	public static ProxyInformation getSQLProxyInformation(Connection conn)
			throws SQLException {
		String spallocUri = null;
		String jobUri = null;
		String bearerToken = null;
		try (var s = conn.prepareStatement(GET_PROXY_INFORMATION)) {
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					String name = rs.getString(FIRST);
					String value = rs.getString(SECOND);
					if (name.equals(SPALLOC_URI)) {
						spallocUri = value;
					} else if (name.equals(PROXY_URI)) {
						jobUri = value;
					} else if (name.equals(PROXY_AUTH)) {
						bearerToken = value;
						if (!bearerToken.startsWith("Bearer ")) {
							throw new SQLException(
									"Unexpected proxy authentication: "
							        + bearerToken);
						}
						bearerToken = bearerToken.substring(SEVENTH);
					}
				}
			}
		}
		if (spallocUri == null || jobUri == null || bearerToken == null) {
			return null;
		}
		return new ProxyInformation(spallocUri, jobUri, bearerToken);
	}

}
