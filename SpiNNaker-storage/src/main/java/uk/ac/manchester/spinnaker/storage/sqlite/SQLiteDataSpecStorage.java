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
import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.FIFTH;
import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.FIRST;
import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.FOURTH;
import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.SECOND;
import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.SIXTH;
import static uk.ac.manchester.spinnaker.storage.sqlite.Ordinals.THIRD;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.ADD_LOADING_METADATA;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.COUNT_WORK;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_CORE_DATA_SPEC;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.LIST_CORES_TO_LOAD;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.LIST_CORES_TO_LOAD_FILTERED;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.LIST_ETHERNETS;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
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
	public int countWorkRequired() throws StorageException {
		return callR(SQLiteDataSpecStorage::countWorkRequired,
				"listing ethernets");
	}

	private static int countWorkRequired(Connection conn) throws SQLException {
		try (var s = conn.prepareStatement(COUNT_WORK);
				var rs = s.executeQuery()) {
			while (rs.next()) {
				// count_content
				return rs.getInt(FIRST);
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
						rs.getInt(THIRD), rs.getString(FOURTH)));
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

	private static CoreToLoadImpl sanitise(CoreToLoad core, String desc) {
		if (!(core instanceof CoreToLoadImpl)) {
			throw new IllegalArgumentException(
					"can only " + desc + " for cores described by this class");
		}
		return (CoreToLoadImpl) core;
	}

	@Override
	public List<CoreToLoad> listCoresToLoad(Ethernet ethernet)
			throws StorageException {
		return callR(conn -> listCoresToLoad(conn, sanitise(ethernet)),
				"listing cores to load data onto");
	}

	private List<CoreToLoad> listCoresToLoad(Connection conn,
			EthernetImpl ethernet) throws SQLException {
		try (var s = conn.prepareStatement(LIST_CORES_TO_LOAD)) {
			// ethernet_id
			s.setInt(FIRST, ethernet.id);
			try (var rs = s.executeQuery()) {
				var result = new ArrayList<CoreToLoad>();
				while (rs.next()) {
					// core_id, x, y, processor, content
					result.add(new CoreToLoadImpl(rs.getInt(FIRST),
							rs.getInt(SECOND), rs.getInt(THIRD),
							rs.getInt(FOURTH), rs.getInt(FIFTH),
							rs.getInt(SIXTH)));
				}
				return result;
			}
		}
	}

	@Override
	public List<CoreToLoad> listCoresToLoad(Ethernet ethernet,
			boolean loadSystemCores) throws StorageException {
		return callR(conn -> listCoresToLoad(conn, sanitise(ethernet),
				loadSystemCores), "listing cores to load data onto");
	}

	private List<CoreToLoad> listCoresToLoad(Connection conn,
			EthernetImpl ethernet, boolean loadSystemCores)
			throws SQLException {
		try (var s = conn.prepareStatement(LIST_CORES_TO_LOAD_FILTERED)) {
			// ethernet_id
			s.setInt(FIRST, ethernet.id);
			s.setBoolean(SECOND, loadSystemCores);
			try (var rs = s.executeQuery()) {
				var result = new ArrayList<CoreToLoad>();
				while (rs.next()) {
					// core_id, x, y, processor, content
					result.add(new CoreToLoadImpl(rs.getInt(FIRST),
							rs.getInt(SECOND), rs.getInt(THIRD),
							rs.getInt(FOURTH), rs.getInt(FIFTH),
							rs.getInt(SIXTH)));
				}
				return result;
			}
		}
	}

	/**
	 * Gets the actual data specification data.
	 *
	 * @param core
	 *            What core to load from.
	 * @return The contents of the data spec.
	 * @throws StorageException
	 *             If anything fails with the database, or if the core doesn't
	 *             have a data spec.
	 */
	ByteBuffer getDataSpec(CoreToLoad core) throws StorageException {
		return callR(
				conn -> getDataSpec(conn, sanitise(core, "read data specs")),
				"reading data specification for core");
	}

	private static ByteBuffer getDataSpec(Connection conn, CoreToLoadImpl core)
			throws SQLException {
		try (var s = conn.prepareStatement(GET_CORE_DATA_SPEC)) {
			s.setInt(FIRST, core.id);
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return wrap(rs.getBytes(FIRST)).asReadOnlyBuffer();
				}
			}
			throw new IllegalStateException(
					"could not read data spec for core known to have one");
		}
	}

	@Override
	public void saveLoadingMetadata(CoreToLoad core, MemoryLocation start,
			int memoryUsed, int memoryWritten) throws StorageException {
		callV(conn -> saveLoadingMetadata(conn, sanitise(core, "save metadata"),
				start, memoryUsed, memoryWritten),
				"saving data loading metadata");
	}

	private static void saveLoadingMetadata(Connection conn,
			CoreToLoadImpl core, MemoryLocation start, int memoryUsed,
			int memoryWritten) throws SQLException {
		try (var s = conn.prepareStatement(ADD_LOADING_METADATA)) {
			s.setInt(FIRST, start.address);
			s.setInt(SECOND, memoryUsed);
			s.setInt(THIRD, memoryWritten);
			s.setInt(FOURTH, core.id);
			s.executeUpdate();
		}
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
			var b = (EthernetImpl) other;
			return id == b.id;
		}

		@Override
		public int hashCode() {
			return id ^ 444113;
		}
	}

	private final class CoreToLoadImpl extends CoreToLoad {
		/** The primary key. */
		final int id;

		private CoreToLoadImpl(int id, int x, int y, int p, int appID,
				int sizeToWrite) {
			super(x, y, p, appID, sizeToWrite);
			this.id = id;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof CoreToLoadImpl)) {
				return false;
			}
			var c = (CoreToLoadImpl) other;
			return id == c.id;
		}

		@Override
		public int hashCode() {
			return id ^ 187043;
		}

		@Override
		public ByteBuffer getDataSpec() throws StorageException {
			return SQLiteDataSpecStorage.this.getDataSpec(this);
		}
	}
}
