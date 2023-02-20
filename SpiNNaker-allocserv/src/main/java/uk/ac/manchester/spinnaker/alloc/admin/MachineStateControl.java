/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.lmap;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.StateControlProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.Epoch;
import uk.ac.manchester.spinnaker.alloc.bmp.BlacklistStore;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.RowMapper;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.model.BoardIssueReport;
import uk.ac.manchester.spinnaker.alloc.model.BoardRecord;
import uk.ac.manchester.spinnaker.alloc.model.MachineTagging;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

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

	/** Just for {@link #launchBackground()}. */
	@Autowired
	private ScheduledExecutorService executor;

	/** Just for {@link #launchBackground()}. */
	@Autowired
	private SpallocProperties properties;

	private StateControlProperties props;

	/** Calls {@link #readAllBoardSerialNumbers()} after a delay. */
	private ScheduledFuture<?> readAllTask;

	@PostConstruct
	private void launchBackground() {
		props = properties.getStateControl();
		// After a minute, start retrieving board serial numbers
		readAllTask =
				executor.schedule((Runnable) this::readAllBoardSerialNumbers,
						props.getBlacklistTimeout().getSeconds(), SECONDS);
		// Why can't I pass a Duration directly there?
	}

	@PreDestroy
	private void stopBackground() {
		var t = readAllTask;
		if (t != null) {
			readAllTask = null;
			try {
				t.cancel(true);
				t.get();
			} catch (InterruptedException | CancellationException e) {
				log.trace("stopped background loader", e);
			} catch (Exception e) {
				log.info("failure in background board serial number fetch", e);
			}
		}
	}

	/**
	 * Access to the enablement-state of a board.
	 */
	public final class BoardState {
		// No validity definitions; instances originate in DB
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
		 * @return The allocatable state of the board. If a board is not
		 *         allocatable, it will not be handed out in new allocations to
		 *         jobs, but can continue to be used by whatever job it is
		 *         currently allocated to (if any).
		 */
		public boolean getState() {
			return executeRead(conn -> {
				try (var q = conn.query(GET_FUNCTIONING_FIELD)) {
					return q.call1(bool("functioning"), id).orElse(false);
				}
			});
		}

		/** @param newValue The allocatable state to set the board to. */
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
					return q.call1(integer("allocated_job"), id);
				}
			});
		}

		/** @return Is the board switched on? */
		public boolean getPower() {
			return executeRead(conn -> {
				try (var q = conn.query(GET_BOARD_POWER_INFO)) {
					return q.call1(bool("board_power"), id).orElse(false);
				}
			});
		}

		/** @return When was the board last switched on? */
		public Optional<Instant> getPowerOnTime() {
			return executeRead(conn -> {
				try (var q = conn.query(GET_BOARD_POWER_INFO)) {
					return q.call1(instant("power_on_timestamp"), id);
				}
			});
		}

		/** @return When was the board last switched off? */
		public Optional<Instant> getPowerOffTime() {
			return executeRead(conn -> {
				try (var q = conn.query(GET_BOARD_POWER_INFO)) {
					return q.call1(instant("power_off_timestamp"), id);
				}
			});
		}

		/** @return What issues have been logged against the board? */
		public List<BoardIssueReport> getReports() {
			return executeRead(conn -> {
				try (var q = conn.query(GET_BOARD_REPORTS)) {
					return q.call(BoardIssueReport::new, id);
				}
			});
		}

		@Override
		public String toString() {
			return format("(%d,%d,%d)", x, y, z);
		}

		/**
		 * Convert this active object to a static record.
		 *
		 * @return The static (conceptually serialisable) record object.
		 */
		public BoardRecord toBoardRecord() {
			var br = new BoardRecord();
			br.setId(id);
			br.setMachineName(machineName);
			br.setX(x);
			br.setY(y);
			br.setZ(z);
			br.setCabinet(cabinet);
			br.setFrame(frame);
			br.setBoard(board);
			br.setIpAddress(address);
			br.setBmpSerial(bmpSerial);
			br.setPhysicalSerial(physicalSerial);
			br.setLastPowerOn(getPowerOnTime().orElse(null));
			br.setLastPowerOff(getPowerOffTime().orElse(null));
			br.setPowered(getPower());
			br.setJobId(getAllocatedJob().orElse(null));
			br.setReports(getReports());
			br.setEnabled(getState());
			return br;
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
				return q.call1(BoardState::new, id);
			}
		});
	}

	/**
	 * Look up a board for management.
	 *
	 * @param machine
	 *            The name of the machine.
	 * @param coords
	 *            Triad coordinates
	 * @return Board state manager
	 */
	public Optional<BoardState> findTriad(@NotBlank String machine,
			@Valid @NonNull TriadCoords coords) {
		return executeRead(conn -> {
			try (var q = conn.query(FIND_BOARD_BY_NAME_AND_XYZ)) {
				return q.call1(BoardState::new, machine, coords.x, coords.y,
						coords.z);
			}
		});
	}

	/**
	 * Look up a board for management.
	 *
	 * @param machine
	 *            The name of the machine.
	 * @param coords
	 *            Physical coordinates
	 * @return Board state manager
	 */
	public Optional<BoardState> findPhysical(@NotBlank String machine,
			@Valid @NotNull PhysicalCoords coords) {
		return executeRead(conn -> {
			try (var q = conn.query(FIND_BOARD_BY_NAME_AND_CFB)) {
				return q.call1(BoardState::new, machine, coords.c, coords.f,
						coords.b);
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
	public Optional<BoardState> findIP(@NotBlank String machine,
			@IPAddress String address) {
		return executeRead(conn -> {
			try (var q = conn.query(FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
				return q.call1(BoardState::new, machine, address);
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
				var infos = getMachines.call(MachineTagging::new, true);
				for (var t : infos) {
					t.setTags(getTags.call(string("tag"), t.getId()));
				}
				return infos;
			}
		});
	}

	private class MachineNameId {
		int id;

		String name;

		MachineNameId(Row row) {
			id = row.getInt("machine_id");
			name = row.getString("machine_name");
		}
	}

	/**
	 * @return The unacknowledged reports about boards with potential problems
	 *         in existing machines, categorised by machine.
	 */
	public Map<String, List<BoardIssueReport>> getMachineReports() {
		return executeRead(conn -> {
			try (var getMachines = conn.query(GET_ALL_MACHINES);
					var getMachineReports = conn.query(GET_MACHINE_REPORTS)) {
				return Row.stream(getMachines.call(MachineNameId::new, true))
						.toMap(m -> m.name, m -> getMachineReports.call(
								BoardIssueReport::new, m.id));
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
	public void updateTags(@NotBlank String machineName,
			Set<@NotBlank String> tags) {
		execute(conn -> {
			try (var getMachine = conn.query(GET_NAMED_MACHINE);
					var deleteTags = conn.update(DELETE_MACHINE_TAGS);
					var addTag = conn.update(INSERT_TAG)) {
				int machineId = getMachine.call1(integer("machine_id"),
						machineName, true).orElseThrow(
						() -> new IllegalArgumentException("no such machine"));
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
	public void setMachineState(@NotBlank String machineName,
			boolean inService) {
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
			return synched.call(Instant.now(), boardId) > 0;
		}
	}

	private boolean synched(Connection conn, int boardId) {
		try (var synched = conn.update(MARK_BOARD_BLACKLIST_SYNCHED)) {
			return synched.call(Instant.now(), boardId) > 0;
		}
	}

	/**
	 * Ensure that the database has the actual serial numbers of all boards in a
	 * machine.
	 *
	 * @param machineName
	 *            Which machine to read the serial numbers of.
	 */
	public void readAllBoardSerialNumbers(@NotBlank String machineName) {
		scheduleSerialNumberReads(requireNonNull(machineName));
	}

	/**
	 * Ensure that the database has the actual serial numbers of all known
	 * boards.
	 */
	private void readAllBoardSerialNumbers() {
		scheduleSerialNumberReads(null);
	}

	/**
	 * Common core of {@link #readAllBoardSerialNumbers(String)} and
	 * {@link #readAllBoardSerialNumbers()}.
	 *
	 * @param machineName
	 *            The machine name, or {@code null} for all.
	 */
	@SuppressWarnings("MustBeClosed")
	private void scheduleSerialNumberReads(String machineName) {
		batchReqs(machineName, "retrieving serial numbers",
				props.getSerialReadBatchSize(),
				id -> new Op(CREATE_SERIAL_READ_REQ, id), Op::completed);
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
			try {
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
			} finally {
				ops.forEach(Op::close);
			}
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
	@SuppressWarnings("MustBeClosed")
	public void updateAllBlacklists(@NotBlank String machineName) {
		batchReqs(requireNonNull(machineName), "retrieving blacklists",
				props.getBlacklistReadBatchSize(),
				id -> new Op(CREATE_BLACKLIST_READ, id),
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
				return all.call(integer("board_id"));
			}
			return machines.call1(integer("machine_id"), machineName).map(
					mid -> boards.call(integer("board_id"), mid))
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
	public void writeBlacklistToDB(BoardState board,
			@Valid Blacklist blacklist) {
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
	public void writeBlacklistToMachine(BoardState board,
			@Valid Blacklist blacklist) throws InterruptedException {
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
				return isCurrent.call1(bool("current"), board.id)
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
		 *            Values to bind to parameters in the SQL. The first
		 *            parameter <em>must</em> be the board ID! Presumably that
		 *            will be the board that the operation refers to.
		 */
		@MustBeClosed
		Op(@CompileTimeConstant final String operation, Object... args) {
			boardId = (Integer) args[0];
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
		<T> Optional<T> getResult(RowMapper<T> retriever)
				throws InterruptedException, BlacklistException,
				DataAccessException {
			var end = now().plus(props.getBlacklistTimeout());
			while (end.isAfter(now())) {
				var result = executeRead(conn -> {
					try (var getResult =
							conn.query(GET_COMPLETED_BLACKLIST_OP)) {
						return getResult.call1(row -> retriever.mapRow(
								throwIfFailed(row)), op);
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
			getResult(__ -> this);
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
