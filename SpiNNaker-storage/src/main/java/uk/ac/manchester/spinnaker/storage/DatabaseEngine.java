package uk.ac.manchester.spinnaker.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * The database engine interface. Based on SQLite.
 *
 * @author Donal Fellows
 */
public class DatabaseEngine {
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

	private File dbFile;

	/**
	 * Create an engine interface for a particular database.
	 *
	 * @param dbFile
	 *            The file containing the database.
	 */
	public DatabaseEngine(File dbFile) {
		this.dbFile = dbFile;
	}

	/**
	 * Get a connection to an SQLite database, creating it if needed.
	 *
	 * @return The configured connection to the database. The database will have
	 *         been seeded with DDL if necessary.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	Connection getConnection() throws SQLException {
		String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
		log.debug("opening database connection {}", url);
		Connection conn = DriverManager.getConnection(url);
		try (Statement statement = conn.createStatement()) {
			statement.executeUpdate(sqlDDL);
		}
		return conn;
	}

	private static final String STORE =
			"INSERT INTO storage(x, y, processor, region, content) "
					+ "VALUES(?, ?, ?, ?, ?)";
	private static final String FETCH = "SELECT content FROM storage WHERE "
			+ "x = ? AND y = ? AND processor = ? AND region = ?";
	private static final String DELETE = "DELETE FROM storage WHERE "
			+ "x = ? AND y = ? AND processor = ? AND region = ?";

	public int storeRegionContents(HasCoreLocation core, int region,
			byte[] contents) throws SQLException {
		try (Connection conn = getConnection();
				PreparedStatement s =
						conn.prepareStatement(STORE, RETURN_GENERATED_KEYS)) {
			s.setInt(1, core.getX());
			s.setInt(2, core.getY());
			s.setInt(3, core.getP());
			s.setInt(4, region);
			s.setBytes(5, contents);
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				keys.next();
				return keys.getInt(1);
			}
		}
	}

	public byte[] getRegionContents(HasCoreLocation core, int region)
			throws SQLException {
		try (Connection conn = getConnection();
				PreparedStatement s = conn.prepareStatement(FETCH)) {
			s.setInt(1, core.getX());
			s.setInt(2, core.getY());
			s.setInt(3, core.getP());
			s.setInt(4, region);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getBytes(1);
				}
				throw new IllegalArgumentException(
						"core " + core + " has no data for region " + region);
			}
		}
	}

	public void deleteRegionContents(HasCoreLocation core, int region)
			throws SQLException {
		try (Connection conn = getConnection();
				PreparedStatement s = conn.prepareStatement(DELETE)) {
			s.setInt(1, core.getX());
			s.setInt(2, core.getY());
			s.setInt(3, core.getP());
			s.setInt(4, region);
			s.executeUpdate();
		}
	}
}
