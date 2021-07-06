/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.admin;

import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;

import java.sql.SQLException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;

/**
 * How to manage the state of a machine and boards in it.
 *
 * @author Donal Fellows
 */
@Component
public class MachineStateControl extends SQLQueries {
	@Autowired
	private DatabaseEngine db;

	/**
	 * Access to the enablement-state of a board.
	 */
	public final class BoardState {
		private final int boardId;

		/** The X triad coordinate. */
		public final int x;

		/** The Y triad coordinate. */
		public final int y;

		/** The Z triad coordinate. */
		public final int z;

		/** The cabinet number. */
		public final int cabinet;

		/** The frame number. */
		public final int frame;

		/** The board number. */
		public final int board;

		/** The IP address managed by the board's root chip. */
		public final String address;

		private BoardState(Row row) throws SQLException {
			this.boardId = row.getInt("board_id");
			this.x = row.getInt("x");
			this.y = row.getInt("y");
			this.z = row.getInt("z");
			this.cabinet = row.getInt("cabinet");
			this.frame = row.getInt("frame");
			this.board = row.getInt("board_num");
			this.address = row.getString("address");
		}

		/**
		 * @return The state of the board.
		 * @throws SQLException
		 *             if something goes wrong
		 */
		public boolean getState() throws SQLException {
			return db.execute(c -> {
				try (Query q = query(c, GET_FUNCTIONING_FIELD)) {
					Optional<Row> result = q.call1(boardId);
					if (result.isPresent()) {
						return result.get().getBoolean("functioning");
					}
					return false;
				}
			});
		}

		public void setState(boolean newValue) throws SQLException {
			db.executeVoid(c -> {
				try (Update u = update(c, SET_FUNCTIONING_FIELD)) {
					u.call(newValue, boardId);
				}
			});
		}
	}

	/**
	 * Look up a board for management.
	 *
	 * @param machine
	 *            The name of the machine.
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 * @param z
	 *            Z coordinate
	 * @return Board state manager
	 * @throws SQLException
	 *             If anything goes wrong
	 */
	public Optional<BoardState> findTriad(String machine, int x, int y, int z)
			throws SQLException {
		return db.execute(conn -> {
			try (Query q = query(conn, FIND_BOARD_BY_NAME_AND_XYZ)) {
				for (Row row : q.call(machine, x, y, z)) {
					return Optional.of(new BoardState(row));
				}
			}
			return Optional.empty();
		});
	}

	/**
	 * Look up a board for management.
	 *
	 * @param machine
	 *            The name of the machine.
	 * @param c
	 *            Cabinet number
	 * @param f
	 *            Frame number
	 * @param b
	 *            Board number
	 * @return Board state manager
	 * @throws SQLException
	 *             If anything goes wrong
	 */
	public Optional<BoardState> findPhysical(String machine, int c, int f,
			int b) throws SQLException {
		return db.execute(conn -> {
			try (Query q = query(conn, FIND_BOARD_BY_NAME_AND_CFB)) {
				for (Row row : q.call(machine, c, f, b)) {
					return Optional.of(new BoardState(row));
				}
			}
			return Optional.empty();
		});
	}

	/**
	 * Look up a board for management.
	 *
	 * @param machine
	 *            The name of the machine.
	 * @param address
	 *            Board IP address
	 * @return Board state manager
	 * @throws SQLException
	 *             If anything goes wrong
	 */
	public Optional<BoardState> findIP(String machine, String address)
			throws SQLException {
		return db.execute(conn -> {
			try (Query q = query(conn, FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
				for (Row row : q.call(machine, address)) {
					return Optional.of(new BoardState(row));
				}
			}
			return Optional.empty();
		});
	}
}
