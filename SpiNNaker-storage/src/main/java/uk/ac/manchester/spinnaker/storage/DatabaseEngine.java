package uk.ac.manchester.spinnaker.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;

/**
 * The database engine interface. Based on SQLite.
 *
 * @author Donal Fellows
 */
public class DatabaseEngine implements ConnectionProvider {
	private static final Logger log = getLogger(DatabaseEngine.class);
	private static String sqlDDL;
	static {
		try {
			sqlDDL = resourceToString("/db.sql", UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("failed to read database definition SQL",
					e);
		}
	}

	private String dbConnectionUrl;

	/**
	 * Create an engine interface for an in-memory database.
	 */
	public DatabaseEngine() {
		this.dbConnectionUrl = "jdbc:sqlite::memory:";
		log.info("will manage database in memory");
	}

	/**
	 * Create an engine interface for a particular database.
	 *
	 * @param dbFile
	 *            The file containing the database.
	 */
	public DatabaseEngine(File dbFile) {
		this.dbConnectionUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
		log.info("will manage database at " + dbFile.getAbsolutePath());
	}

	@Override
	public Connection getConnection() throws SQLException {
		log.debug("opening database connection {}", dbConnectionUrl);
		Connection conn = DriverManager.getConnection(dbConnectionUrl);
		try (Statement statement = conn.createStatement()) {
			statement.executeUpdate(sqlDDL);
		}
		return conn;
	}
}
