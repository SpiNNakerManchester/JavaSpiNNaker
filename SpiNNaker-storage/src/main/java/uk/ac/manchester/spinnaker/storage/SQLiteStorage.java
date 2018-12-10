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
public class SQLiteStorage implements Storage {
	private final ConnectionProvider connProvider;

	private static final String STORE =
			"INSERT OR REPLACE INTO storage(x, y, processor, region, content) "
					+ "VALUES(?, ?, ?, ?, ?)";

	private static final String APPEND =
			"INSERT INTO storage(x, y, processor, region, content) "
					+ "VALUES(?, ?, ?, ?, ?) "
					+ "ON CONFLICT(x, y, processor, region) DO UPDATE "
					+ "SET content = storage.content || excluded.content";

	private static final String FETCH = "SELECT content FROM storage WHERE "
			+ "x = ? AND y = ? AND processor = ? AND region = ?";

	private static final String DELETE = "DELETE FROM storage WHERE "
			+ "x = ? AND y = ? AND processor = ? AND region = ?";

	private static final String CORES =
			"SELECT DISTINCT x, y, processor FROM storage "
					+ "ORDER BY x, y, processor";

	private static final String REGIONS =
			"SELECT DISTINCT region FROM storage WHERE "
					+ "x = ? AND y = ? AND processor = ? ORDER BY region";

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
		this.connProvider = connectionProvider;
	}

	@Override
	public int storeRegionContents(HasCoreLocation core, int region,
			byte[] contents) throws StorageException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s =
						conn.prepareStatement(STORE, RETURN_GENERATED_KEYS)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.setInt(FOURTH, region);
			s.setBytes(FIFTH, contents);
			s.executeUpdate();
			try (ResultSet keys = s.getGeneratedKeys()) {
				keys.next();
				return keys.getInt(FIRST);
			}
		} catch (SQLException e) {
			throw new StorageException("while creating a region", e);
		}
	}

	@Override
	public void appendRegionContents(HasCoreLocation core, int region,
			byte[] contents) throws StorageException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(APPEND)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.setInt(FOURTH, region);
			s.setBytes(FIFTH, contents);
			s.executeUpdate();
		} catch (SQLException e) {
			throw new StorageException("while appending to a region", e);
		}
	}

	@Override
	public byte[] getRegionContents(HasCoreLocation core, int region)
			throws StorageException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(FETCH)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.setInt(FOURTH, region);
			try (ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getBytes(FIRST);
				}
				throw new IllegalArgumentException(
						"core " + core + " has no data for region " + region);
			}
		} catch (SQLException e) {
			throw new StorageException("while retrieving a region", e);
		}
	}

	@Override
	public void deleteRegionContents(HasCoreLocation core, int region)
			throws StorageException {
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(DELETE)) {
			s.setInt(FIRST, core.getX());
			s.setInt(SECOND, core.getY());
			s.setInt(THIRD, core.getP());
			s.setInt(FOURTH, region);
			s.executeUpdate();
		} catch (SQLException e) {
			throw new StorageException("while deleting a region", e);
		}
	}

	@Override
	public List<CoreLocation> getCoresWithStorage() throws StorageException {
		ArrayList<CoreLocation> result = new ArrayList<>();
		try (Connection conn = connProvider.getConnection();
				PreparedStatement s = conn.prepareStatement(CORES);
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
				PreparedStatement s = conn.prepareStatement(REGIONS)) {
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
