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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
	public List<Ethernet> listEthernetsToLoad() throws StorageException {
		return callR(SQLiteDataSpecStorage::listEthernetsToLoad, "listing ethernets");
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
