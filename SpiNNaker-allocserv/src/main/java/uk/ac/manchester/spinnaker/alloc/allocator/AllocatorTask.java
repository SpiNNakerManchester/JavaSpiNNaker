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
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.db.Row.enumerate;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.isBusy;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.POWER;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.READY;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.OFF;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.ON;
import static uk.ac.manchester.spinnaker.machine.board.TriadCoords.TRIAD_DEPTH;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AllocatorProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.HistoricalDataProperties;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.machine.board.BoardDirection;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * The allocation engine. Allocations are performed by running suitable
 * (non-trivial) SQL queries on a periodic basis, putting jobs that cannot be
 * allocated back on the queue for a later attempt (and increasing their
 * priority when it does so). This class is also responsible for destroying jobs
 * that are not kept alive sufficiently frequently, and eventually migrating
 * records of dead jobs to long-term storage ("tombstoning").
 */
@Service
public class AllocatorTask extends DatabaseAwareBean
		implements PowerController {
	/**
	 * @see #setPower(Connection,int,PowerState)
	 */
	private static final EnumSet<BoardDirection> NO_PERIMETER =
			EnumSet.noneOf(BoardDirection.class);

	private static final Logger log = getLogger(AllocatorTask.class);

	private static final Rectangle ONE_BOARD = new Rectangle(1, 1, 1);

	/**
	 * Maximum number of jobs that we actually run the quota check for. Used
	 * because we are reusing a query, and we'll probably never have that many
	 * live jobs even on the big machine.
	 */
	private static final Integer NUMBER_OF_JOBS_TO_QUOTA_CHECK = 100000;

	@Autowired
	private Epochs epochs;

	@Autowired
	private ServiceMasterControl serviceControl;

	@Autowired
	private QuotaManager quotaManager;

	@Autowired
	private AllocatorProperties allocProps;

	@Autowired
	private HistoricalDataProperties historyProps;

	@Autowired
	private ProxyRememberer rememberer;

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

		private Rectangle(int width, int height, int depth) {
			this.width = width;
			this.height = height;
			this.depth = depth;
		}

		private Rectangle(Row row) {
			this(row.getInt("max_width"), row.getInt("max_height"),
					TRIAD_DEPTH);
		}

		@Override
		public String toString() {
			return format("%dx%dx%d", width, height, depth);
		}

		public int getArea() {
			return width * height * depth;
		}
	}

	/**
	 * Helper class representing the logical coordinates of a board.
	 *
	 * @author Donal Fellows
	 */
	private static final class TriadCoords {
		final int x;

		final int y;

		final int z;

		private TriadCoords(Row row) {
			this.x = row.getInt("x");
			this.y = row.getInt("y");
			this.z = row.getInt("z");
		}

		@Override
		public String toString() {
			return format("[%d,%d,%d]", x, y, z);
		}
	}

	/**
	 * Allocate all current requests for resources.
	 */
	@Scheduled(fixedDelayString = "#{allocatorProperties.period}")
	public void allocate() {
		if (serviceControl.isPaused()) {
			return;
		}

		try {
			if (execute(this::allocate)) {
				log.debug("advancing job epoch");
				epochs.nextJobsEpoch();
			}
		} catch (DataAccessException e) {
			if (isBusy(e)) {
				log.info("database is busy; "
						+ "will try allocation processing later");
				return;
			}
			throw e;
		}
	}

	/** Encapsulates the queries and updates used in power control. */
	private class PowerSQL extends AbstractSQL {
		/** Get basic information about a specific job. */
		private final Query getJobState;

		/** Get what boards are allocated to a job (that is queued or ready). */
		private final Query getJobBoards;

		/**
		 * Get the links on the perimeter of the allocation to a job. The
		 * perimeter is defined as being the links between a board that is part
		 * of the allocation and a board that is not; it's <em>not</em> a
		 * geometric definition, but rather a relational algebraic one.
		 */
		private final Query getPerimeter;

		/** Create a request to change the power status of a board. */
		private final Update issuePowerChange;

		/** Set the state and number of pending changes for a job. */
		private final Update setStatePending;

		PowerSQL(Connection conn) {
			super(conn);
			getJobState = conn.query(GET_JOB);
			getJobBoards = conn.query(GET_JOB_BOARDS);
			getPerimeter = conn.query(getPerimeterLinks);
			issuePowerChange = conn.update(issueChangeForJob);
			setStatePending = conn.update(SET_STATE_PENDING);
		}

		@Override
		public void close() {
			getJobState.close();
			getJobBoards.close();
			getPerimeter.close();
			issuePowerChange.close();
			setStatePending.close();
		}
	}

	/** Encapsulates the queries and updates used in allocation. */
	@UsedInJavadocOnly(SQLQueries.class)
	private final class AllocSQL extends PowerSQL {
		/** Increases the importance of a job. */
		private final Update bumpImportance;

		/** Get the list of allocation tasks for jobs in a given state. */
		private final Query getTasks;

		/** Delete an allocation task. */
		private final Update delete;

		/** Find a single free board. */
		private final Query findFreeBoard;

		/**
		 * Find a rectangle of triads of boards that may be allocated.
		 *
		 * @see SQLQueries#FIND_FREE_BOARD
		 */
		private final Query getRectangles;

		/**
		 * Find a rectangle of triads of boards that may be allocated rooted at
		 * a particular board.
		 *
		 * @see SQLQueries#FIND_FREE_BOARD_AT
		 */
		private final Query getRectangleAt;

		/**
		 * Count the number of <em>connected</em> boards (i.e., have at least
		 * one path over enabled links to the root board of the allocation)
		 * within a rectangle of triads. The triads are taken as being full
		 * depth.
		 */
		private final Query countConnectedBoards;

		/**
		 * Find an allocatable board with a specific board ID. (This will have
		 * been previously converted from some other form of board coordinates.)
		 */
		private final Query findSpecificBoard;

		/**
		 * Get the set of boards at some coordinates within a triad rectangle
		 * that are connected (i.e., have at least one path over enableable
		 * links) to the root board.
		 */
		private final Query getConnectedBoardIDs;

		/** Tell a board that it is allocated. */
		private final Update allocBoard;

		/** Tell a job that it is allocated. Doesn't set the state. */
		private final Update allocJob;

		AllocSQL(Connection conn) {
			super(conn);
			bumpImportance = conn.update(BUMP_IMPORTANCE);
			getTasks = conn.query(getAllocationTasks);
			delete = conn.update(DELETE_TASK);
			findFreeBoard = conn.query(FIND_FREE_BOARD);
			getRectangles = conn.query(findRectangle);
			getRectangleAt = conn.query(findRectangleAt);
			countConnectedBoards = conn.query(countConnected);
			findSpecificBoard = conn.query(findLocation);
			getConnectedBoardIDs = conn.query(getConnectedBoards);
			allocBoard = conn.update(ALLOCATE_BOARDS_BOARD);
			allocJob = conn.update(ALLOCATE_BOARDS_JOB);
		}

		@Override
		public void close() {
			super.close();
			bumpImportance.close();
			getTasks.close();
			delete.close();
			findFreeBoard.close();
			getRectangles.close();
			getRectangleAt.close();
			countConnectedBoards.close();
			findSpecificBoard.close();
			getConnectedBoardIDs.close();
			allocBoard.close();
			allocJob.close();
		}
	}

	/** Encapsulates the queries and updates used in deletion. */
	private final class DestroySQL extends PowerSQL {
		/** Get basic information about a specific job. */
		private final Query getJob;

		/** Mark a job as dead. */
		private final Update markAsDestroyed;

		/** Delete a request to allocate resources for a job. */
		private final Update killAlloc;

		/**
		 * Delete a request to change the power of boards allocated to a job.
		 */
		private final Update killPending;

		DestroySQL(Connection conn) {
			super(conn);
			getJob = conn.query(GET_JOB);
			markAsDestroyed = conn.update(DESTROY_JOB);
			killAlloc = conn.update(KILL_JOB_ALLOC_TASK);
			killPending = conn.update(KILL_JOB_PENDING);
		}

		@Override
		public void close() {
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
	 */
	private boolean allocate(Connection conn) {
		try (var sql = new AllocSQL(conn)) {
			int maxImportance = -1;
			boolean changed = false;
			for (var row : sql.getTasks.call(QUEUED)) {
				int id = row.getInt("req_id");
				int importance = row.getInt("importance");
				if (importance > maxImportance) {
					maxImportance = importance;
				} else if (importance < maxImportance
						- allocProps.getImportanceSpan()) {
					// Too much of a span
					continue;
				}
				var handled = allocate(sql, row);
				changed |= handled;
				log.debug("allocate for {}: {}", id, handled);
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
	 */
	@Scheduled(fixedDelayString = "#{keepaliveProperties.expiryPeriod}")
	public void expireJobs() {
		if (serviceControl.isPaused()) {
			return;
		}

		try {
			if (execute(this::expireJobs)) {
				epochs.nextJobsEpoch();
				epochs.nextMachineEpoch();
			}
		} catch (DataAccessException e) {
			if (isBusy(e)) {
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
	 */
	private boolean expireJobs(Connection conn) {
		boolean changed = false;
		try (var find = conn.query(FIND_EXPIRED_JOBS)) {
			var toKill = find.call().map(integer("job_id")).toList();
			for (var id : toKill) {
				changed |= destroyJob(conn, id, "keepalive expired");
			}
		}
		try (var find = conn.query(GET_LIVE_JOB_IDS)) {
			var toKill = new ArrayList<Integer>();
			for (var row : find.call(NUMBER_OF_JOBS_TO_QUOTA_CHECK, 0)) {
				int jobId = row.getInt("job_id");
				if (!quotaManager.mayLetJobContinue(jobId)) {
					toKill.add(jobId);
				}
			}
			for (var id : toKill) {
				changed |= destroyJob(conn, id, "quota exceeded");
			}
		}
		return changed;
	}

	/**
	 * Migrates long dead jobs to the historical data DB.
	 */
	@Scheduled(cron = "#{historyProperties.schedule}")
	public void tombstone() {
		if (serviceControl.isPaused()) {
			return;
		}

		try (var conn = getConnection()) {
			var c = tombstone(conn);
			log.info("tombstoning completed: "
					+ "moved {} job records and {} allocation records",
					c.numJobs(), c.numAllocs());
		} catch (DataAccessException e) {
			if (isBusy(e)) {
				log.info("database is busy; "
						+ "will try job tombstone processing at future date");
				return;
			}
			throw e;
		}
	}

	/**
	 * Describes what the first stage of the tombstoner has copied.
	 */
	static final class Copied {
		private final List<Integer> jobIds;

		private final List<Integer> allocIds;

		private Copied(List<Integer> jobIds, List<Integer> allocIds) {
			this.jobIds = jobIds;
			this.allocIds = allocIds;
		}

		private Stream<Integer> allocs() {
			return allocIds.stream().filter(Objects::nonNull);
		}

		private Stream<Integer> jobs() {
			return jobIds.stream().filter(Objects::nonNull);
		}

		/**
		 * @return The number of job records copied over to the historical
		 *         database.
		 */
		int numJobs() {
			return jobIds.size();
		}

		/**
		 * @return The number of board allocation records copied over to the
		 *         historical database.
		 */
		int numAllocs() {
			return allocIds.size();
		}
	}

	/**
	 * Implementation of {@link #tombstone()}. This is done as two transactions
	 * to help manage the amount of locking (especially multi-DB locking);
	 * nothing else ought to be updating any of these jobs at the time this task
	 * usually runs, but we'll still try to keep things minimally locked.
	 *
	 * @param conn
	 *            The DB connection
	 * @return Description of the tombstoned IDs
	 */
	private Copied tombstone(Connection conn) {
		try (var copyJobs = conn.query(copyJobsToHistoricalData);
				var copyAllocs = conn.query(copyAllocsToHistoricalData);
				var deleteJobs = conn.update(DELETE_JOB_RECORD);
				var deleteAllocs = conn.update(DELETE_ALLOC_RECORD)) {
			var grace = historyProps.getGracePeriod();
			var copied = conn.transaction(() -> new Copied(
					copyJobs.call(grace).map(integer("job_id")).toList(),
					copyAllocs.call(grace).map(integer("alloc_id")).toList()));
			conn.transaction(() -> {
				copied.allocs().forEach(deleteAllocs::call);
				copied.jobs().forEach(deleteJobs::call);
			});
			return copied;
		}
	}

	@Override
	public void destroyJob(int id, String reason) {
		if (execute(conn -> destroyJob(conn, id, reason))) {
			epochs.nextJobsEpoch();
			epochs.nextMachineEpoch();
		}
	}

	/**
	 * Destroy a job.
	 *
	 * @param conn
	 *            How to talk to the DB
	 * @param id
	 *            The ID of the job
	 * @param reason
	 *            Why is the job being destroyed.
	 * @return Whether the job was destroyed.
	 */
	private boolean destroyJob(Connection conn, int id, String reason) {
		JobLifecycle.log.info("destroying job {} \"{}\"", id, reason);
		try (var sql = new DestroySQL(conn)) {
			if (sql.getJob.call1(id).map(enumerate("job_state", JobState.class))
					.orElse(DESTROYED) == DESTROYED) {
				/*
				 * Don't do anything if the job doesn't exist or is already
				 * destroyed
				 */
				return false;
			}
			sql.killPending.call(id);
			// Inserts into pending_changes; these run after job is dead
			setPower(sql, id, OFF, DESTROYED);
			sql.killAlloc.call(id);
			return sql.markAsDestroyed.call(reason, id) > 0;
		} finally {
			rememberer.killProxies(id);
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
	 */
	private boolean allocate(AllocSQL sql, Row task) {
		int jobId = task.getInt("job_id");
		int machineId = task.getInt("machine_id");
		var max = new Rectangle(task);
		int maxDeadBoards = task.getInt("max_dead_boards");
		var numBoards = task.getInteger("num_boards");
		if (nonNull(numBoards) && numBoards > 0) {
			if (numBoards == 1) {
				return allocateOneBoard(sql, jobId, machineId);
			}
			var estimate = new DimensionEstimate(numBoards, max);
			return allocateDimensions(sql, jobId, machineId, estimate,
					maxDeadBoards);
		}

		var width = task.getInteger("width");
		var height = task.getInteger("height");
		var root = task.getInteger("board_id");

		if (nonNull(width) && nonNull(height) && nonNull(root)) {
			return allocateTriadsAt(sql, jobId, machineId, root, width, height,
					maxDeadBoards);
		}

		if (nonNull(width) && nonNull(height) && width > 0 && height > 0) {
			if (height == 1 && width == 1) {
				return allocateOneBoard(sql, jobId, machineId);
			}
			var estimate = new DimensionEstimate(width, height, max);
			log.debug(
					"resolved request for {}x{} boards to {}x{} triads "
							+ "with tolerance {}",
					width, height, estimate.width, estimate.height,
					estimate.tolerance);
			return allocateDimensions(sql, jobId, machineId, estimate,
					maxDeadBoards);
		}

		if (nonNull(root)) {
			// Ignores maxDeadBoards; is a single-board allocate
			return allocateBoard(sql, jobId, machineId, root);
		}

		log.warn("job {} could not be allocated; "
				+ "bad request will be cleared from queue", jobId);
		return true;
	}

	private boolean allocateOneBoard(AllocSQL sql, int jobId, int machineId) {
		// This is simplified; no subsidiary searching needed
		return sql.findFreeBoard
				.call1(machineId).map(row -> setAllocation(sql, jobId,
						ONE_BOARD, machineId, new TriadCoords(row)))
				.orElse(false);
	}

	private boolean allocateDimensions(AllocSQL sql, int jobId, int machineId,
			DimensionEstimate estimate, int userMaxDead) {
		int tolerance = userMaxDead + estimate.tolerance;
		int minArea =
				estimate.width * estimate.height * TRIAD_DEPTH - tolerance;
		for (var root : sql.getRectangles
				.call(estimate.width, estimate.height, machineId, tolerance)
				.map(TriadCoords::new)) {
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
	 * @param width
	 *            The width of the planned allocation, in triads
	 * @param height
	 *            The width of the planned allocation, in triads
	 * @return How many boards in the allocation are reachable.
	 */
	private int connectedSize(AllocSQL sql, int machineId, TriadCoords root,
			int width, int height) {
		return sql.countConnectedBoards
				.call1(machineId, root.x, root.y, width, height)
				.map(integer("connected_size")).orElse(-1);
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
	 * @param rect
	 *            The requested allocation dimensions
	 * @return How many boards in the allocation are reachable.
	 */
	private int connectedSize(AllocSQL sql, int machineId, TriadCoords root,
			Rectangle rect) {
		return connectedSize(sql, machineId, root, rect.width, rect.height);
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
	 */
	private int connectedSize(AllocSQL sql, int machineId, TriadCoords root,
			DimensionEstimate estimate) {
		return connectedSize(sql, machineId, root, estimate.width,
				estimate.height);
	}

	private boolean allocateBoard(AllocSQL sql, int jobId, int machineId,
			int boardId) {
		return sql.findSpecificBoard
				.call1(machineId, boardId).map(row -> setAllocation(sql, jobId,
						ONE_BOARD, machineId, new TriadCoords(row)))
				.orElse(false);
	}

	private boolean allocateTriadsAt(AllocSQL sql, int jobId, int machineId,
			int rootId, int width, int height, int maxDeadBoards) {
		var rect = new Rectangle(width, height, TRIAD_DEPTH);
		return sql.getRectangleAt
				.call1(rootId, width, height, machineId, maxDeadBoards)
				.map(TriadCoords::new)
				.filter(root -> connectedSize(sql, machineId, root,
						rect) >= rect.getArea() - maxDeadBoards)
				.map(root -> setAllocation(sql, jobId, rect, machineId, root))
				.orElse(false);
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
	 */
	private boolean setAllocation(AllocSQL sql, int jobId, Rectangle rect,
			int machineId, TriadCoords root) {
		log.debug("performing allocation for {}: {}x{}x{} at {}:{}:{}", jobId,
				rect.width, rect.height, rect.depth, root.x, root.y, root.z);
		var boardsToAllocate = sql.getConnectedBoardIDs
				.call(machineId, root.x, root.y, root.z, rect.width,
						rect.height, rect.depth)
				.map(integer("board_id")).toList();
		if (boardsToAllocate.isEmpty()) {
			return false;
		}
		for (var boardId : boardsToAllocate) {
			sql.allocBoard.call(jobId, boardId);
		}

		sql.allocJob.call(rect.width, rect.height, rect.depth,
				boardsToAllocate.get(0), boardsToAllocate.size(), jobId);
		log.debug("allocated {} boards to {}; issuing power up commands",
				boardsToAllocate.size(), jobId);
		// Any proxies that existed are now defunct; user must make anew
		rememberer.killProxies(jobId);
		return setPower(sql, jobId, ON, READY);
	}

	@Override
	public boolean setPower(int jobId, PowerState power, JobState targetState) {
		boolean updated = execute(conn -> {
			try (var sql = new PowerSQL(conn)) {
				return setPower(sql, jobId, power, targetState);
			}
		});
		if (updated) {
			rememberer.killProxies(jobId);
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
	 */
	private boolean setPower(PowerSQL sql, int jobId, PowerState power,
			JobState targetState) {
		var sourceState = sql.getJobState.call1(jobId).orElseThrow()
				.getEnum("job_state", JobState.class);
		var boards = sql.getJobBoards.call(jobId).map(integer("board_id"))
				.toList();
		if (boards.isEmpty()) {
			if (targetState == DESTROYED) {
				log.debug("no boards for {} in destroy", jobId);
			}
			return false;
		}

		// Number of changes pending, one per board
		int numPending = 0;

		if (power == ON) {
			/*
			 * This is a bit of a trickier case, as we need to say which links
			 * are to be switched on or, more particularly, which are to be
			 * switched off because they are links to boards that are not
			 * allocated to the job. Off-board links are shut off by default.
			 */
			var perimeterLinks =
					new HashMap<Integer, EnumSet<BoardDirection>>();
			for (var row : sql.getPerimeter.call(jobId)) {
				perimeterLinks
						.computeIfAbsent(row.getInt("board_id"),
								k -> EnumSet.noneOf(BoardDirection.class))
						.add(row.getEnum("direction", BoardDirection.class));
			}

			for (var boardId : boards) {
				var toChange = perimeterLinks.getOrDefault(boardId,
						NO_PERIMETER);
				numPending += sql.issuePowerChange.call(jobId, boardId,
						sourceState, targetState, true,
						!toChange.contains(BoardDirection.N),
						!toChange.contains(BoardDirection.E),
						!toChange.contains(BoardDirection.SE),
						!toChange.contains(BoardDirection.S),
						!toChange.contains(BoardDirection.W),
						!toChange.contains(BoardDirection.NW));
			}
		} else {
			// Powering off; all links switch to off so no perimeter check
			for (var boardId : boards) {
				numPending += sql.issuePowerChange.call(jobId, boardId,
						sourceState, targetState, false, false, false, false,
						false, false, false);
			}
		}

		if (targetState == DESTROYED) {
			log.debug("num changes for {} in destroy: {}", jobId, numPending);
		}
		sql.setStatePending.call(
				targetState == DESTROYED ? DESTROYED
						: numPending > 0 ? POWER : targetState,
				numPending, jobId);

		return numPending > 0;
	}

	/** Operations for testing only. */
	@ForTestingOnly
	interface TestAPI {
		/**
		 * Allocate all current requests for resources.
		 *
		 * @return Whether any changes have been done
		 */
		boolean allocate();

		/**
		 * Destroy a job.
		 *
		 * @param id
		 *            The ID of the job
		 * @param reason
		 *            Why is the job being destroyed.
		 * @return Whether the job was destroyed.
		 */
		boolean destroyJob(int id, String reason);

		/**
		 * Destroy jobs that have missed their keepalive.
		 *
		 * @return Whether any jobs have been expired.
		 */
		boolean expireJobs();

		/**
		 * Implementation of {@link AllocatorTask#tombstone()}.
		 *
		 * @return Description of the tombstoned IDs
		 */
		Copied tombstone();
	}

	/**
	 * @param conn
	 *            The DB connection
	 * @return The test interface.
	 * @deprecated This interface is just for testing.
	 */
	@ForTestingOnly
	@Deprecated
	TestAPI getTestAPI(Connection conn) {
		ForTestingOnly.Utils.checkForTestClassOnStack();
		return new TestAPI() {
			@Override
			public boolean allocate() {
				return AllocatorTask.this.allocate(conn);
			}

			@Override
			public boolean destroyJob(int id, String reason) {
				return AllocatorTask.this.destroyJob(conn, id, reason);
			}

			@Override
			public boolean expireJobs() {
				return AllocatorTask.this.expireJobs(conn);
			}

			@Override
			public Copied tombstone() {
				return AllocatorTask.this.tombstone(conn);
			}
		};
	}
}
