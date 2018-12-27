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
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.ConnectionProvider;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;

/**
 * How to actually talk to an SQLite database.
 *
 * @author Donal Fellows
 */
public class SQLiteStorage extends SQLiteConnectionManager
		implements BufferManagerStorage, DSEStorage {
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

	@Override
	public List<Ethernet> listEthernetsToLoad() throws StorageException {
		return callR(SQLiteStorage::listEthernetsToLoad, "listing ethernets");
	}

	private static List<Ethernet> listEthernetsToLoad(Connection conn)
			throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(SQL.LIST_ETHERNETS);
				ResultSet rs = s.executeQuery()) {
			List<Ethernet> result = new ArrayList<>();
			while (rs.next()) {
				// ethernet_id, ethernet_x, ethernet_y, ip_address
				result.add(new EthernetImpl(rs.getInt(FIRST), rs.getInt(SECOND),
						rs.getInt(THIRD), rs.getString(FOURTH)));
			}
			return result;
		}
	}

	@Override
	public List<CoreToLoad> listCoresToLoad(Ethernet ethernet)
			throws StorageException {
		if (!(ethernet instanceof EthernetImpl)) {
			throw new IllegalArgumentException(
                "can only list cores for ethernets described by this class");
		}
		return callR(conn -> listCoresToLoad(conn, (EthernetImpl) ethernet),
				"listing cores to load data onto");
	}

	private static List<CoreToLoad> listCoresToLoad(Connection conn,
			EthernetImpl ethernet) throws SQLException {
		try (PreparedStatement s =
				conn.prepareStatement(SQL.LIST_CORES_TO_LOAD)) {
			// ethernet_id
			s.setInt(FIRST, ethernet.id);
			try (ResultSet rs = s.executeQuery()) {
				List<CoreToLoad> result = new ArrayList<>();
				while (rs.next()) {
					// core_id, x, y, processor, content
					result.add(new CoreToLoadImpl(rs.getInt(FIRST),
							rs.getInt(SECOND), rs.getInt(THIRD),
							rs.getInt(FOURTH), rs.getInt(FIFTH),
							rs.getBytes(SIXTH)));
				}
				return result;
			}
		}
	}

	@Override
	public void saveLoadingMetadata(CoreToLoad core, int startAddress,
			int memoryUsed, int memoryWritten) throws StorageException {
		if (!(core instanceof CoreToLoadImpl)) {
			throw new IllegalArgumentException(
					"can only save metadata for cores described by this class");
		}
		callV(conn -> saveLoadingMetadata(conn, (CoreToLoadImpl) core,
				startAddress, memoryUsed, memoryWritten),
				"saving data loading metadata");
	}

	private static void saveLoadingMetadata(Connection conn,
			CoreToLoadImpl core, int startAddress, int memoryUsed,
			int memoryWritten) throws SQLException {
		try (PreparedStatement s =
				conn.prepareStatement(SQL.ADD_LOADING_METADATA)) {
			s.setInt(FIRST, startAddress);
			s.setInt(SECOND, memoryUsed);
			s.setInt(THIRD, memoryWritten);
			s.setInt(FOURTH, core.id);
			s.executeUpdate();
		}
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
		try (PreparedStatement s = conn.prepareStatement(SQL.APPEND_CONTENT)) {
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

	private static final class EthernetImpl extends Ethernet {
		/** The primary key. */
		final int id;

		private EthernetImpl(int id, int etherx, int ethery, String addr) {
			super(etherx, ethery, addr);
			this.id = id;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof EthernetImpl)) {
				return false;
			}
			EthernetImpl b = (EthernetImpl) other;
			return id == b.id;
		}

		@Override
		public int hashCode() {
			return id ^ 444113;
		}
	}

	private static final class CoreToLoadImpl extends CoreToLoad {
		/** The primary key. */
		final int id;

		private CoreToLoadImpl(int id, int x, int y, int p, int appID,
				byte[] bytes) {
			super(x, y, p, appID, bytes);
			this.id = id;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof CoreToLoadImpl)) {
				return false;
			}
			CoreToLoadImpl c = (CoreToLoadImpl) other;
			return id == c.id;
		}

		@Override
		public int hashCode() {
			return id ^ 187043;
		}
	}
}
