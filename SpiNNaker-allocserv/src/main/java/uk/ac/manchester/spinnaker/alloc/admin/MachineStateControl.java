/*
 * Copyright (c) 2021-2022 The University of Manchester
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

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.db.Row.bool;
import static uk.ac.manchester.spinnaker.alloc.db.Row.instant;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.Row.serial;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.batch;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.curry;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.lmap;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.StateControlProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.Epoch;
import uk.ac.manchester.spinnaker.alloc.bmp.BlacklistStore;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.model.BoardIssueReport;
import uk.ac.manchester.spinnaker.alloc.model.MachineTagging;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;

/**
 * How to manage the state of a machine and boards in it.
 *
 * @author Donal Fellows
 */
@Service
public class MachineStateControl extends DatabaseAwareBean {
	private static final Logger log = getLogger(MachineStateControl.class);

	@Autowired
	private Epochs epochs;

	@Autowired
	private BlacklistStore blacklistStore;

	@Autowired
	private ScheduledExecutorService executor;

	@Autowired
	private SpallocProperties properties;

	private StateControlProperties props;

	@PostConstruct
	private void launchBackground() throws InterruptedException {
		props = properties.getStateControl();
		// After a minute, start retrieving board serial numbers
		executor.schedule((Runnable) this::readAllBoardSerialNumbers,
				props.getBlacklistTimeout().getSeconds(), SECONDS);
		// Why can't I pass a Duration directly there?
	}

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
			return executeRead(conn -> {
				try (var q = conn.query(GET_FUNCTIONING_FIELD)) {
					return q.call1(id).map(bool("functioning")).orElse(false);
				}
			});
		}

		public void setState(boolean newValue) {
			execute(conn -> {
				try (var u = conn.update(SET_FUNCTIONING_FIELD)) {
					return u.call(newValue, id);
				}
			});
		}

		/** @return What job has been allocated to the board? */
		public Optional<Integer> getAllocatedJob() {
			return executeRead(conn -> {
				try (var q = conn.query(GET_BOARD_JOB)) {
					return q.call1(id).map(integer("allocated_job"));
				}
			});
		}

		/** @return Is the board switched on? */
		public boolean getPower() {
			return executeRead(conn -> {
				try (var q = conn.query(GET_BOARD_POWER_INFO)) {
					return q.call1(id).map(bool("board_power")).orElse(false);
				}
			});
		}

		/** @return When was the board last switched on? */
		public Optional<Instant> getPowerOnTime() {
			return executeRead(conn -> {
				try (var q = conn.query(GET_BOARD_POWER_INFO)) {
					return q.call1(id).map(instant("power_on_timestamp"));
				}
			});
		}

		/** @return When was the board last switched off? */
		public Optional<Instant> getPowerOffTime() {
			return executeRead(conn -> {
				try (var q = conn.query(GET_BOARD_POWER_INFO)) {
					return q.call1(id).map(instant("power_off_timestamp"));
				}
			});
		}

		/** @return What issues have been logged against the board? */
		public List<BoardIssueReport> getReports() {
			return executeRead(conn -> {
				try (var q = conn.query(GET_BOARD_REPORTS)) {
					return q.call(id).map(BoardIssueReport::new).toList();
				}
			});
		}

		@Override
		public String toString() {
			return format("(%d,%d,%d)", x, y, z);
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
		return executeRead(conn -> {
			try (var q = conn.query(FIND_BOARD_BY_ID)) {
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
		return executeRead(conn -> {
			try (var q = conn.query(FIND_BOARD_BY_NAME_AND_XYZ)) {
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
		return executeRead(conn -> {
			try (var q = conn.query(FIND_BOARD_BY_NAME_AND_CFB)) {
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
		return executeRead(conn -> {
			try (var q = conn.query(FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
				return q.call1(machine, address).map(BoardState::new);
			}
		});
	}

	/**
	 * @return The mapping from machine names+IDs to tags.
	 */
	public List<MachineTagging> getMachineTagging() {
		return executeRead(conn -> {
			try (var getMachines = conn.query(GET_ALL_MACHINES);
					var getTags = conn.query(GET_TAGS)) {
				var infos = getMachines.call(true).map(MachineTagging::new)
						.toList();
				for (var t : infos) {
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
		return executeRead(conn -> {
			try (var getMachines = conn.query(GET_ALL_MACHINES);
					var getMachineReports = conn.query(GET_MACHINE_REPORTS)) {
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
			try (var getMachine = conn.query(GET_NAMED_MACHINE);
					var deleteTags = conn.update(DELETE_MACHINE_TAGS);
					var addTag = conn.update(INSERT_TAG)) {
				int machineId = getMachine.call1(machineName, true).orElseThrow(
						() -> new IllegalArgumentException("no such machine"))
						.getInt("machine_id");
				deleteTags.call(machineId);
				for (var tag : tags) {
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
			try (var setState = conn.update(SET_MACHINE_STATE)) {
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
	 * @param board
	 *            The board to get the blacklist of.
	 * @return The blacklist that was transferred, if any.
	 */
	public Optional<Blacklist> pullBlacklist(BoardState board) {
		try {
			return readBlacklistFromMachine(board).map(bl -> {
				blacklistStore.writeBlacklist(board.id, bl);
				execute(c -> {
					// These must be done in ONE transaction
					changed(c, board.id);
					synched(c, board.id);
					return this; // Unimportant result
				});
				return bl;
			});
		} catch (InterruptedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Take the blacklist for the given board in the database and write it to
	 * the board.
	 *
	 * @param board
	 *            The board to set the blacklist of.
	 * @return The blacklist that was transferred, if any.
	 */
	public Optional<Blacklist> pushBlacklist(BoardState board) {
		return readBlacklistFromDB(board).map(bl -> {
			try {
				writeBlacklistToMachine(board, bl);
				execute(c -> synched(c, board.id)); // Unimportant result
				return bl;
			} catch (InterruptedException e) {
				return null;
			}
		});
	}

	private boolean changed(Connection conn, int boardId) {
		try (var synched = conn.update(MARK_BOARD_BLACKLIST_CHANGED)) {
			return synched.call(boardId) > 0;
		}
	}

	private boolean synched(Connection conn, int boardId) {
		try (var synched = conn.update(MARK_BOARD_BLACKLIST_SYNCHED)) {
			return synched.call(boardId) > 0;
		}
	}

	/**
	 * Ensure that the database has the actual serial numbers of all boards in a
	 * machine.
	 *
	 * @param machineName
	 *            Which machine to read the serial numbers of.
	 */
	public void readAllBoardSerialNumbers(String machineName) {
		batchReqs(requireNonNull(machineName), "retrieving serial numbers",
				props.getSerialReadBatchSize(),
				curry(Op::new, CREATE_SERIAL_READ_REQ), Op::completed);
	}

	/**
	 * Ensure that the database has the actual serial numbers of all known
	 * boards.
	 */
	private void readAllBoardSerialNumbers() {
		batchReqs(null, "retrieving serial numbers",
				props.getSerialReadBatchSize(),
				curry(Op::new, CREATE_SERIAL_READ_REQ), Op::completed);
	}

	private interface InterruptableConsumer<T> {
		/**
		 * Performs this operation on the given argument.
		 *
		 * @param t
		 *            the input argument
		 * @throws InterruptedException
		 *             if interrupted
		 */
		void accept(T t) throws InterruptedException;
	}

	/**
	 * Perform an action for all boards in a machine (or all known), batching
	 * them as necessary. If interrupted, will complete the currently processing
	 * batch but will not perform further batches.
	 *
	 * @param machineName
	 *            Which machine are we talking about? If {@code null}, all
	 *            boards of all machines will be processed.
	 * @param action
	 *            What are we doing (for log messages)?
	 * @param batchSize
	 *            How many requests to handle at once in a batch of requests to
	 *            the back end engine.
	 * @param opGenerator
	 *            How to generate an individual operation to perform.
	 * @param opResultsHandler
	 *            How to process the results of an individual operation.
	 */
	private void batchReqs(String machineName, String action, int batchSize,
			Function<Integer, Op> opGenerator,
			InterruptableConsumer<Op> opResultsHandler) {
		var boards = executeRead(c -> listAllBoards(c, machineName));
		for (var batch : batch(batchSize, boards)) {
			/*
			 * Theoretically, this could be more efficiently done. Practically,
			 * a proper multi-op scheme is really complex, even before
			 * considering how to handle failure modes! This isn't a performance
			 * sensitive part of the code.
			 */
			var ops = lmap(batch, opGenerator);
			boolean stop = false;
			for (var op : ops) {
				try {
					opResultsHandler.accept(op);
				} catch (RuntimeException e) {
					log.warn("failed while {}", action, e);
				} catch (InterruptedException e) {
					log.info("interrupted while {}", action, e);
					stop = true;
				}
			}
			ops.forEach(Op::close);
			if (stop) {
				// Mark as interrupted
				currentThread().interrupt();
				break;
			}
		}
	}

	/**
	 * Retrieve all blacklists, parse them, and store them in the DB's model.
	 *
	 * @param machineName
	 *            Which machine to get the blacklists of.
	 */
	public void updateAllBlacklists(String machineName) {
		batchReqs(requireNonNull(machineName), "retrieving blacklists",
				props.getBlacklistReadBatchSize(),
				curry(Op::new, CREATE_BLACKLIST_READ),
				op -> op.getResult(serial("data", Blacklist.class))
						.ifPresent(bl -> {
							blacklistStore.writeBlacklist(op.boardId, bl);
							execute(c -> {
								// These must be done in ONE transaction
								changed(c, op.boardId);
								synched(c, op.boardId);
								return this; // Unimportant result
							});
						}));
	}

	private static List<Integer> listAllBoards(Connection conn,
			String machineName) {
		try (var machines = conn.query(GET_NAMED_MACHINE);
				var boards = conn.query(GET_ALL_BOARDS);
				var all = conn.query(GET_ALL_BOARDS_OF_ALL_MACHINES)) {
			if (machineName == null) {
				return all.call().map(integer("board_id")).toList();
			}
			return machines.call1(machineName).map(integer("machine_id")).map(
					mid -> boards.call(mid).map(integer("board_id")).toList())
					.orElse(List.of());
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
		return blacklistStore.readBlacklist(board.id);
	}

	/**
	 * Given a board, write a blacklist for it to the database. Does
	 * <em>not</em> push the blacklist to the board.
	 *
	 * @param board
	 *            Which board to write the blacklist of.
	 * @param blacklist
	 *            The blacklist to write.
	 * @throws DataAccessException
	 *             If access to the DB fails.
	 */
	public void writeBlacklistToDB(BoardState board, Blacklist blacklist) {
		blacklistStore.writeBlacklist(board.id, blacklist);
		execute(c -> changed(c, board.id)); // Unimportant result
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
		try (var op = new Op(CREATE_BLACKLIST_READ, board.id)) {
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
		try (var op = new Op(CREATE_BLACKLIST_WRITE, board.id, blacklist)) {
			op.completed();
		}
	}

	/**
	 * Read the serial number off a board.
	 *
	 * @param board
	 *            Which board to get the serial number of.
	 * @return The serial number.
	 * @throws DataAccessException
	 *             If access to the DB fails.
	 * @throws BlacklistException
	 *             If the write fails.
	 * @throws InterruptedException
	 *             If interrupted.
	 */
	public String getSerialNumber(BoardState board)
			throws InterruptedException {
		try (var op = new Op(CREATE_SERIAL_READ_REQ, board.id)) {
			op.completed();
		}
		// Can now read out of the DB normally
		return findId(board.id).map(b -> b.bmpSerial).orElse(null);
	}

	/**
	 * Test whether a board's blacklist is believed to be synchronised to the
	 * hardware.
	 *
	 * @param board
	 *            Which board?
	 * @return True if the synch has happened, i.e., the time the blacklist data
	 *         was changed is no later than the last time the synch happened.
	 */
	public boolean isBlacklistSynched(BoardState board) {
		return executeRead(conn -> {
			try (var isCurrent = conn.query(IS_BOARD_BLACKLIST_CURRENT)) {
				return isCurrent.call1(board.id).map(bool("current"))
						.orElse(false);
			}
		});
	}

	/**
	 * Manages the transactions used to safely talk with the BMP controller.
	 *
	 * @author Donal Fellows
	 */
	private final class Op implements AutoCloseable {
		private final int op;

		private final Epoch epoch;

		private final int boardId;

		/**
		 * @param operation
		 *            The SQL to create the operation to carry out. Must
		 *            generate an ID, so presumably is an {@code INSERT}.
		 * @param args
		 *            Values to bind to parameters in the SQL.
		 */
		Op(String operation, Object... args) {
			boardId = ((Integer) args[0]).intValue(); // TODO yuck!
			epoch = epochs.getBlacklistEpoch();
			op = execute(conn -> {
				try (var readReq = conn.update(operation)) {
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
			var end = now().plus(props.getBlacklistTimeout());
			while (end.isAfter(now())) {
				var result = executeRead(conn -> {
					try (var getResult =
							conn.query(GET_COMPLETED_BLACKLIST_OP)) {
						return getResult.call1(op).map(this::throwIfFailed)
								.map(retriever);
					}
				});
				if (result.isPresent()) {
					return result;
				}
				epoch.waitForChange(props.getBlacklistPoll());
			}
			return Optional.empty();
		}

		/**
		 * Wait for the result of the request to be ready, then discard that
		 * result. Used instead of {@link #getResult(Function)} when the value
		 * of the result is not interesting at all.
		 *
		 * @throws InterruptedException
		 *             If the thread is interrupted.
		 * @throws BlacklistException
		 *             If the BMP throws an exception.
		 * @throws DataAccessException
		 *             If there is a problem accessing the database.
		 */
		void completed() throws DataAccessException, BlacklistException,
				InterruptedException {
			getResult(row -> this);
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
				try (var delReq = conn.update(DELETE_BLACKLIST_OP)) {
					return delReq.call(op);
				}
			});
		}
	}
}
