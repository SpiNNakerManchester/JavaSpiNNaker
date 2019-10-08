/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage.sqlite;

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
import uk.ac.manchester.spinnaker.storage.BufferManagerDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;

/**
 * How to actually talk to an SQLite database.
 *
 * @author Donal Fellows
 */
public class SQLiteBufferStorage
		extends SQLiteConnectionManager<BufferManagerStorage>
		implements BufferManagerStorage {
	private static final int FIRST = 1;
	private static final int SECOND = 2;
	private static final int THIRD = 3;
	private static final int FOURTH = 4;

	/**
	 * Create an instance.
	 *
	 * @param connectionProvider
	 *            The connection provider that will be asked for how to talk SQL
	 *            to the database.
	 */
	public SQLiteBufferStorage(BufferManagerDatabaseEngine connectionProvider) {
		super(connectionProvider);
	}

	private static int getRecordingCore(Connection conn, CoreLocation core)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(SQL.GET_LOCATION)) {
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
		try (PreparedStatement s = conn.prepareStatement(SQL.INSERT_LOCATION,
				RETURN_GENERATED_KEYS)) {
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
		try (PreparedStatement s = conn.prepareStatement(SQL.GET_REGION)) {
			// core_id, local_region_index
			s.setInt(FIRST, coreID);
			s.setInt(SECOND, region.regionIndex);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(FIRST);
				}
			}
		}
		try (PreparedStatement s = conn.prepareStatement(SQL.INSERT_REGION,
				RETURN_GENERATED_KEYS)) {
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
		int chunkLen = content.length;
		ByteArrayInputStream chunk = new ByteArrayInputStream(content);
		long timestamp = System.currentTimeMillis();
		if (useMainTable(conn, regionID, chunkLen)) {
			addContentToMainRow(conn, regionID, chunkLen, chunk, timestamp);
		} else {
			prepareExtraContent(conn, regionID, timestamp);
			addExtraContentRow(conn, regionID, chunkLen, chunk);
		}
	}

	private void addContentToMainRow(Connection conn, int regionID,
			int chunkLen, ByteArrayInputStream chunk, long timestamp)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(SQL.ADD_CONTENT)) {
			// content, append_time, region_id
			s.setBinaryStream(FIRST, chunk, chunkLen);
			s.setLong(SECOND, timestamp);
			s.setInt(THIRD, regionID);
			s.executeUpdate();
		}
	}

	private void prepareExtraContent(Connection conn, int regionID,
			long timestamp) throws SQLException {
		try (PreparedStatement s =
				conn.prepareStatement(SQL.PREP_EXTRA_CONTENT)) {
			// append_time, region_id
			s.setLong(FIRST, timestamp);
			s.setInt(SECOND, regionID);
			s.executeUpdate();
		}
	}

	private void addExtraContentRow(Connection conn, int regionID, int chunkLen,
			ByteArrayInputStream chunk) throws SQLException {
		try (PreparedStatement s =
				conn.prepareStatement(SQL.ADD_EXTRA_CONTENT)) {
			// region_id, content
			s.setInt(FIRST, regionID);
			s.setBinaryStream(SECOND, chunk, chunkLen);
			s.executeUpdate();
		}
	}

	private boolean useMainTable(Connection conn, int regionID, int chunkLen)
			throws SQLException {
		try (PreparedStatement s =
				conn.prepareStatement(SQL.GET_MAIN_CONTENT_EXISTS)) {
			s.setInt(FIRST, regionID);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(FIRST) == 0;
				}
			}
		}
		return false;
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
		// TODO: Make this work with the multi-region system
		try (PreparedStatement s = conn.prepareStatement(SQL.FETCH_RECORDING)) {
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
					conn.prepareStatement(SQL.GET_CORES_WITH_STORAGE);
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
					conn.prepareStatement(SQL.GET_REGIONS_WITH_STORAGE)) {
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
