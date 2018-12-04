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

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * How to actually talk to an SQLite database.
 *
 * @author Donal Fellows
 */
public class SQLiteStorage extends SQLiteConnectionManager implements Storage {
	// Recording regions
	private static final String INSERT_LOCATION =
			"INSERT INTO core(x, y, processor) VALUES(?, ?, ?)";
	private static final String GET_LOCATION = "SELECT core_id FROM core"
			+ " WHERE x = ? AND y = ? AND processor = ? LIMIT 1";
	private static final String GET_REGION =
			"SELECT region_id FROM region WHERE "
					+ "core_id = ? AND local_region_index = ? LIMIT 1";
	private static final String INSERT_REGION =
			"INSERT INTO region(core_id, local_region_index, address) "
					+ "VALUES (?, ?, ?)";
	private static final String APPEND_CONTENT =
			"UPDATE region SET content = content || ?, fetches = fetches + 1,"
					+ " append_time = ? WHERE region_id = ?";
	private static final String FETCH_RECORDING =
			"SELECT content, fetches, append_time FROM region_view"
					+ " WHERE x = ? AND y = ? AND processor = ?"
					+ " AND local_region_index = ? LIMIT 1";
	private static final String GET_CORES_WITH_STORAGE =
			"SELECT DISTINCT x, y, processor FROM region_view"
					+ " ORDER BY x, y, processor";
	private static final String GET_REGIONS_WITH_STORAGE =
			"SELECT DISTINCT local_region_index FROM region_view"
					+ " WHERE x = ? AND y = ? AND processor = ?"
					+ " ORDER BY local_region_index";

	// Data loading
	private static final String LIST_BOARDS =
			"SELECT DISTINCT board_id, ethernet_x, ethernet_y, ethernet_address"
					+ " FROM core_view";
	private static final String LIST_CORES_TO_LOAD =
			"SELECT core_id, x, y, processor, content FROM core_view "
					+ "WHERE board_id = ?";
	private static final String ADD_LOADING_METADATA = "UPDATE core "
			+ "SET start_address = ?, memory_used = ?, memory_written = ? "
			+ "WHERE core_id = ?";

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
		super(connectionProvider);
	}

	/**
	 * A board which has data specifications loaded onto it.
	 *
	 * @author Donal Fellows
	 */
	public static class Board {
		private final int id;
		/**
		 * The virtual location of this board.
		 */
		public final ChipLocation ethernet;
		/**
		 * The network address of this board.
		 */
		public final String ethernetAddress;

		private Board(int id, int etherx, int ethery, String addr) {
			this.id = id;
			this.ethernet = new ChipLocation(etherx, ethery);
			this.ethernetAddress = addr;
		}

		@Override
		public boolean equals(Object other) {
			if (other == null || !(other instanceof Board)) {
				return false;
			}
			Board b = (Board) other;
			return id == b.id;
		}

		@Override
		public int hashCode() {
			return id;
		}
	}

	/**
	 * A core with a data specification to load.
	 *
	 * @author Donal Fellows
	 */
	public static class CoreToLoad implements HasCoreLocation {
		private final int id;
		private final int x, y, p;
		/**
		 * The data specification to execute for this core.
		 */
		public final byte[] dataSpec;

		private CoreToLoad(int id, int x, int y, int p, byte[] bytes) {
			this.id = id;
			this.x = x;
			this.y = y;
			this.p = p;
			this.dataSpec = bytes;
		}

		@Override
		public int getX() {
			return x;
		}

		@Override
		public int getY() {
			return y;
		}

		@Override
		public int getP() {
			return p;
		}

		@Override
		public boolean equals(Object other) {
			if (other == null || !(other instanceof CoreToLoad)) {
				return false;
			}
			CoreToLoad c = (CoreToLoad) other;
			return id == c.id;
		}

		@Override
		public int hashCode() {
			return id;
		}
	}

	public List<Board> listBoardsToLoad() throws StorageException {
		return this.callR(SQLiteStorage::listBoardsToLoad, "listing boards");
	}

	private static List<Board> listBoardsToLoad(Connection conn)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(LIST_BOARDS);
				ResultSet rs = s.executeQuery()) {
			List<Board> result = new ArrayList<>();
			while (rs.next()) {
				// board_id, ethernet_x, ethernet_y, ethernet_address
				result.add(new Board(rs.getInt(FIRST), rs.getInt(SECOND),
						rs.getInt(THIRD), rs.getString(FOURTH)));
			}
			return result;
		}
	}

	public List<CoreToLoad> listCoresToLoad(Board board)
			throws StorageException {
		return this.callR(conn -> listCoresToLoad(conn, board),
				"listing cores to load data onto");
	}

	private static List<CoreToLoad> listCoresToLoad(Connection conn,
			Board board) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(LIST_CORES_TO_LOAD)) {
			// board_id
			s.setInt(FIRST, board.id);
			try (ResultSet rs = s.executeQuery()) {
				List<CoreToLoad> result = new ArrayList<>();
				while (rs.next()) {
					// core_id, x, y, processor, content
					result.add(new CoreToLoad(rs.getInt(FIRST),
							rs.getInt(SECOND), rs.getInt(THIRD),
							rs.getInt(FOURTH), rs.getBytes(FIFTH)));
				}
				return result;
			}
		}
	}

	public void saveLoadingMetadata(CoreToLoad core, int startAddress,
			int memoryUsed, int memoryWritten) throws StorageException {
		callV(conn -> saveLoadingMetadata(conn, core, startAddress, memoryUsed,
				memoryWritten), "saving data loading metadata");
	}

	private static void saveLoadingMetadata(Connection conn, CoreToLoad core,
			int startAddress, int memoryUsed, int memoryWritten)
			throws SQLException {
		try (PreparedStatement s =
				conn.prepareStatement(ADD_LOADING_METADATA)) {
			s.setInt(FIRST, startAddress);
			s.setInt(SECOND, memoryUsed);
			s.setInt(THIRD, memoryWritten);
			s.setInt(FOURTH, core.id);
			s.executeUpdate();
		}
	}

	private static int getRecordingCore(Connection conn, CoreLocation core)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(GET_LOCATION)) {
			// x, y, processor
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(FIRST);
				}
			}
		}
		try (PreparedStatement s =
				conn.prepareStatement(INSERT_LOCATION, RETURN_GENERATED_KEYS)) {
			// x, y, processor
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				while (keys.next()) {
					return keys.getInt(FIRST);
				}
				throw new IllegalStateException(
						"could not make or find recording region core record");
			}
		}
	}

	private static int getRecordingRegion(Connection conn, int coreID,
			Region region) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(GET_REGION)) {
			// core_id, local_region_index
			s.setInt(FIRST, coreID);
			s.setInt(SECOND, region.regionIndex);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(FIRST);
				}
			}
		}
		try (PreparedStatement s =
				conn.prepareStatement(INSERT_REGION, RETURN_GENERATED_KEYS)) {
			// core_id, local_region_index, address
			s.setInt(FIRST, coreID);
			s.setInt(SECOND, region.regionIndex);
			s.setInt(THIRD, region.startAddress);
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				while (keys.next()) {
					return keys.getInt(FIRST);
				}
				throw new IllegalStateException(
						"could not make or find recording region record");
			}
		}
	}

	private void appendRecordingContents(Connection conn, int regionID,
			byte[] content) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(APPEND_CONTENT)) {
			// content, append_time, region_id
			s.setBinaryStream(FIRST, new ByteArrayInputStream(content),
					content.length);
			s.setLong(SECOND, System.currentTimeMillis());
			s.setInt(THIRD, regionID);
			s.executeUpdate();
		}
	}

	@Override
	public void appendRecordingContents(Region region, byte[] contents)
			throws StorageException {
		callV(conn -> appendRecordContents(conn, region, contents),
				"creating a region");
	}

	/**
	 * The core of how to append content to a recording.
	 *
	 * @param conn
	 *            The connection, with a transaction open.
	 * @param region
	 *            The recording region owning the recording.
	 * @param contents
	 *            The bytes to append.
	 * @throws SQLException
	 *             If anything goes wrong.
	 * @see #appendRecordingContents(Region,int,byte[])
	 */
	private void appendRecordContents(Connection conn, Region region,
			byte[] contents) throws SQLException {
		int coreID = getRecordingCore(conn, region.core);
		int regionID = getRecordingRegion(conn, coreID, region);
		appendRecordingContents(conn, regionID, contents);
	}

	@Override
	public byte[] getRecordingRegionContents(Region region)
			throws StorageException {
		return callR(conn -> getRecordingRegionContents(conn, region),
				"retrieving a recording region");
	}

	private static byte[] getRecordingRegionContents(Connection conn,
			Region region) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(FETCH_RECORDING)) {
			// x, y, processor, local_region_index
			s.setInt(FIRST, region.core.getX());
			s.setInt(SECOND, region.core.getY());
			s.setInt(THIRD, region.core.getP());
			s.setInt(FOURTH, region.regionIndex);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getBytes(FIRST);
				}
				throw new IllegalArgumentException("core " + region.core
						+ " has no data for region " + region.regionIndex);
			}
		}
	}

	@Override
	public List<CoreLocation> getCoresWithStorage() throws StorageException {
		return callR(conn -> {
			try (PreparedStatement s =
					conn.prepareStatement(GET_CORES_WITH_STORAGE);
					ResultSet rs = s.executeQuery()) {
				ArrayList<CoreLocation> result = new ArrayList<>();
				while (rs.next()) {
					int x = rs.getInt(FIRST);
					int y = rs.getInt(SECOND);
					int p = rs.getInt(THIRD);
					result.add(new CoreLocation(x, y, p));
				}
				return result;
			}
		}, "listing cores");
	}

	@Override
	public List<Integer> getRegionsWithStorage(HasCoreLocation core)
			throws StorageException {
		return callR(conn -> {
			try (PreparedStatement s =
					conn.prepareStatement(GET_REGIONS_WITH_STORAGE)) {
				s.setInt(FIRST, core.getX());
				s.setInt(SECOND, core.getY());
				s.setInt(THIRD, core.getP());
				try (ResultSet rs = s.executeQuery()) {
					ArrayList<Integer> result = new ArrayList<>();
					while (rs.next()) {
						result.add(rs.getInt(FIRST));
					}
					return result;
				}
			}
		}, "listing regions for a core");
	}
}
