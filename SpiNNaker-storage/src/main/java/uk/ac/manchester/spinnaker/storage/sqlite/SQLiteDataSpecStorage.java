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

import static java.nio.ByteBuffer.wrap;
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_APP_ID;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_REGION_POINTER_AND_CONTEXT;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_REGION_SIZES;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_START_ADDRESS;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.LIST_CORES_TO_LOAD;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.LIST_ETHERNETS;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.SET_REGION_POINTER;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.SET_START_ADDRESS;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.readOnly;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.RegionInfo;
import uk.ac.manchester.spinnaker.storage.StorageException;

/**
 * How to actually talk to an SQLite database.
 *
 * @author Donal Fellows
 */
public final class SQLiteDataSpecStorage extends SQLiteStorage<DSEStorage>
		implements DSEStorage {
	/**
	 * Create an instance.
	 *
	 * @param db
	 *            The database engine that will be asked for how to talk SQL to
	 *            the database.
	 */
	public SQLiteDataSpecStorage(DSEDatabaseEngine db) {
		super(db);
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
				result.add(new EthernetImpl(rs.getInt("ethernet_x"),
						rs.getInt("ethernet_y"), rs.getString("ip_address")));
			}
			return result;
		}
	}

	private static EthernetImpl sanitise(Ethernet ethernet) {
		if (ethernet instanceof EthernetImpl eth) {
			return eth;
		}
		throw new IllegalArgumentException("can only list cores"
				+ " for ethernets described by this class");
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
			// ethernet_x, ethernet_y, is_system
			setArguments(s, ethernet.location.getX(), ethernet.location.getY(),
					loadSystemCores);
			try (var rs = s.executeQuery()) {
				var result = new ArrayList<CoreLocation>();
				while (rs.next()) {
					result.add(new CoreLocation(rs.getInt("x"),
							rs.getInt("y"), rs.getInt("p")));
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
			// x, y, p
			setArguments(s, xyp.getX(), xyp.getY(), xyp.getP());
			try (var rs = s.executeQuery()) {
				var result = new LinkedHashMap<Integer, Integer>();
				while (rs.next()) {
					result.put(rs.getInt("region_num"), rs.getInt("size"));
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
		try (var s = conn.prepareStatement(GET_REGION_POINTER_AND_CONTEXT)) {
			// x, y, p
			setArguments(s, xyp.getX(), xyp.getY(), xyp.getP());
			try (var rs = s.executeQuery()) {
				var results = new HashMap<Integer, RegionInfo>();
				while (rs.next()) {
					results.put(rs.getInt("region_num"),
							new RegionInfo(
									wrapIfNotNull(rs.getBytes("content")),
									new MemoryLocation(rs.getInt("pointer"))));
				}
				return results;
			}
		}
	}

	private static ByteBuffer wrapIfNotNull(byte[] buffer) {
		if (nonNull(buffer)) {
			return readOnly(wrap(buffer));
		}
		return null;
	}

	@Override
	public void setStartAddress(CoreLocation xyp,
			MemoryLocation start) throws StorageException {
		callV(conn -> setStartAddress(conn, xyp, start),
				"saving data loading metadata");
	}

	private static void setStartAddress(Connection conn,
			CoreLocation xyp, MemoryLocation start) throws SQLException {
		try (var s = conn.prepareStatement(SET_START_ADDRESS)) {
			// start_address, x, y, p
			setArguments(s, start.address(), xyp.getX(), xyp.getY(),
					xyp.getP());
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
			// x, y, p
			setArguments(s, xyp.getX(), xyp.getY(), xyp.getP());
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return new MemoryLocation(rs.getInt("start_address"));
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
			// pointer, x, y, p, region_num
			setArguments(s, pointer, xyp.getX(), xyp.getY(), xyp.getP(),
					regionNum);
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
					return rs.getInt("app_id");
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
			return (other instanceof EthernetImpl b)
					&& (location == b.location);
		}

		@Override
		public int hashCode() {
			return location.hashCode();
		}
	}
}
