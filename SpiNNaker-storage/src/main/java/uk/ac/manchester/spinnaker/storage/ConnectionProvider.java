package uk.ac.manchester.spinnaker.storage;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Indicates a class that can provide database connections to
 * suitably-configured SQLite databases.
 *
 * @author Donal Fellows
 */
public interface ConnectionProvider {
	/**
	 * Get a connection to an SQLite database, creating it if needed.
	 *
	 * @return The configured connection to the database. The database will have
	 *         been seeded with DDL if necessary.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	Connection getConnection() throws SQLException;
}
