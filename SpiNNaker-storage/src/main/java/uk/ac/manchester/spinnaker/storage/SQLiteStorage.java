/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

import java.io.ByteArrayInputStream;
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
public class SQLiteStorage extends SQLiteConnectionManager implements Storage {
	// Locations
	private static final String INSERT_LOCATION =
			"INSERT INTO core(x, y, processor) VALUES(?, ?, ?)";
	private static final String GET_LOCATION =
			"SELECT core_id FROM core"
					+ " WHERE x = ? AND y = ? AND processor = ? LIMIT 1";

	// DSE regions
	private static final String MAKE_DSE_RECORD =
			"INSERT OR REPLACE INTO dse (core_id, dse_index, address, size)"
					+ " VALUES (?, ?, ?, ?)";
	private static final String GET_DSE_RECORD_ID =
			"SELECT dse_id FROM dse WHERE core_id = ? AND dse_index = ?";
	private static final String FETCH_DSE =
			"SELECT content FROM dse_storage_view WHERE"
					+ " x = ? AND y = ? AND processor = ? AND dse_index = ?"
					+ " AND reset_counter = ? AND run_counter = ?";
	private static final String DELETE_DSE_REGION =
			"DELETE FROM dse WHERE dse_id IN ("
					+ "SELECT dse_id FROM dse_view WHERE"
					+ " x = ? AND y = ? AND processor = ? AND dse_index = ?)";
	private static final String CORES_WITH_DSE_STORAGE =
			"SELECT DISTINCT x, y, processor FROM dse_storage_view"
					+ " ORDER BY x, y, processor";
	private static final String DSE_REGIONS_WITH_STORAGE =
			"SELECT DISTINCT dse_index FROM dse_storage_view"
					+ " WHERE x = ? AND y = ? AND processor = ?"
					+ " ORDER BY dse_index";
	private static final String STORE_CONTENT =
			"INSERT OR REPLACE INTO dse_storage "
					+ "(dse_id, reset_counter, run_counter, content,"
					+ " creation_time) VALUES(?, ?, ?, ?, ?)";

	// Recording regions
	private static final String CREATE_VERTEX =
			"INSERT INTO vertex (meta_data_id, label) VALUES (?, ?)";
	private static final String CREATE_RECORDING =
			"INSERT INTO region (vertex_id, local_region_index, address)"
					+ " VALUES (?, ?, ?)";
	private static final String GET_RECORDING =
			"SELECT region_id FROM region_view"
					+ " WHERE meta_data_id = ? AND local_region_index = ?";
	private static final String GET_REC_STORAGE =
			"SELECT region_storage_id FROM region_storage"
					+ " WHERE region_id = ? AND reset_counter = ? LIMIT 1";
	private static final String REC_STORAGE_INIT =
			"INSERT INTO region_storage (region_id, reset_counter)"
					+ " VALUES (?, ?)";
	private static final String APPEND_CONTENT =
			"UPDATE region_storage SET content = content || ?,"
					+ " fetches = fetches + 1, append_time = ?"
					+ " WHERE region_storage_id = ?";
	private static final String FETCH_RECORDING =
			"SELECT content, region_storage_id FROM region_storage_view"
			+ " WHERE x = ? AND y = ? AND processor = ? AND dse_index = ?"
			+ " AND local_region_index = ? AND reset_counter = ? LIMIT 1";

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
		super(connectionProvider);
	}

	private static int getLocationID(Connection conn, HasCoreLocation core)
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

	private static int storeContent(Connection conn, int dseId,
			int resetCounter, int runCounter, byte[] content)
			throws SQLException {
		try (PreparedStatement s =
				conn.prepareStatement(STORE_CONTENT, RETURN_GENERATED_KEYS)) {
			// dse_id, reset_counter, run_counter, content, creation_time
			s.setInt(FIRST, dseId);
			s.setInt(SECOND, resetCounter);
			s.setInt(THIRD, runCounter);
			s.setBinaryStream(FOURTH, new ByteArrayInputStream(content),
					content.length);
			s.setLong(FIFTH, System.currentTimeMillis());
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				while (keys.next()) {
					return keys.getInt(FIRST);
				}
			}
		}
		throw new IllegalStateException("could not store blob");
	}

	private static int makeDSErecord(Connection conn, int coreID, int dseIndex,
			int address, int size)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(GET_DSE_RECORD_ID)) {
			s.setInt(FIRST, coreID);
			s.setInt(SECOND, dseIndex);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(FIRST);
				}
			}
		}
		try (PreparedStatement s =
				conn.prepareStatement(MAKE_DSE_RECORD, RETURN_GENERATED_KEYS)) {
			s.setInt(FIRST, coreID);
			s.setInt(SECOND, dseIndex);
			s.setInt(THIRD, address);
			s.setInt(FOURTH, size);
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				while (keys.next()) {
					return keys.getInt(FIRST);
				}
			}
		}
		throw new IllegalStateException("could not make DSE region record");
	}

	private static int getRun(Connection conn) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(
				"SELECT current_run_counter FROM global_setup LIMIT 1")) {
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(FIRST);
				}
			}
		}
		return 1; // Default
	}

	private static int getReset(Connection conn) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(
				"SELECT current_reset_counter FROM global_setup LIMIT 1")) {
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(FIRST);
				}
			}
		}
		return 1; // Default
	}

	@Override
	public int storeDSEContents(Region region, byte[] contents)
			throws StorageException {
		return callR(conn -> storeDSEContents(conn, region, contents),
				"creating a stored copy of a DSE region");
	}

	private int storeDSEContents(Connection conn, Region region,
			byte[] contents) throws SQLException {
		int reset = getReset(conn);
		int run = getRun(conn);
		int locID = getLocationID(conn, region.core);
		int dseID = makeDSErecord(conn, locID, region.regionIndex,
				region.startAddress, region.size);
		return storeContent(conn, dseID, reset, run, contents);
	}

	private static int createRecordingRegion(Connection conn, int dseID,
			int recordingRegionIndex, int address) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(GET_RECORDING)) {
			// dse_id, local_region_index
			s.setInt(FIRST, dseID);
			s.setInt(SECOND, recordingRegionIndex);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(FIRST);
				}
			}
		}
		Integer vertex_id = null;
		try (PreparedStatement s = conn.prepareStatement(
				"SELECT vertex_id FROM vertex WHERE meta_data_id = ?")) {
			s.setInt(FIRST, dseID);
			try (ResultSet resultSet = s.executeQuery()) {
				while (resultSet.next()) {
					vertex_id = resultSet.getInt(FIRST);
					break;
				}
			}
		}
		if (vertex_id == null) {
			throw new IllegalStateException(
					"could not find vertex for recording region");
		}
		try (PreparedStatement s = conn.prepareStatement(CREATE_RECORDING,
				RETURN_GENERATED_KEYS)) {
			// vertex_id, local_region_index, address
			s.setInt(FIRST, vertex_id);
			s.setInt(SECOND, recordingRegionIndex);
			s.setInt(THIRD, address);
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

	private static Integer getRecordingStorage(Connection conn, int recID, int reset)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(GET_REC_STORAGE)) {
			s.setInt(FIRST, recID);
			s.setInt(SECOND, reset);
			try (ResultSet resultSet = s.executeQuery()) {
				while (resultSet.next()) {
					return resultSet.getInt(FIRST);
				}
			}
		}
		return null;
	}

	private static int recordingInit(Connection conn, int regionID, int reset)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(REC_STORAGE_INIT,
				RETURN_GENERATED_KEYS)) {
			// region_id, reset_counter
			s.setInt(FIRST, regionID);
			s.setInt(SECOND, reset);
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				while (keys.next()) {
					return keys.getInt(FIRST);
				}
			}
		}
		throw new IllegalStateException(
				"cound not create region storage record");
	}

	private static void appendContent(Connection conn, int storageId,
			byte[] content) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(APPEND_CONTENT)) {
			// content, append_time, region_storage_id
			s.setBinaryStream(FIRST, new ByteArrayInputStream(content),
					content.length);
			s.setLong(SECOND, System.currentTimeMillis());
			s.setInt(THIRD, storageId);
			s.executeUpdate();
		}
	}

	@Override
	public void appendRecordingContents(Region region, int recordingID,
			byte[] contents) throws StorageException {
		callV(conn -> appendRecordContents(conn, region, recordingID, contents),
				"creating a region");
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
		int reset = getReset(conn);
		int coreID = getLocationID(conn, region.core);
		int dseID = makeDSErecord(conn, coreID, region.regionIndex,
				region.startAddress, region.size);
		int recID = createRecordingRegion(conn, dseID, recordingID,
				region.startAddress);
		Integer existingStorage = getRecordingStorage(conn, recID, reset);
		if (existingStorage == null) {
			existingStorage = recordingInit(conn, recID, reset);
		}
		appendContent(conn, existingStorage, contents);
	}

	@Override
	public int noteRecordingVertex(Region region, String label)
			throws StorageException {
		return callR(conn -> noteRecordingVertex(conn, region, label),
				"creating a vertex with recording");
	}

	private int noteRecordingVertex(Connection conn, Region region,
			String label) throws SQLException {
		int coreID = getLocationID(conn, region.core);
		int dseID = makeDSErecord(conn, coreID, region.regionIndex,
				region.startAddress, region.size);
		try (PreparedStatement s =
				conn.prepareStatement(CREATE_VERTEX, RETURN_GENERATED_KEYS)) {
			s.setInt(FIRST, dseID);
			s.setString(SECOND, label);
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				while (keys.next()) {
					return keys.getInt(FIRST);
				}
			}
		}
		throw new IllegalStateException("cound not create vertex record");
	}

	@Override
	public byte[] getRegionContents(Region region, Integer reset, Integer run)
			throws StorageException {
		return callR(conn -> getRegionContents(conn, region, reset, run),
				"retrieving a region");
	}

	private static byte[] getRegionContents(Connection conn, Region region,
			Integer reset, Integer run) throws SQLException {
		if (run == null) {
			run = getRun(conn);
		}
		if (reset == null) {
			reset = getReset(conn);
		}
		try (PreparedStatement s = conn.prepareStatement(FETCH_DSE)) {
			// x, y, processor, dse_index, reset_counter, run_counter
			s.setInt(FIRST, region.core.getX());
			s.setInt(SECOND, region.core.getY());
			s.setInt(THIRD, region.core.getP());
			s.setInt(FOURTH, region.regionIndex);
			s.setInt(FIFTH, reset);
			s.setInt(SIXTH, run);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getBytes(FIRST);
				}
			}
		}
		throw new IllegalArgumentException("core " + region.core
				+ " has no data for region " + region.regionIndex);
	}

	@Override
	public byte[] getRecordingRegionContents(Region region, int recordingIndex,
			Integer run) throws StorageException {
		return callR(conn -> getRecordingRegionContents(conn, region,
				recordingIndex, run), "retrieving a recording region");
	}

	private static byte[] getRecordingRegionContents(Connection conn,
			Region region, int recordingIndex, Integer run)
			throws SQLException {
		if (run == null) {
			run = getRun(conn);
		}
		try (PreparedStatement s = conn.prepareStatement(FETCH_RECORDING)) {
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
		}
	}

	@Override
	public void deleteRegionContents(HasCoreLocation core, int region,
			Integer run) throws StorageException {
		callV(conn -> deleteRegionContents(conn, core, region, run),
				"deleting a region");
	}

	private static void deleteRegionContents(Connection conn,
			HasCoreLocation core, int region, Integer run) throws SQLException {
		if (run == null) {
			run = getRun(conn);
		}
		try (PreparedStatement s = conn.prepareStatement(DELETE_DSE_REGION)) {
			// x, y, processor, dse_index
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.setInt(FOURTH, region);
			s.executeUpdate();
		}
	}

	@Override
	public List<CoreLocation> getCoresWithStorage() throws StorageException {
		return callR(conn -> {
			ArrayList<CoreLocation> result = new ArrayList<>();
			try (PreparedStatement s =
					conn.prepareStatement(CORES_WITH_DSE_STORAGE);
					ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					int x = rs.getInt(FIRST);
					int y = rs.getInt(SECOND);
					int p = rs.getInt(THIRD);
					result.add(new CoreLocation(x, y, p));
				}
			}
			return result;
		}, "listing cores");
	}

	@Override
	public List<Integer> getRegionsWithStorage(HasCoreLocation core)
			throws StorageException {
		return callR(conn -> {
			try (PreparedStatement s =
					conn.prepareStatement(DSE_REGIONS_WITH_STORAGE)) {
				s.setInt(FIRST, core.getX());
				s.setInt(SECOND, core.getY());
				s.setInt(THIRD, core.getP());
				ArrayList<Integer> result = new ArrayList<>();
				try (ResultSet rs = s.executeQuery()) {
					while (rs.next()) {
						result.add(rs.getInt(FIRST));
					}
				}
				return result;
			}
		}, "listing regions for a core");
	}
}
