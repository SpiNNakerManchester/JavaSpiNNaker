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

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static uk.ac.manchester.spinnaker.alloc.db.Row.bool;
import static uk.ac.manchester.spinnaker.alloc.db.Row.instant;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.Row.serial;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.alloc.allocator.Epochs;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.Epoch;
import uk.ac.manchester.spinnaker.alloc.bmp.BlacklistIO;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.model.BoardIssueReport;
import uk.ac.manchester.spinnaker.alloc.model.MachineTagging;
import uk.ac.manchester.spinnaker.messages.bmp.Blacklist;

/**
 * How to manage the state of a machine and boards in it.
 *
 * @author Donal Fellows
 */
@Service
public class MachineStateControl extends DatabaseAwareBean {
	// TODO how long to wait?
	private static final int BLACKLIST_WAIT_SECS = 15;

	// TODO how long to wait?
	private static final int BLACKLIST_LONG_WAIT_SECS = 60;

	private static final Duration BLACKLIST_WAIT =
			Duration.ofSeconds(BLACKLIST_WAIT_SECS);

	@Autowired
	private Epochs epochs;

	@Autowired
	private BlacklistIO blacklistHandler;

	/**
	 * Access to the enablement-state of a board.
	 */
	public final class BoardState {
		/** The name of the containing SpiNNaker machine. */
		public final String machineName;

		/** The board ID. Unique. */
		public final int id;

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
		public final Integer board;

		/** The IP address managed by the board's root chip. */
		public final String address;

		/** The BMP serial number, if known. */
		public final String bmpSerial;

		/** The physical board serial number, if known. */
		public final String physicalSerial;

		private BoardState(Row row) {
			this.id = row.getInt("board_id");
			this.x = row.getInt("x");
			this.y = row.getInt("y");
			this.z = row.getInt("z");
			this.cabinet = row.getInt("cabinet");
			this.frame = row.getInt("frame");
			this.board = row.getInteger("board_num");
			this.address = row.getString("address");
			this.machineName = row.getString("machine_name");
			this.bmpSerial = row.getString("bmp_serial_id");
			this.physicalSerial = row.getString("physical_serial_id");
		}

		/**
		 * @return The state of the board.
		 */
		public boolean getState() {
			return execute(false, conn -> {
				try (Query q = conn.query(GET_FUNCTIONING_FIELD)) {
					return q.call1(id).map(bool("functioning")).orElse(false);
				}
			});
		}

		public void setState(boolean newValue) {
			execute(conn -> {
				try (Update u = conn.update(SET_FUNCTIONING_FIELD)) {
					return u.call(newValue, id);
				}
			});
		}

		/** @return What job has been allocated to the board? */
		public Optional<Integer> getAllocatedJob() {
			return execute(false, conn -> {
				try (Query q = conn.query(GET_BOARD_JOB)) {
					return q.call1(id).map(integer("allocated_job"));
				}
			});
		}

		/** @return Is the board switched on? */
		public boolean getPower() {
			return execute(false, conn -> {
				try (Query q = conn.query(GET_BOARD_POWER_INFO)) {
					return q.call1(id).map(bool("board_power")).orElse(false);
				}
			});
		}

		/** @return When was the board last switched on? */
		public Optional<Instant> getPowerOnTime() {
			return execute(false, conn -> {
				try (Query q = conn.query(GET_BOARD_POWER_INFO)) {
					return q.call1(id).map(instant("power_on_timestamp"));
				}
			});
		}

		/** @return When was the board last switched off? */
		public Optional<Instant> getPowerOffTime() {
			return execute(false, conn -> {
				try (Query q = conn.query(GET_BOARD_POWER_INFO)) {
					return q.call1(id).map(instant("power_off_timestamp"));
				}
			});
		}

		/** @return What issues have been logged against the board? */
		public List<BoardIssueReport> getReports() {
			return execute(false, conn -> {
				try (Query q = conn.query(GET_BOARD_REPORTS)) {
					return q.call(id).map(BoardIssueReport::new).toList();
				}
			});
		}
	}

	/**
	 * Look up a board for management.
	 *
	 * @param id
	 *            The unique ID of the board. Because this is fully unique, the
	 *            machine name is not needed.
	 * @return Board state manager
	 */
	public Optional<BoardState> findId(int id) {
		return execute(false, conn -> {
			try (Query q = conn.query(FIND_BOARD_BY_ID)) {
				return q.call1(id).map(BoardState::new);
			}
		});
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
	 */
	public Optional<BoardState> findTriad(String machine, int x, int y, int z) {
		return execute(false, conn -> {
			try (Query q = conn.query(FIND_BOARD_BY_NAME_AND_XYZ)) {
				return q.call1(machine, x, y, z).map(BoardState::new);
			}
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
	 */
	public Optional<BoardState> findPhysical(String machine, int c, int f,
			int b) {
		return execute(false, conn -> {
			try (Query q = conn.query(FIND_BOARD_BY_NAME_AND_CFB)) {
				return q.call1(machine, c, f, b).map(BoardState::new);
			}
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
	 */
	public Optional<BoardState> findIP(String machine, String address) {
		return execute(false, conn -> {
			try (Query q = conn.query(FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
				return q.call1(machine, address).map(BoardState::new);
			}
		});
	}

	/**
	 * @return The mapping from machine names+IDs to tags.
	 */
	public List<MachineTagging> getMachineTagging() {
		return execute(false, conn -> {
			try (Query getMachines = conn.query(GET_ALL_MACHINES);
					Query getTags = conn.query(GET_TAGS)) {
				List<MachineTagging> infos = new ArrayList<>();
				getMachines.call(true).map(MachineTagging::new)
						.forEach(infos::add);
				for (MachineTagging t : infos) {
					t.setTags(getTags.call(t.getId()).map(string("tag")));
				}
				return infos;
			}
		});
	}

	/**
	 * @return The unacknowledged reports about boards with potential problems
	 *         in existing machines, categorised by machine.
	 */
	public Map<String, List<BoardIssueReport>> getMachineReports() {
		return execute(false, conn -> {
			try (Query getMachines = conn.query(GET_ALL_MACHINES);
					Query getMachineReports = conn.query(GET_MACHINE_REPORTS)) {
				return getMachines.call(true).toMap(string("machine_name"),
						machine -> getMachineReports
								.call(machine.getInt("machine_id"))
								.map(BoardIssueReport::new).toList());
			}
		});
	}

	/**
	 * Replace the tags on a machine with a given set.
	 *
	 * @param machineName
	 *            The name of the machine to update the tags of.
	 * @param tags
	 *            The tags to apply. Existing tags will be removed.
	 * @throws IllegalArgumentException
	 *             If the machine with that name doesn't exist.
	 */
	public void updateTags(String machineName, Set<String> tags) {
		execute(conn -> {
			try (Query getMachine = conn.query(GET_NAMED_MACHINE);
					Update deleteTags = conn.update(DELETE_MACHINE_TAGS);
					Update addTag = conn.update(INSERT_TAG)) {
				int machineId = getMachine.call1(machineName, true).orElseThrow(
						() -> new IllegalArgumentException("no such machine"))
						.getInt("machine_id");
				deleteTags.call(machineId);
				for (String tag : tags) {
					addTag.call(machineId, tag);
				}
				return this; // Unimportant value
			}
		});
	}

	/**
	 * Sets whether a machine is in service.
	 *
	 * @param machineName
	 *            The name of the machine to control
	 * @param inService
	 *            Whether to put the machine in or out of service.
	 */
	public void setMachineState(String machineName, boolean inService) {
		execute(conn -> {
			try (Update setState = conn.update(SET_MACHINE_STATE)) {
				setState.call(inService, machineName);
				return this; // Unimportant value
			}
		});
	}

	/**
	 * Exception thrown when blacklists can't be read from or written to the
	 * machine.
	 *
	 * @author Donal Fellows
	 */
	public static final class BlacklistException extends RuntimeException {
		private static final long serialVersionUID = -6450838951059318431L;

		private BlacklistException(String msg, Exception exn) {
			super(msg, exn);
		}

		private BlacklistException(String msg) {
			super(msg);
		}
	}

	/**
	 * Retrieve the blacklist for the given board from the board and store it in
	 * the database.
	 *
	 * @param boardId
	 *            The board to get the blacklist of.
	 * @return The blacklist that was transferred, if any.
	 */
	public Optional<Blacklist> pullBlacklist(int boardId) {
		return findId(boardId).flatMap(board -> {
			try {
				return readBlacklistFromMachine(board).map(bl -> {
					blacklistHandler.writeBlacklistToDB(boardId, bl);
					return bl;
				});
			} catch (InterruptedException e) {
				return Optional.empty();
			}
		});
	}

	/**
	 * Take the blacklist for the given board in the database and write it to
	 * the board.
	 *
	 * @param boardId
	 *            The board to set the blacklist of.
	 * @return The blacklist that was transferred, if any.
	 */
	public Optional<Blacklist> pushBlacklist(int boardId) {
		return findId(boardId).flatMap(board -> blacklistHandler
				.readBlacklistFromDB(boardId).map(bl -> {
					try {
						writeBlacklistToMachine(board, bl);
						return bl;
					} catch (InterruptedException e) {
						return null;
					}
				}));
	}

	void updateAllBlacklists(int machineId) throws InterruptedException {
		List<Integer> boards = execute(false, c -> listAllBoards(c, machineId));

		// TODO consider order randomization and retrieving multiple at once
		for (Integer boardId : boards) {
			Optional<BoardState> board = findId(boardId);
			if (!board.isPresent()) {
				continue;
			}
			readBlacklistFromMachine(board.get()).ifPresent(
					bl -> blacklistHandler.writeBlacklistToDB(boardId, bl));
		}
	}

	private List<Integer> listAllBoards(Connection conn, int machineId) {
		try (Query q = conn.query(GET_ALL_BOARDS)) {
			return q.call(machineId).map(integer("board_id")).toList();
		}
	}

	/**
	 * Given a board, read its blacklist from the database.
	 *
	 * @param board
	 *            Which board to read the blacklist of.
	 * @return The board's blacklist.
	 * @throws DataAccessException
	 *             If access to the DB fails.
	 */
	public Optional<Blacklist> readBlacklistFromDB(BoardState board) {
		return blacklistHandler.readBlacklistFromDB(board.id);
	}

	/**
	 * Given a board, read its blacklist off the machine.
	 *
	 * @param board
	 *            Which board to read the blacklist of.
	 * @return The board's blacklist.
	 * @throws DataAccessException
	 *             If access to the DB fails.
	 * @throws BlacklistException
	 *             If the read fails.
	 * @throws InterruptedException
	 *             If interrupted.
	 */
	public Optional<Blacklist> readBlacklistFromMachine(BoardState board)
			throws InterruptedException {
		try (Op op = new Op(CREATE_BLACKLIST_READ, board.id)) {
			return op.getResult(serial("data", Blacklist.class));
		}
	}

	/**
	 * Write a blacklist to a board on the machine.
	 *
	 * @param board
	 *            Which board to write the blacklist of.
	 * @param blacklist
	 *            The blacklist to write.
	 * @throws DataAccessException
	 *             If access to the DB fails.
	 * @throws BlacklistException
	 *             If the write fails.
	 * @throws InterruptedException
	 *             If interrupted. Note that interrupting the thread does
	 *             <em>not</em> necessarily halt the write of the blacklist.
	 */
	public void writeBlacklistToMachine(BoardState board, Blacklist blacklist)
			throws InterruptedException {
		try (Op op = new Op(CREATE_BLACKLIST_WRITE, board.id, blacklist)) {
			op.getResult(row -> this); // Dummy result
		}
	}

	/**
	 * Manages the transactions used to safely talk with the BMP controller.
	 *
	 * @author Donal Fellows
	 */
	private final class Op implements AutoCloseable {
		private final int op;

		private final Epoch epoch;

		/**
		 * @param operation
		 *            The SQL to create the operation to carry out. Must
		 *            generate an ID, so presumably is an {@code INSERT}.
		 * @param args
		 *            Values to bind to parameters in the SQL.
		 */
		Op(String operation, Object... args) {
			epoch = epochs.getBlacklistEpoch();
			op = execute(conn -> {
				try (Update readReq = conn.update(operation)) {
					return readReq.key(args);
				}
			}).orElseThrow(() -> new BlacklistException(
					"could not create blacklist request"));
		}

		/**
		 * Wait for the result of the request to be ready.
		 *
		 * @param <T>
		 *            The type of the result value.
		 * @param retriever
		 *            How to convert the row containing the result into the
		 *            actual result of the transaction.
		 * @return The wrapped result, or empty if the operation times out.
		 * @throws InterruptedException
		 *             If the thread is interrupted.
		 * @throws BlacklistException
		 *             If the BMP throws an exception.
		 * @throws DataAccessException
		 *             If there is a problem accessing the database.
		 */
		<T> Optional<T> getResult(Function<Row, T> retriever)
				throws InterruptedException, BlacklistException,
				DataAccessException {
			Instant end = now().plus(ofSeconds(BLACKLIST_LONG_WAIT_SECS));
			while (end.isAfter(now())) {
				epoch.waitForChange(BLACKLIST_WAIT);
				Optional<T> result = execute(false, conn -> {
					try (Query getResult =
							conn.query(GET_COMPLETED_BLACKLIST_OP)) {
						return getResult.call1(op)
								.map(this::throwIfFailed)
								.map(retriever);
					}
				});
				if (result.isPresent()) {
					return result;
				}
			}
			return Optional.empty();
		}

		/**
		 * If a row encodes a failure state, unpack the exception from the row
		 * and throw it as a wrapped exception. Otherwise, just pass on the row.
		 *
		 * @param row
		 *            The row to examine.
		 * @return The row, which is now guaranteed to not be a failure.
		 * @throws BlacklistException
		 *             If the row encodes a failure.
		 */
		private Row throwIfFailed(Row row) throws BlacklistException {
			if (row.getBoolean("failed")) {
				throw new BlacklistException(
						"failed to access hardware blacklist",
						row.getSerial("failure", Exception.class));
			}
			return row;
		}

		/**
		 * Deletes the temporary operation description row in the DB.
		 * <p>
		 * {@inheritDoc}
		 */
		@Override
		public void close() {
			execute(conn -> {
				try (Update delReq = conn.update(DELETE_BLACKLIST_OP)) {
					return delReq.call(op);
				}
			});
		}
	}
}
