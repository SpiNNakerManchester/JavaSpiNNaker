package uk.ac.manchester.spinnaker.storage;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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

	// Locations
	private static final String INSERT_LOCATION =
			"INSERT INTO locations(x, y, processor) VALUES(?, ?, ?)";
	private static final String GET_LOCATION =
			"SELECT global_location_id FROM locations "
					+ "WHERE x = ? AND y = ? AND processor = ? LIMIT 1";

	// Basic storage
	private static final String STORE_CONTENT =
			"INSERT INTO storage(content, creation_time) VALUES(?, ?)";
	private static final String APPEND_CONTENT =
			"UPDATE storage SET content = content || ? "
					+ "WHERE storage_id = ?";

	// DSE regions
	private static final String MAKE_DSE_RECORD =
			"INSERT OR REPLACE INTO dse_regions "
					+ "(global_location_id, dse_index, address, size, "
					+ "storage_id, run) VALUES (?, ?, ?, ?, ?, ?)";
	private static final String GET_DSE_RECORD_ID =
			"SELECT dse_id FROM dse_regions WHERE "
					+ "global_location_id = ? AND dse_index = ? AND run = ? "
					+ "LIMIT 1";
	private static final String UPDATE_DSE_STORAGE =
			"UPDATE dse_regions SET storage_id = ? WHERE dse_id = ?";
	private static final String FETCH_DSE =
			"SELECT content FROM storage NATURAL JOIN dse_view WHERE "
					+ "x = ? AND y = ? AND processor = ? AND dse_index = ? "
					+ "AND run = ? LIMIT 1";
	private static final String DELETE_DSE_REGION =
			"DELETE FROM dse_regions WHERE dse_id IN ("
					+ "SELECT dse_id FROM dse_view WHERE "
					+ "x = ? AND y = ? AND processor = ? AND dse_index = ? "
					+ "AND run = ?)";
	private static final String CORES_WITH_DSE_STORAGE =
			"SELECT DISTINCT x, y, processor FROM dse_view "
					+ "WHERE storage_id IS NOT NULL "
					+ "ORDER BY x, y, processor";
	private static final String DSE_REGIONS_WITH_STORAGE =
			"SELECT DISTINCT dse_index FROM storage NATURAL JOIN dse_view "
					+ "WHERE x = ? AND y = ? AND processor = ? "
					+ "AND storage_id IS NOT NULL ORDER BY dse_index";

	// Recording regions
	private static final String CREATE_RECORDING =
			"INSERT INTO recording_regions(dse_id, local_region_id)"
					+ " VALUES (?, ?)";
	private static final String GET_RECORDING =
			"SELECT recording_region_id FROM recording_regions "
					+ "WHERE dse_id = ? AND local_region_id = ?";
	private static final String GET_REC_STORAGE =
			"SELECT storage_id FROM recording_regions "
					+ "WHERE recording_region_id = ? LIMIT 1";
	private static final String REC_CHUNK_ONE =
			"UPDATE recording_regions SET storage_id = ?, fetches = 1 "
					+ "WHERE recording_region_id = ?";
	private static final String INCR_FETCHES =
			"UPDATE recording_regions SET fetches = fetches + 1 "
					+ "WHERE recording_region_id = ?";
	private static final String FETCH_RECORDING =
			"SELECT content, storage_id "
					+ "FROM storage NATURAL JOIN recording_view WHERE "
					+ "x = ? AND y = ? AND processor = ? AND dse_index = ? "
					+ "AND local_region_id = ? AND run = ? LIMIT 1";

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

	static int getLocationID(Connection conn, HasCoreLocation core)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(GET_LOCATION)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			try (ResultSet keys = s.executeQuery()) {
				while (keys.next()) {
					return keys.getInt(FIRST);
				}
			}
		}
		try (PreparedStatement s =
				conn.prepareStatement(INSERT_LOCATION, RETURN_GENERATED_KEYS)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				while (keys.next()) {
					return keys.getInt(FIRST);
				}
			}
		}
		throw new IllegalStateException(
				"could neither create nor find location ID");
	}

	private static int storeContent(Connection conn, byte[] content)
			throws SQLException {
		try (PreparedStatement s =
				conn.prepareStatement(STORE_CONTENT, RETURN_GENERATED_KEYS)) {
			s.setBytes(FIRST, content);
			s.setLong(SECOND, System.currentTimeMillis());
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				while (keys.next()) {
					return keys.getInt(FIRST);
				}
			}
		}
		throw new IllegalStateException("could not store blob");
	}

	private static void appendContent(Connection conn, int storageId,
			byte[] content) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(APPEND_CONTENT)) {
			s.setBytes(FIRST, content);
			s.setInt(SECOND, storageId);
			s.executeUpdate();
		}
	}

	private static int makeDSErecord(Connection conn, int locID, int regionID,
			int address, int size, Integer storageID, int run)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(GET_DSE_RECORD_ID)) {
			s.setInt(FIRST, locID);
			s.setInt(SECOND, regionID);
			s.setInt(THIRD, run);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(FIRST);
				}
			}
		}
		try (PreparedStatement s =
				conn.prepareStatement(MAKE_DSE_RECORD, RETURN_GENERATED_KEYS)) {
			s.setInt(FIRST, locID);
			s.setInt(SECOND, regionID);
			s.setInt(THIRD, address);
			s.setInt(FOURTH, size);
			if (storageID != null) {
				s.setInt(FIFTH, storageID);
			} else {
				s.setNull(FIFTH, Types.INTEGER);
			}
			s.setInt(SIXTH, run);
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				while (keys.next()) {
					return keys.getInt(FIRST);
				}
			}
		}
		throw new IllegalStateException("could not make DSE region record");
	}

	private static void updateStorage(Connection conn, int dseID, int storID)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(UPDATE_DSE_STORAGE)) {
			s.setInt(FIRST, storID);
			s.setInt(SECOND, dseID);
			s.executeUpdate();
		}
	}

	private static int getRun(Connection conn) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(
				"SELECT current_run FROM global_setup LIMIT 1")) {
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(FIRST);
				}
			}
		}
		return 1; // Default
	}

	@Override
	public int storeRegionContents(Region region, byte[] contents)
			throws StorageException {
		try (Connection conn = connProvider.getConnection()) {
			conn.setAutoCommit(false);
			try {
				int recID = storeRegionContents(conn, region, contents);
				conn.commit();
				return recID;
			} catch (Exception e) {
				conn.rollback();
				throw e;
			}
		} catch (SQLException e) {
			throw new StorageException("while creating a region", e);
		}
	}

	private int storeRegionContents(Connection conn, Region region,
			byte[] contents) throws SQLException {
		int run = getRun(conn);
		int locID = getLocationID(conn, region.core);
		int storID = storeContent(conn, contents);
		int dseID = makeDSErecord(conn, locID, region.regionIndex,
				region.startAddress, region.size, null, run);
		updateStorage(conn, dseID, storID);
		return dseID;
	}

	private static int createRecordingRegion(Connection conn, int dseRegionID,
			int recordingRegionID) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(GET_RECORDING)) {
			s.setInt(FIRST, dseRegionID);
			s.setInt(SECOND, recordingRegionID);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(FIRST);
				}
			}
		}
		try (PreparedStatement s = conn.prepareStatement(CREATE_RECORDING,
				RETURN_GENERATED_KEYS)) {
			s.setInt(FIRST, dseRegionID);
			s.setInt(SECOND, recordingRegionID);
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				while (keys.next()) {
					return keys.getInt(FIRST);
				}
			}
		}
		throw new IllegalStateException(
				"could not make or find recording region record");
	}

	private static Integer getRecordingStorage(Connection conn, int recID)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(GET_REC_STORAGE)) {
			s.setInt(FIRST, recID);
			try (ResultSet resultSet = s.executeQuery()) {
				while (resultSet.next()) {
					Integer storID = resultSet.getInt(FIRST);
					if (resultSet.wasNull()) {
						storID = null;
					}
					return storID;
				}
			}
		}
		throw new IllegalStateException(
				"could not find recording region record");
	}

	private static void recordFirstChunk(Connection conn, int recID, int storID)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(REC_CHUNK_ONE)) {
			s.setInt(FIRST, storID);
			s.setInt(SECOND, recID);
			s.executeUpdate();
		}
	}

	private static void incrementFetches(Connection conn, int recID)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(INCR_FETCHES)) {
			s.setInt(FIRST, recID);
			s.executeUpdate();
		}
	}

	@Override
	public void appendRecordingContents(Region region, int recordingID,
			byte[] contents) throws StorageException {
		try (Connection conn = connProvider.getConnection()) {
			conn.setAutoCommit(false);
			try {
				appendRecordContents(conn, region, recordingID, contents);
				conn.commit();
			} catch (Exception e) {
				conn.rollback();
				throw e;
			}
		} catch (SQLException e) {
			throw new StorageException("while creating a region", e);
		}
	}

	/**
	 * The core of how to append content to a recording.
	 *
	 * @param conn
	 *            The connection, with a transaction open.
	 * @param region
	 *            The DSE region owning the recording.
	 * @param recordingID
	 *            The ID of this recording.
	 * @param contents
	 *            The bytes to append.
	 * @throws SQLException
	 *             If anything goes wrong.
	 * @see #appendRecordingContents(Region,int,byte[])
	 */
	private void appendRecordContents(Connection conn, Region region,
			int recordingID, byte[] contents) throws SQLException {
		int run = getRun(conn);
		int locID = getLocationID(conn, region.core);
		int dseID = makeDSErecord(conn, locID, region.regionIndex,
				region.startAddress, region.size, null, run);
		int recID = createRecordingRegion(conn, dseID, recordingID);
		Integer existingStorage = getRecordingStorage(conn, recID);
		if (existingStorage == null) {
			int storID = storeContent(conn, contents);
			recordFirstChunk(conn, recID, storID);
		} else {
			appendContent(conn, existingStorage, contents);
			incrementFetches(conn, recID);
		}
	}

	@Override
	public byte[] getRegionContents(Region region, Integer run)
			throws StorageException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(FETCH_DSE)) {
			if (run == null) {
				run = getRun(conn);
			}
			s.setInt(FIRST, region.core.getX());
			s.setInt(SECOND, region.core.getY());
			s.setInt(THIRD, region.core.getP());
			s.setInt(FOURTH, region.regionIndex);
			s.setInt(FIFTH, run);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getBytes(FIRST);
				}
			}
			throw new IllegalArgumentException("core " + region.core
					+ " has no data for region " + region.regionIndex);
		} catch (SQLException e) {
			throw new StorageException("while retrieving a region", e);
		}
	}

	@Override
	public byte[] getRecordingRegionContents(Region region, int recordingIndex,
			Integer run) throws StorageException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(FETCH_RECORDING)) {
			if (run == null) {
				run = getRun(conn);
			}
			s.setInt(FIRST, region.core.getX());
			s.setInt(SECOND, region.core.getY());
			s.setInt(THIRD, region.core.getP());
			s.setInt(FOURTH, region.regionIndex);
			s.setInt(FIFTH, recordingIndex);
			s.setInt(SIXTH, run);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getBytes(FIRST);
				}
			}
			throw new IllegalArgumentException("core " + region.core
					+ " has no data for region " + region.regionIndex);
		} catch (SQLException e) {
			throw new StorageException("while retrieving a region", e);
		}
	}

	@Override
	public void deleteRegionContents(HasCoreLocation core, int region, Integer run)
			throws StorageException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s =
						conn.prepareStatement(DELETE_DSE_REGION)) {
			if (run == null) {
				run = getRun(conn);
			}
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.setInt(FOURTH, region);
			s.setInt(FIFTH, run);
			s.executeUpdate();
		} catch (SQLException e) {
			throw new StorageException("while deleting a region", e);
		}
	}

	@Override
	public List<CoreLocation> getCoresWithStorage() throws StorageException {
		ArrayList<CoreLocation> result = new ArrayList<>();
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s =
						conn.prepareStatement(CORES_WITH_DSE_STORAGE);
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
				PreparedStatement s =
						conn.prepareStatement(DSE_REGIONS_WITH_STORAGE)) {
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
}
