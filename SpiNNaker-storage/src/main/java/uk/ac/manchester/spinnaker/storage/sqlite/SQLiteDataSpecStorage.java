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

import static java.nio.ByteBuffer.wrap;
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

import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;

/**
 * How to actually talk to an SQLite database.
 *
 * @author Donal Fellows
 */
public class SQLiteDataSpecStorage extends SQLiteConnectionManager<DSEStorage>
		implements DSEStorage {
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
	public void saveLoadingMetadata(CoreToLoad core, int startAddress,
			int memoryUsed, int memoryWritten) throws StorageException {
		callV(conn -> saveLoadingMetadata(conn, sanitise(core, "save metadata"),
				startAddress, memoryUsed, memoryWritten),
				"saving data loading metadata");
	}

	private static void saveLoadingMetadata(Connection conn,
			CoreToLoadImpl core, int startAddress, int memoryUsed,
			int memoryWritten) throws SQLException {
		try (var s = conn.prepareStatement(ADD_LOADING_METADATA)) {
			s.setInt(FIRST, startAddress);
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
			EthernetImpl b = (EthernetImpl) other;
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
			CoreToLoadImpl c = (CoreToLoadImpl) other;
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

	@Override
	public int getSizeForCore(CoreToLoad coreToLoad) {
		return 0; // FIXME incomplete method
	}
}
