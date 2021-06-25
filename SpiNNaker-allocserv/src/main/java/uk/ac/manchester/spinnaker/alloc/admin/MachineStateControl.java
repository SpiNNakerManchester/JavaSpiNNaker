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
	public class BoardState {
		private final int boardId;

		private BoardState(int id) {
			this.boardId = id;
		}

		/** @return The state of the board. */
		public boolean getState() throws SQLException {
			return db.execute(c -> {
				try (Query q = query(c, "SELECT functioning FROM boards "
						+ "WHERE board_id = :board_id LIMIT 1")) {
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
				try (Update u =
						update(c, "UPDATE boards SET functioning = :enabled "
								+ "WHERE board_id = :board_id")) {
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
			try (Query q = query(conn,
					"SELECT board_id FROM boards JOIN machines "
							+ "ON boards.machine_id = machines.machine_id "
							+ "WHERE machine_name = :machine_name "
							+ "AND x = :x AND y = :y AND z = :z LIMIT 1")) {
				for (Row row : q.call(machine, x, y, z)) {
					return Optional.of(new BoardState(row.getInt("board_id")));
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
			try (Query q = query(conn,
					"SELECT board_id FROM boards JOIN machines "
							+ "ON boards.machine_id = machines.machine_id "
							+ "JOIN bmp ON boards.bmp_id = bmp.bmp_id "
							+ "WHERE machine_name = :machine_name "
							+ "AND bmp.cabinet = :cabinet "
							+ "AND bmp.frame = :frame "
							+ "AND boards.board_num = :board LIMIT 1")) {
				for (Row row : q.call(machine, c, f, b)) {
					return Optional.of(new BoardState(row.getInt("board_id")));
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
			try (Query q = query(conn,
					"SELECT board_id FROM boards JOIN machines "
							+ "ON boards.machine_id = machines.machine_id "
							+ "WHERE machine_name = :machine_name "
							+ "AND boards.address = :address LIMIT 1")) {
				for (Row row : q.call(machine, address)) {
					return Optional.of(new BoardState(row.getInt("board_id")));
				}
			}
			return Optional.empty();
		});
	}
}
