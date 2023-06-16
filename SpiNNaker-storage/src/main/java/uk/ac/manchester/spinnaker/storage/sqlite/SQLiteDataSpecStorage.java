/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.storage.sqlite;

import java.nio.ByteBuffer;
import static java.nio.ByteBuffer.wrap;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;

import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.FIFTH;
import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.FIRST;
import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.FOURTH;
import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.SECOND;
import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.THIRD;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_APP_ID;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_REGION_POINTER_AND_CONTEXT;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_REGION_SIZES;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_START_ADDRESS;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.LIST_ETHERNETS;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.SET_REGION_POINTER;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.SET_START_ADDRESS;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.LIST_CORES_TO_LOAD;

import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.RegionInfo;
import uk.ac.manchester.spinnaker.storage.StorageException;

/**
 * How to actually talk to an SQLite database.
 *
 * @author Donal Fellows
 */
public class SQLiteDataSpecStorage extends SQLiteProxyStorage<DSEStorage>
		implements DSEStorage {

	/**
	 * Create an instance.
	 *
	 * @param connectionProvider
	 *            The connection provider that will be asked for how to talk SQL
	 *            to the database.
	 */
	public SQLiteDataSpecStorage(DSEDatabaseEngine connectionProvider) {
		super(connectionProvider);
	}

	@Override
	public int countCores(boolean loadSystemCores) throws StorageException {
		return callR(conn -> countCores(conn, loadSystemCores),
				"Counting cores");
	}

	private static int countCores(Connection conn, boolean loadSystemCores)
			throws SQLException {
		try (var s = conn.prepareStatement(LIST_CORES_TO_LOAD)) {
			// ethernet_id
			s.setBoolean(FIRST, loadSystemCores);
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					// count_content
					return rs.getInt(FIRST);
				}
			}
		}
		return 0; // If we get here, nothing to count
	}

	@Override
	public List<Ethernet> listEthernetsToLoad() throws StorageException {
		return callR(SQLiteDataSpecStorage::listEthernetsToLoad,
				"listing ethernets");
	}

	private static List<Ethernet> listEthernetsToLoad(Connection conn)
			throws SQLException {
		try (var s = conn.prepareStatement(LIST_ETHERNETS);
				var rs = s.executeQuery()) {
			var result = new ArrayList<Ethernet>();
			while (rs.next()) {
				// ethernet_id, ethernet_x, ethernet_y, ip_address
				result.add(new EthernetImpl(rs.getInt(FIRST), rs.getInt(SECOND),
						rs.getString(THIRD)));
			}
			return result;
		}
	}

	private static EthernetImpl sanitise(Ethernet ethernet) {
		if (!(ethernet instanceof EthernetImpl)) {
			throw new IllegalArgumentException("can only list cores"
					+ " for ethernets described by this class");
		}
		return (EthernetImpl) ethernet;
	}

	@Override
	public List<CoreLocation> listCoresToLoad(Ethernet ethernet,
			boolean loadSystemCores) throws StorageException {
		return callR(conn -> listCoresToLoad(conn, sanitise(ethernet),
				loadSystemCores), "listing cores to load data onto");
	}

	private List<CoreLocation> listCoresToLoad(Connection conn,
			EthernetImpl ethernet, boolean loadSystemCores)
			throws SQLException {
		try (var s = conn.prepareStatement(LIST_CORES_TO_LOAD)) {
			// ethernet_id
			s.setInt(FIRST, ethernet.location.getX());
			s.setInt(SECOND, ethernet.location.getY());
			s.setBoolean(THIRD, loadSystemCores);
			try (var rs = s.executeQuery()) {
				var result = new ArrayList<CoreLocation>();
				while (rs.next()) {
					// core_id, x, y, processor, content
					result.add(new CoreLocation(rs.getInt(FIRST),
							rs.getInt(SECOND), rs.getInt(THIRD)));
				}
				return result;
			}
		}
	}

	@Override
	public LinkedHashMap<Integer, Integer> getRegionSizes(CoreLocation core)
			throws StorageException {
		return callR(conn -> getRegionSizes(conn, core),
				"getting region sizes");
	}

	private LinkedHashMap<Integer, Integer> getRegionSizes(
			Connection conn, CoreLocation xyp) throws SQLException {
		try (var s = conn.prepareStatement(GET_REGION_SIZES)) {
			// ethernet_id
			s.setInt(FIRST, xyp.getX());
			s.setInt(SECOND, xyp.getY());
			s.setInt(THIRD, xyp.getP());
			try (var rs = s.executeQuery()) {
				var result = new LinkedHashMap<Integer, Integer>();
				while (rs.next()) {
					// core_id, x, y, processor, content
					result.put(rs.getInt(FIRST), rs.getInt(SECOND));
				}
				return result;
			}
		}
	}

	@Override
	public HashMap<Integer, RegionInfo> getRegionPointersAndContent(
			CoreLocation xyp) throws StorageException {
		return callR(
				conn -> getRegionPointersAndContent(conn, xyp),
				"reading data specification for region");
	}

	private static HashMap<Integer, RegionInfo> getRegionPointersAndContent(
			Connection conn, CoreLocation xyp) throws SQLException {
		HashMap<Integer, RegionInfo> results =
				new HashMap<Integer, RegionInfo>();
		try (var s = conn.prepareStatement(GET_REGION_POINTER_AND_CONTEXT)) {
			s.setInt(FIRST, xyp.getX());
			s.setInt(SECOND, xyp.getY());
			s.setInt(THIRD, xyp.getP());
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					ByteBuffer content = null;
					if (rs.getBytes(SECOND) != null) {
						content = wrap(rs.getBytes(SECOND)).asReadOnlyBuffer();
					}
					RegionInfo info = new RegionInfo(
							content, new MemoryLocation(rs.getInt(THIRD)));
					results.put(rs.getInt(FIRST), info);
				}
			}
			return results;
		}
	}

	@Override
	public void setStartAddress(CoreLocation xyp,
			MemoryLocation start) throws StorageException {
		callV(conn -> setStartAddres(conn, xyp, start),
				"saving data loading metadata");
	}

	private static void setStartAddres(Connection conn,
			CoreLocation xyp, MemoryLocation start) throws SQLException {
		try (var s = conn.prepareStatement(SET_START_ADDRESS)) {
			s.setInt(FIRST, start.address);
			s.setInt(SECOND, xyp.getX());
			s.setInt(THIRD, xyp.getY());
			s.setInt(FOURTH, xyp.getP());
			s.executeUpdate();
		}
	}

	@Override
	public MemoryLocation getStartAddress(CoreLocation xyp)
			throws StorageException {
		return callR(conn -> getStartAddress(conn, xyp),
				"getting start address");
	}

	private MemoryLocation getStartAddress(Connection conn, CoreLocation xyp)
			throws SQLException {
		try (var s = conn.prepareStatement(GET_START_ADDRESS)) {
			// ethernet_id
			s.setInt(FIRST, xyp.getX());
			s.setInt(SECOND, xyp.getY());
			s.setInt(THIRD, xyp.getP());
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					// core_id, x, y, processor, content
					return new MemoryLocation(rs.getInt(FIRST));
				}
			}
		}
		throw new IllegalStateException(
				"could not get_start_address for core "
						+  xyp.getX() + ":" + xyp.getY() + ":" + xyp.getP()
						+ " known to have one");

	}

	@Override
	public void setRegionPointer(CoreLocation xyp, int regionNum,
			int pointer) throws StorageException {
		callV(conn -> setRegionPointer(conn, xyp, regionNum, pointer),
				"saving data loading metadata");
	}

	private static void setRegionPointer(Connection conn,
			CoreLocation xyp, int regionNum, int pointer)
			throws SQLException {
		try (var s = conn.prepareStatement(SET_REGION_POINTER)) {
			s.setInt(FIRST, pointer);
			s.setInt(SECOND, xyp.getX());
			s.setInt(THIRD, xyp.getY());
			s.setInt(FOURTH, xyp.getP());
			s.setInt(FIFTH, regionNum);
			s.executeUpdate();
		}
	}

	@Override
	public int getAppId() throws StorageException {
		return callR(conn -> getAppId(conn), "getting app id");
	}

	private int getAppId(Connection conn)
			throws SQLException {
		try (var s = conn.prepareStatement(GET_APP_ID)) {
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(FIRST);
				}
			}
		}
		throw new IllegalStateException("could not ge app id");
	}

	private static final class EthernetImpl extends Ethernet {

		private EthernetImpl(int etherx, int ethery, String addr) {
			super(etherx, ethery, addr);
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof EthernetImpl)) {
				return false;
			}
			var b = (EthernetImpl) other;
			return location == b.location;
		}

		@Override
		public int hashCode() {
			return location.hashCode();
		}
	}

}
