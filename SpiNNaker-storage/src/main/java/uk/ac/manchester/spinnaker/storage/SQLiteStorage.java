package uk.ac.manchester.spinnaker.storage;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * How to actually talk to an SQLite database.
 *
 * @author Donal Fellows
 */
public class SQLiteStorage {
	private final ConnectionProvider connProvider;

	private static final String STORE =
			"INSERT OR REPLACE INTO storage(x, y, processor, region, content) "
					+ "VALUES(?, ?, ?, ?, ?)";
	private static final String APPEND =
			"UPDATE storage SET content = content || ? WHERE "
					+ "x = ? AND y = ? AND processor = ? AND region = ?";
	private static final String FETCH = "SELECT content FROM storage WHERE "
			+ "x = ? AND y = ? AND processor = ? AND region = ?";
	private static final String DELETE = "DELETE FROM storage WHERE "
			+ "x = ? AND y = ? AND processor = ? AND region = ?";
	private static final String CORES =
			"SELECT DISTINCT x, y, processor FROM storage "
					+ "ORDER BY x, y, processor";
	private static final String REGIONS =
			"SELECT DISTINCT region FROM storage WHERE "
					+ "x = ? AND y = ? AND processor = ? ORDER BY region";

	private static final int FIRST = 1;
	private static final int SECOND = 2;
	private static final int THIRD = 3;
	private static final int FOURTH = 4;
	private static final int FIFTH = 5;

	/**
	 * Create an instance.
	 *
	 * @param connectionProvider
	 *            The connection provider that will be asked for how to talk SQL
	 *            to the database.
	 */
	public SQLiteStorage(ConnectionProvider connectionProvider) {
		this.connProvider = connectionProvider;
	}

	/**
	 * Stores some bytes in the database. The bytes represent the contents of a
	 * region of a particular SpiNNaker core.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @param contents
	 *            The contents to store.
	 * @return The row ID. (Not currently used elsewhere.)
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public int storeRegionContents(HasCoreLocation core, int region,
			byte[] contents) throws SQLException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s =
						conn.prepareStatement(STORE, RETURN_GENERATED_KEYS)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.setInt(FOURTH, region);
			s.setBytes(FIFTH, contents);
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				keys.next();
				return keys.getInt(FIRST);
			}
		}
	}

	/**
	 * Appends some bytes to some already in the database. The bytes represent
	 * the contents of a region of a particular SpiNNaker core.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @param contents
	 *            The contents to store.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public void appendRegionContents(HasCoreLocation core, int region,
			byte[] contents) throws SQLException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(APPEND)) {
			s.setBytes(FIRST, contents);
			s.setInt(SECOND, core.getX());
			s.setInt(THIRD, core.getY());
			s.setInt(FOURTH, core.getP());
			s.setInt(FIFTH, region);
			s.executeUpdate();
		}
	}

	/**
	 * Retrieves some bytes from the database. The bytes represent the contents
	 * of a region of a particular SpiNNaker core.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @return The region contents.
	 * @throws IllegalArgumentException
	 *             If there's no such saved region.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public byte[] getRegionContents(HasCoreLocation core, int region)
			throws SQLException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(FETCH)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.setInt(FOURTH, region);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getBytes(FIRST);
				}
				throw new IllegalArgumentException(
						"core " + core + " has no data for region " + region);
			}
		}
	}

	/**
	 * Removes some bytes from the database. The bytes represent the contents of
	 * a region of a particular SpiNNaker core.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public void deleteRegionContents(HasCoreLocation core, int region)
			throws SQLException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(DELETE)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.setInt(FOURTH, region);
			s.executeUpdate();
		}
	}

	/**
	 * Get a list of all cores that have data stored in the database.
	 * <i>Warning: this is a potentially expensive operation!</i>
	 *
	 * @return A list of cores for which something is stored.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public List<CoreLocation> getCores() throws SQLException {
		ArrayList<CoreLocation> result = new ArrayList<>();
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(CORES);
				ResultSet rs = s.executeQuery()) {
			while (rs.next()) {
				int x = rs.getInt(FIRST);
				int y = rs.getInt(SECOND);
				int p = rs.getInt(THIRD);
				result.add(new CoreLocation(x, y, p));
			}
		}
		return result;
	}

	/**
	 * Get a list of all regions for a particular core that have data stored in
	 * the database.
	 *
	 * @param core
	 *            The core that has the memory regions.
	 * @return A list of region IDs for which something is stored.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public List<Integer> getRegions(HasCoreLocation core) throws SQLException {
		ArrayList<Integer> result = new ArrayList<>();
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(REGIONS)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					int r = rs.getInt(FIRST);
					result.add(r);
				}
			}
		}
		return result;
	}
}
