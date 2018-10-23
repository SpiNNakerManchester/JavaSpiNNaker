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
public class SQLiteStorage implements Storage {
	private final ConnectionProvider connProvider;

	private static final String STORE =
			"INSERT OR REPLACE INTO storage(x, y, processor, region, content) "
					+ "VALUES(?, ?, ?, ?, ?)";
	private static final String APPEND =
			"INSERT INTO storage(x, y, processor, region, content) "
					+ "VALUES(?, ?, ?, ?, ?) "
					+ "ON CONFLICT(x, y, processor, region) DO UPDATE "
					+ "SET content = storage.content || excluded.content";
	private static final String FETCH = "SELECT content FROM storage WHERE "
			+ "x = ? AND y = ? AND processor = ? AND region = ? LIMIT 1";
	private static final String DELETE = "DELETE FROM storage WHERE "
			+ "x = ? AND y = ? AND processor = ? AND region = ?";
	private static final String CORES =
			"SELECT DISTINCT x, y, processor FROM storage "
					+ "ORDER BY x, y, processor";
	private static final String REGIONS =
			"SELECT DISTINCT region FROM storage WHERE "
					+ "x = ? AND y = ? AND processor = ? ORDER BY region";

	private static final String REMEMBER_REGION_DEF =
			"INSERT INTO regions(x, y, processor, region, address, size) "
					+ "VALUES(?, ?, ?, ?, ?, ?)";
	private static final String FETCH_REGION_DEF =
			"SELECT address, sizee FROM regions WHERE "
					+ "x = ? AND y = ? AND processor = ? AND region = ? "
					+ "LIMIT 1";

	private static final int FIRST = 1;
	private static final int SECOND = 2;
	private static final int THIRD = 3;
	private static final int FOURTH = 4;
	private static final int FIFTH = 5;
	private static final int SIXTH = 6;

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

	@Override
	public int storeRegionContents(HasCoreLocation core, int region,
			byte[] contents) throws StorageException {
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
		} catch (SQLException e) {
			throw new StorageException("while creating a region", e);
		}
	}

	@Override
	public void appendRegionContents(HasCoreLocation core, int region,
			byte[] contents) throws StorageException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(APPEND)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.setInt(FOURTH, region);
			s.setBytes(FIFTH, contents);
			s.executeUpdate();
		} catch (SQLException e) {
			throw new StorageException("while appending to a region", e);
		}
	}

	@Override
	public byte[] getRegionContents(HasCoreLocation core, int region)
			throws StorageException {
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
		} catch (SQLException e) {
			throw new StorageException("while retrieving a region", e);
		}
	}

	@Override
	public void deleteRegionContents(HasCoreLocation core, int region)
			throws StorageException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(DELETE)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.setInt(FOURTH, region);
			s.executeUpdate();
		} catch (SQLException e) {
			throw new StorageException("while deleting a region", e);
		}
	}

	@Override
	public List<CoreLocation> getCoresWithStorage() throws StorageException {
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
		} catch (SQLException e) {
			throw new StorageException("while listing cores", e);
		}
		return result;
	}

	@Override
	public List<Integer> getRegionsWithStorage(HasCoreLocation core)
			throws StorageException {
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
		} catch (SQLException e) {
			throw new StorageException("while listing regions for a core", e);
		}
		return result;
	}

	private void storeLocations(CoreLocation core,
			List<RegionDescriptor> regions, Connection conn)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(REMEMBER_REGION_DEF)) {
			int rid = 0;
			for (RegionDescriptor d : regions) {
				s.setInt(FIRST, core.getX());
				s.setInt(SECOND, core.getY());
				s.setInt(THIRD, core.getP());
				s.setInt(FOURTH, rid++);
				s.setInt(FIFTH, d.baseAddress);
				s.setInt(SIXTH, d.size);
				s.executeUpdate();
			}
		}
	}

	@Override
	public void rememberLocations(CoreLocation core,
			List<RegionDescriptor> regions) throws StorageException {
		try (Connection conn = connProvider.getConnection();) {
			conn.setAutoCommit(false);
			try {
				storeLocations(core, regions, conn);
				conn.commit();
			} catch (Throwable t) {
				conn.rollback();
				throw t;
			}
		} catch (SQLException e) {
			throw new StorageException(
					"while remembering a region's definition", e);
		}
	}

	@Override
	public RegionDescriptor getRegionLocation(CoreLocation core, int region)
			throws StorageException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(FETCH_REGION_DEF)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.setInt(FOURTH, region);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return new RegionDescriptor(rs.getInt(FIRST),
							rs.getInt(SECOND));
				}
			}
		} catch (SQLException e) {
			throw new StorageException("while retrieving a region", e);
		}
		return null;
	}
}
