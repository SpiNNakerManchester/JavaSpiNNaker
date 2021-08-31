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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteErrorCode.SQLITE_BUSY;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.rowsAsList;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.POWER;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.READY;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.sqlite.SQLiteException;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;

@Component
public class AllocatorTask extends SQLQueries implements PowerController {
	/** Triads contain three boards. */
	private static final int TRIAD_DEPTH = 3;

	/**
	 * @see #setPower(Connection,int,PowerState)
	 */
	private static final EnumSet<Direction> NO_PERIMETER =
			EnumSet.noneOf(Direction.class);

	private static final Logger log = getLogger(AllocatorTask.class);

	private static final Rectangle ONE_BOARD = new Rectangle(1, 1, 1);

	/**
	 * Maximum number of jobs that we actually run the quota check for. Used
	 * because we are reusing a query, and we'll probably never have that many
	 * live jobs even on the big machine.
	 */
	private static final Integer NUMBER_OF_JOBS_TO_QUOTA_CHECK = 100000;

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private Epochs epochs;

	@Autowired
	private ServiceMasterControl serviceControl;

	@Autowired
	private QuotaManager quotaManager;

	@Value("${spalloc.allocator.importance-span:1000}")
	private int importanceSpan;

	@Value("${spalloc.historical-data.grace-period:P14D}")
	private Duration tombstoneGracePeriod;

	/**
	 * Helper class representing a rectangle of triads.
	 *
	 * @author Donal Fellows
	 */
	private static final class Rectangle {
		final int width;

		final int height;

		/** Depth of rectangle. 1 or 3 */
		final int depth;

		Rectangle(int width, int height, int depth) {
			this.width = width;
			this.height = height;
			this.depth = depth;
		}
	}

	private static final class TriadCoords {
		final int x;

		final int y;

		final int z;

		TriadCoords(Row row) throws SQLException {
			this.x = row.getInt("x");
			this.y = row.getInt("y");
			this.z = row.getInt("z");
		}
	}

	/**
	 * Allocate all current requests for resources.
	 *
	 * @throws SQLException
	 *             If anything goes wrong at the DB level
	 */
	@Scheduled(fixedDelayString = "${spalloc.allocator.period:PT5S}")
	public void allocate() throws SQLException {
		if (serviceControl.isPaused()) {
			return;
		}

		try {
			if (db.execute(this::allocate)) {
				log.debug("advancing job epoch");
				epochs.nextJobsEpoch();
			}
		} catch (SQLiteException e) {
			if (e.getResultCode().equals(SQLITE_BUSY)) {
				log.info("database is busy; "
						+ "will try allocation processing later");
				return;
			}
			throw e;
		}
	}

	/** Encapsulates the queries and updates used in power control. */
	private class PowerSQL implements AutoCloseable {
		private final Query getJobState;

		private final Query getJobBoards;

		private final Query getPerimeter;

		private final Update issuePowerChange;

		private final Update setStatePending;

		PowerSQL(Connection conn) throws SQLException {
			getJobState = query(conn, GET_JOB);
			getJobBoards = query(conn, GET_JOB_BOARDS);
			getPerimeter = query(conn, getPerimeterLinks);
			issuePowerChange = update(conn, issueChangeForJob);
			setStatePending = update(conn, SET_STATE_PENDING);
		}

		@Override
		public void close() throws SQLException {
			getJobState.close();
			getJobBoards.close();
			getPerimeter.close();
			issuePowerChange.close();
			setStatePending.close();
		}
	}

	/** Encapsulates the queries and updates used in allocation. */
	private final class AllocSQL extends PowerSQL {
		private final Update bumpImportance;

		private final Query getTasks;

		private final Update delete;

		private final Query findFreeBoard;

		private final Query getRectangles;

		private final Query countConnectedBoards;

		private final Query findSpecificBoard;

		private final Query getConnectedBoardIDs;

		private final Update allocBoard;

		private final Update allocJob;

		AllocSQL(Connection conn) throws SQLException {
			super(conn);
			bumpImportance = update(conn, BUMP_IMPORTANCE);
			getTasks = query(conn, getAllocationTasks);
			delete = update(conn, DELETE_TASK);
			findFreeBoard = query(conn, FIND_FREE_BOARD);
			getRectangles = query(conn, findRectangle);
			countConnectedBoards = query(conn, countConnected);
			findSpecificBoard = query(conn, findLocation);
			getConnectedBoardIDs = query(conn, getConnectedBoards);
			allocBoard = update(conn, ALLOCATE_BOARDS_BOARD);
			allocJob = update(conn, ALLOCATE_BOARDS_JOB);
		}

		@Override
		public void close() throws SQLException {
			super.close();
			bumpImportance.close();
			getTasks.close();
			delete.close();
			findFreeBoard.close();
			getRectangles.close();
			countConnectedBoards.close();
			findSpecificBoard.close();
			getConnectedBoardIDs.close();
			allocBoard.close();
			allocJob.close();
		}
	}

	/** Encapsulates the queries and updates used in deletion. */
	private class DestroySQL extends PowerSQL {
		private final Query getJob;

		private final Update markAsDestroyed;

		private final Update killAlloc;

		private final Update killPending;

		DestroySQL(Connection conn) throws SQLException {
			super(conn);
			getJob = query(conn, GET_JOB);
			markAsDestroyed = update(conn, DESTROY_JOB);
			killAlloc = update(conn, KILL_JOB_ALLOC_TASK);
			killPending = update(conn, KILL_JOB_PENDING);
		}

		@Override
		public void close() throws SQLException {
			super.close();
			getJob.close();
			markAsDestroyed.close();
			killAlloc.close();
			killPending.close();
		}
	}

	/**
	 * Allocate all current requests for resources.
	 *
	 * @param conn
	 *            The DB connection
	 * @return Whether any changes have been done
	 * @throws SQLException
	 *             If anything goes wrong at the DB level
	 */
	@Deprecated
	boolean allocate(Connection conn) throws SQLException {
		try (AllocSQL sql = new AllocSQL(conn)) {
			int maxImportance = -1;
			boolean changed = false;
			for (Row row : sql.getTasks.call()) {
				int id = row.getInt("req_id");
				int importance = row.getInt("importance");
				if (importance > maxImportance) {
					maxImportance = importance;
				} else if (importance < maxImportance - importanceSpan) {
					// Too much of a span
					continue;
				}
				JobState currentState =
						row.getEnum("job_state", JobState.class);
				boolean handled = true;
				try {
					// Non-queued jobs should not have allocations done!
					if (currentState == QUEUED) {
						handled = allocate(sql, row);
					}
					/*
					 * NB: having an exception counts as handled; we will nuke
					 * the task.
					 */
				} finally {
					log.debug("allocate for {}: {}", id, handled);
					if (handled) {
						changed |= sql.delete.call(id) > 0;
					}
				}
			}
			/*
			 * Those tasks which weren't allocated get their importance bumped
			 * so they get considered with higher priority when the allocator
			 * runs next time.
			 */
			sql.bumpImportance.call();
			return changed;
		}
	}

	/**
	 * Destroy jobs that have missed their keepalive.
	 *
	 * @throws SQLException
	 *             If anything goes wrong at the DB level
	 */
	@Scheduled(fixedDelayString = "${spalloc.keepalive.expiry-period:PT30S}")
	public void expireJobs() throws SQLException {
		if (serviceControl.isPaused()) {
			return;
		}

		try {
			if (db.execute(this::expireJobs)) {
				epochs.nextJobsEpoch();
				epochs.nextMachineEpoch();
			}
		} catch (SQLiteException e) {
			if (e.getResultCode().equals(SQLITE_BUSY)) {
				log.info("database is busy; "
						+ "will try job expiry processing later");
				return;
			}
			throw e;
		}
	}

	/**
	 * Destroy jobs that have missed their keepalive.
	 *
	 * @param conn
	 *            How to talk to the DB
	 * @return Whether any jobs have been expired.
	 * @throws SQLException
	 *             If anything goes wrong at the DB level
	 */
	@Deprecated // INTERNAL
	boolean expireJobs(Connection conn) throws SQLException {
		boolean changed = false;
		try (Query find = query(conn, FIND_EXPIRED_JOBS)) {
			List<Integer> toKill =
					rowsAsList(find.call(), r -> r.getInteger("job_id"));
			for (int id : toKill) {
				changed |= destroyJob(conn, id, "keepalive expired");
			}
		}
		try (Query find = query(conn, GET_LIVE_JOB_IDS)) {
			List<Integer> toKill = new ArrayList<>();
			for (Row row : find.call(NUMBER_OF_JOBS_TO_QUOTA_CHECK, 0)) {
				int machineId = row.getInt("machine_id");
				int jobId = row.getInt("job_id");
				if (!quotaManager.hasQuotaRemaining(machineId, jobId)) {
					toKill.add(jobId);
				}
			}
			for (int id : toKill) {
				changed |= destroyJob(conn, id, "quota exceeded");
			}
		}
		return changed;
	}

	/**
	 * Migrates long dead jobs to the historical data DB.
	 *
	 * @throws SQLException
	 *             If DB access fails.
	 */
	@Scheduled(cron = "${spalloc.historical-data.schedule:0 0 2 * * *}")
	public void tombstone() throws SQLException {
		if (serviceControl.isPaused()) {
			return;
		}

		try (Connection conn = db.getConnection()) {
			tombstone(conn);
		} catch (SQLiteException e) {
			if (e.getResultCode().equals(SQLITE_BUSY)) {
				log.info("database is busy; "
						+ "will try job tombstone processing later");
				return;
			}
			throw e;
		}
	}

	/**
	 * Implementation of {@link #tombstone()}. This is done as two transactions
	 * to help manage the amount of locking; nothing else ought to be updating
	 * any of these jobs at the time this task usually runs, but we'll still try
	 * to keep things minimally locked.
	 *
	 * @param conn
	 *            The DB connection
	 */
	private void tombstone(Connection conn) throws SQLException {
		try (Query copy = query(conn, copyToHistoricalData);
				Update delete = update(conn,
						DELETE_JOB_RECORD)) {
			List<Integer> jobIds = transaction(conn,
					() -> rowsAsList(copy.call(tombstoneGracePeriod),
							row -> row.getInteger("job_id")));
			transaction(conn, () -> {
				for (Integer jobId : jobIds) {
					// I don't think a NULL jobId is possible
					if (nonNull(jobId)) {
						delete.call(jobId);
					}
				}
			});
		}
	}

	@Override
	public void destroyJob(int id, String reason) throws SQLException {
		if (db.execute(conn -> destroyJob(conn, id, reason))) {
			epochs.nextJobsEpoch();
			epochs.nextMachineEpoch();
		}
	}

	private boolean destroyJob(Connection conn, int id, String reason)
			throws SQLException {
		log.debug("destroying job {} \"{}\"", id, reason);
		try (DestroySQL sql = new DestroySQL(conn)) {
			Optional<Row> row = sql.getJob.call1(id);
			if (!row.isPresent()) {
				return false;
			}
			if (row.get().getEnum("job_state", JobState.class) == DESTROYED) {
				// Don't do anything if the job is already destroyed
				return false;
			}
			sql.killPending.call(id);
			// Inserts into pending_changes; these run after job is dead
			setPower(sql, id, PowerState.OFF, DESTROYED);
			sql.killAlloc.call(id);
			return sql.markAsDestroyed.call(reason, id) > 0;
		}
	}

	/**
	 * Computes the estimate of what sort of allocation will be required.
	 * Converts a number of boards into a close-to-square size to search for.
	 * <p>
	 * With the big machine's level of resources, that's good enough. For now.
	 *
	 * @author Donal Fellows
	 */
	private static final class DimensionEstimate {
		private static final double HORIZONTAL_FACTOR = 1.5;

		private static final double VERTICAL_FACTOR = 2.0;

		/** The estimated width. */
		final int width;

		/** The estimated height. */
		final int height;

		/**
		 * The number of boards in the rectangle of triads that we can tolerate
		 * being down due to overallocation (due to the use of rectangles and
		 * triads).
		 */
		final int tolerance;

		DimensionEstimate(int numBoards, Rectangle max) {
			int numTriads = numBoards / TRIAD_DEPTH;
			if (numBoards % TRIAD_DEPTH > 0) {
				numTriads++;
			}
			width = (int) min(ceil(sqrt(numTriads)), max.width);
			height = (int) min(ceil(numTriads / width), max.height);
			tolerance = (width * height * TRIAD_DEPTH) - numBoards;
			if (width < 1 || height < 1) {
				throw new IllegalArgumentException(
						"computed dimensions must be greater than zero");
			}
			if (tolerance < 0) {
				throw new IllegalArgumentException(
						"that job cannot possibly fit on this machine");
			}
		}

		DimensionEstimate(int w, int h, Rectangle max) {
			int numBoards = w * h;
			width = (int) min(ceil(w / HORIZONTAL_FACTOR), max.width);
			height = (int) min(ceil(h / VERTICAL_FACTOR), max.height);
			tolerance = (width * height * TRIAD_DEPTH) - numBoards;
			if (width < 1 || height < 1) {
				throw new IllegalArgumentException(
						"computed dimensions must be greater than zero");
			}
			if (tolerance < 0) {
				throw new IllegalArgumentException(
						"that job cannot possibly fit on this machine");
			}
		}

		/** @return The estimated dimensions as a rectangle. */
		Rectangle getRect() {
			return new Rectangle(width, height, TRIAD_DEPTH);
		}
	}

	/**
	 * Perform the allocation for a particular task. Note that allocation does
	 * not actually send any messages to the board's BMP (though it does
	 * schedule them). That's an entirely different thing.
	 *
	 * @param sql
	 *            How to talk to the DB
	 * @param task
	 *            The task (as a result set).
	 * @return {@code true} if a decision has been taken about the task, or
	 *         {@code false} if the task is to have the allocator run again in
	 *         the next schedule slot.
	 * @throws SQLException
	 *             If anything goes wrong at the DB level
	 */
	private boolean allocate(AllocSQL sql, Row task) throws SQLException {
		int jobId = task.getInt("job_id");
		int machineId = task.getInt("machine_id");
		Rectangle max = new Rectangle(task.getInt("max_width"),
				task.getInt("max_height"), TRIAD_DEPTH);
		int maxDeadBoards = task.getInt("max_dead_boards");
		Integer numBoards = task.getInteger("num_boards");
		if (nonNull(numBoards) && numBoards > 0) {
			if (numBoards == 1) {
				return allocateOneBoard(sql, jobId, machineId);
			}
			DimensionEstimate estimate = new DimensionEstimate(numBoards, max);
			return allocateDimensions(sql, jobId, machineId, estimate,
					maxDeadBoards);
		}

		Integer width = task.getInteger("width");
		Integer height = task.getInteger("height");
		if (nonNull(width) && nonNull(height) && width > 0 && height > 0) {
			if (height == 1 && width == 1) {
				return allocateOneBoard(sql, jobId, machineId);
			}
			DimensionEstimate estimate =
					new DimensionEstimate(width, height, max);
			log.debug(
					"resolved request for {}x{} boards to {}x{} triads "
							+ "with tolerance {}",
					width, height, estimate.width, estimate.height,
					estimate.tolerance);
			return allocateDimensions(sql, jobId, machineId, estimate,
					maxDeadBoards);
		}

		Integer root = task.getInteger("board_id");
		if (nonNull(root)) {
			// Ignores maxDeadBoards; is a single-board allocate
			return allocateBoard(sql, jobId, machineId, root);
		}

		log.warn("job {} could not be allocated; "
				+ "bad request will be cleared from queue", jobId);
		return true;
	}

	private boolean allocateOneBoard(AllocSQL sql, int jobId, int machineId)
			throws SQLException {
		for (Row row : sql.findFreeBoard.call(machineId)) {
			TriadCoords root = new TriadCoords(row);
			if (setAllocation(sql, jobId, ONE_BOARD, machineId, root)) {
				return true;
			}
		}
		return false;
	}

	private boolean allocateDimensions(AllocSQL sql, int jobId, int machineId,
			DimensionEstimate estimate, int userMaxDead) throws SQLException {
		int tolerance = userMaxDead + estimate.tolerance;
		int minArea =
				estimate.width * estimate.height * TRIAD_DEPTH - tolerance;
		for (Row rs : sql.getRectangles.call(estimate.width, estimate.height,
				machineId, tolerance)) {
			TriadCoords root = new TriadCoords(rs);
			if (minArea > 1) {
				/*
				 * Check that a minimum number of boards are reachable from the
				 * proposed root board. If the root board is isolated, we don't
				 * care if the rest of the allocation works because the rest of
				 * the toolchain won't cope.
				 */
				int size = connectedSize(sql, machineId, root, estimate);
				if (size < minArea) {
					continue;
				}
			}
			if (setAllocation(sql, jobId, estimate.getRect(), machineId,
					root)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Find the number of boards that are reachable from the proposed root
	 * board.
	 *
	 * @param sql
	 *            How to talk to the DB
	 * @param machineId
	 *            The machine on which the allocation is happening
	 * @param root
	 *            Root logical coordinates
	 * @param estimate
	 *            The planned allocation dimensions
	 * @return How many boards in the allocation are reachable.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	private int connectedSize(AllocSQL sql, int machineId, TriadCoords root,
			DimensionEstimate estimate) throws SQLException {
		int size = -1;
		for (Row row : sql.countConnectedBoards.call(machineId, root.x, root.y,
				estimate.width, estimate.height)) {
			size = row.getInt("connected_size");
		}
		return size;
	}

	private boolean allocateBoard(AllocSQL sql, int jobId, int machineId,
			int boardId) throws SQLException {
		for (Row row : sql.findSpecificBoard.call(machineId, boardId)) {
			if (setAllocation(sql, jobId, ONE_BOARD, machineId,
					new TriadCoords(row))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Does the actual allocation.
	 * <p>
	 * At this point, we've checked that there's enough boards <em>and</em> we
	 * are running in a transaction. We take particular care that we only
	 * actually allocate boards that are reachable from the root board; this is
	 * assumed by the SpiNNaker tools, so we'd better conform to that
	 * expectation. Fortunately, the bigger the allocation, the more likely that
	 * is true (and it is trivially true for single-board allocations.)
	 * <p>
	 * If you want a multi-board allocation, you'd better be allocating a full
	 * triad's-worth of depth or you'll get nothing.
	 *
	 * @param sql
	 *            How to talk to the DB
	 * @param jobId
	 *            What job are we allocating for
	 * @param rect
	 *            Proposed rectangle size
	 * @param machineId
	 *            What machine are we allocating on
	 * @param root
	 *            Proposed root coordinates
	 * @return Whether we have successfully allocated (the allocation is stored
	 *         in the DB)
	 * @throws SQLException
	 *             If something goes wrong with talking to the DB
	 */
	private boolean setAllocation(AllocSQL sql, int jobId, Rectangle rect,
			int machineId, TriadCoords root) throws SQLException {
		log.debug("performing allocation for {}: {}x{}x{} at {}:{}:{}", jobId,
				rect.width, rect.height, rect.depth, root.x, root.y, root.z);
		// TODO Use RETURNING to combine getConnectedBoardIDs and allocBoard
		List<Integer> boardsToAllocate = rowsAsList(
				sql.getConnectedBoardIDs.call(machineId, root.x, root.y, root.z,
						rect.width, rect.height, rect.depth),
				row -> row.getInteger("board_id"));
		if (boardsToAllocate.isEmpty()) {
			return false;
		}
		for (int boardId : boardsToAllocate) {
			sql.allocBoard.call(jobId, boardId);
		}

		sql.allocJob.call(rect.width, rect.height, rect.depth,
				boardsToAllocate.get(0), boardsToAllocate.size(), jobId);
		log.debug("allocated {} boards to {}; issuing power up commands",
				boardsToAllocate.size(), jobId);
		return setPower(sql, jobId, PowerState.ON, READY);
	}

	@Override
	public boolean setPower(int jobId, PowerState power, JobState targetState)
			throws SQLException {
		boolean updated = db.execute(conn -> {
			try (PowerSQL sql = new PowerSQL(conn)) {
				return setPower(sql, jobId, power, targetState);
			}
		});
		if (updated) {
			epochs.nextMachineEpoch();
			epochs.nextJobsEpoch();
		}
		return updated;
	}

	/**
	 * Issue a request to change the power for the boards of a job.
	 *
	 * @param sql
	 *            How to talk to the DB
	 * @param jobId
	 *            The job in question
	 * @param power
	 *            The power state to switch to
	 * @param targetState
	 *            The state to put the job in afterwards
	 * @return Whether any changes are pending
	 * @throws SQLException
	 *             If DB access fails
	 */
	private boolean setPower(PowerSQL sql, int jobId, PowerState power,
			JobState targetState) throws SQLException {
		JobState sourceState = sql.getJobState.call1(jobId).get()
				.getEnum("job_state", JobState.class);
		List<Integer> boards = rowsAsList(sql.getJobBoards.call(jobId),
				row -> row.getInteger("board_id"));
		if (boards.isEmpty()) {
			if (targetState == DESTROYED) {
				log.info("no boards for {} in destroy", jobId);
			}
			return false;
		}

		// Number of changes pending, one per board
		int numPending = 0;

		if (power == PowerState.ON) {
			/*
			 * This is a bit of a trickier case, as we need to say which links
			 * are to be switched on or, more particularly, which are to be
			 * switched off because they are links to boards that are not
			 * allocated to the job. Off-board links are shut off by default.
			 */
			// TODO Combine getPerimeter and issuePowerChange
			Map<Integer, EnumSet<Direction>> perimeterLinks = new HashMap<>();
			for (Row row : sql.getPerimeter.call(jobId)) {
				perimeterLinks
						.computeIfAbsent(row.getInt("board_id"),
								k -> EnumSet.noneOf(Direction.class))
						.add(row.getEnum("direction", Direction.class));
			}

			for (int boardId : boards) {
				EnumSet<Direction> toChange =
						perimeterLinks.getOrDefault(boardId, NO_PERIMETER);
				numPending += sql.issuePowerChange.call(jobId, boardId,
						sourceState, targetState, true,
						!toChange.contains(Direction.N),
						!toChange.contains(Direction.E),
						!toChange.contains(Direction.SE),
						!toChange.contains(Direction.S),
						!toChange.contains(Direction.W),
						!toChange.contains(Direction.NW));
			}
		} else {
			// Powering off; all links switch to off so no perimeter check
			for (int boardId : boards) {
				numPending += sql.issuePowerChange.call(jobId, boardId,
						sourceState, targetState, false, false, false, false,
						false, false, false);
			}
		}

		if (targetState == DESTROYED) {
			log.info("num changes for {} in destroy: {}", jobId, numPending);
		}
		sql.setStatePending.call(
				targetState == DESTROYED ? DESTROYED
						: numPending > 0 ? POWER : targetState,
				numPending, jobId);

		return numPending > 0;
	}
}
