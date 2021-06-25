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
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.allocator.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.allocator.JobState.POWER;
import static uk.ac.manchester.spinnaker.alloc.allocator.JobState.READY;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;

@Component
public class AllocatorTask extends SQLQueries implements PowerController {
	/**
	 * Time, in milliseconds, between runs of {@link #allocate()}. (5s)
	 */
	public static final long INTER_ALLOCATE_DELAY = 5000;

	/**
	 * Time, in milliseconds, between runs of {@link #cleanUp()}. (30s)
	 */
	private static final long INTER_DESTROY_DELAY = 30000;

	/** Triads contain three boards. */
	private static final int TRIAD_DEPTH = 3;

	/**
	 * @see #setPower(Connection,int,PowerState)
	 */
	private static final EnumSet<Direction> NO_PERIMETER =
			EnumSet.noneOf(Direction.class);

	private static final Logger log = getLogger(AllocatorTask.class);

	private static final Rectangle ONE_BOARD = new Rectangle(1, 1, 1);

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private Epochs epochs;

	@Autowired
	private ServiceMasterControl serviceControl;

	@PostConstruct
	private void setUp() throws SQLException {
		try (Connection conn = db.getConnection()) {
			DirInfo.load(conn);
		}
	}

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

		private TriadCoords(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		TriadCoords(Row row) throws SQLException {
			this.x = row.getInt("x");
			this.y = row.getInt("y");
			this.z = row.getInt("z");
		}

		static TriadCoords get(Row row) throws SQLException {
			Integer x = (Integer) row.getObject("x");
			Integer y = (Integer) row.getObject("y");
			Integer z = (Integer) row.getObject("z");
			if (x == null || y == null || z == null) {
				return null;
			}
			return new TriadCoords(x, y, z);
		}
	}

	/**
	 * Allocate all current requests for resources.
	 *
	 * @throws SQLException
	 *             If anything goes wrong at the DB level
	 */
	@Scheduled(fixedDelay = INTER_ALLOCATE_DELAY)
	public void allocate() throws SQLException {
		if (serviceControl.isPaused()) {
			return;
		}

		if (db.execute(this::allocate)) {
			epochs.nextJobsEpoch();
		}
	}

	private boolean allocate(Connection conn) throws SQLException {
		try (Query getTasks = query(conn, GET_TASKS);
				Update delete = update(conn, DELETE_TASK)) {
			boolean changed = false;
			for (Row row : getTasks.call()) {
				int id = row.getInt("req_id");
				boolean handled = true;
				try {
					handled = allocate(conn, row);
					/*
					 * NB: having an exception counts as handled; we will nuke
					 * the task.
					 */
				} finally {
					if (handled) {
						changed = delete.call(id) > 0;
					}
				}
			}
			return changed;
		}
	}

	@Scheduled(fixedDelay = INTER_DESTROY_DELAY)
	public void expireJobs() throws SQLException {
		if (serviceControl.isPaused()) {
			return;
		}

		if (db.execute(this::expireJobs)) {
			epochs.nextJobsEpoch();
			epochs.nextMachineEpoch();
		}
	}

	private boolean expireJobs(Connection conn) throws SQLException {
		try (Query find = query(conn, FIND_EXPIRED_JOBS)) {
			boolean changed = false;
			List<Integer> toKill = new ArrayList<>();
			for (Row row : find.call()) {
				toKill.add(row.getInt("job_id"));
			}
			for (int id : toKill) {
				changed |= destroyJob(conn, id, "keepalive expired");
			}
			return changed;
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
		try (Update mark = update(conn, DESTROY_JOB);
				Update killAlloc = update(conn, KILL_JOB_ALLOC_TASK);
				Update killPending = update(conn, KILL_JOB_PENDING)) {
			setPower(conn, id, PowerState.OFF, DESTROYED);
			boolean success = mark.call(reason, id) > 0;
			if (success) {
				killAlloc.call(id);
				killPending.call(id);
			}
			return success;
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
	private static class DimensionEstimate {
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
	 * @param conn
	 *            The database connection
	 * @param task
	 *            The task (as a result set).
	 * @return {@code true} if a decision has been taken about the task, or
	 *         {@code false} if the task is to have the allocator run again in
	 *         the next schedule slot.
	 * @throws SQLException
	 *             If anything goes wrong at the DB level
	 */
	private boolean allocate(Connection conn, Row task) throws SQLException {
		int jobId = task.getInt("job_id");
		int machineId = task.getInt("machine_id");
		Rectangle max = new Rectangle(task.getInt("max_width"),
				task.getInt("max_height"), TRIAD_DEPTH);
		int maxDeadBoards = task.getInt("max_dead_boards");
		Integer numBoards = (Integer) task.getObject("num_boards");
		if (numBoards != null && numBoards > 0) {
			if (numBoards == 1) {
				return allocateOneBoard(conn, jobId, machineId);
			}
			DimensionEstimate estimate = new DimensionEstimate(numBoards, max);
			return allocateDimensions(conn, jobId, machineId, estimate,
					maxDeadBoards);
		}

		Integer width = (Integer) task.getObject("width");
		Integer height = (Integer) task.getObject("height");
		if (width != null && height != null && width > 0 && height > 0) {
			if (height == 1 && width == 1) {
				return allocateOneBoard(conn, jobId, machineId);
			}
			DimensionEstimate estimate =
					new DimensionEstimate(width, height, max);
			return allocateDimensions(conn, jobId, machineId, estimate,
					maxDeadBoards);
		}

		TriadCoords root = TriadCoords.get(task);
		if (root != null) {
			// Ignores maxDeadBoards; is a single-board allocate
			return allocateCoords(conn, jobId, machineId, root);
		}

		log.warn("job {} could not be allocated; "
				+ "bad request will be cleared from queue", jobId);
		return true;
	}

	private boolean allocateOneBoard(Connection conn, int jobId, int machineId)
			throws SQLException {
		try (Query s = query(conn, FIND_FREE_BOARD)) {
			for (Row row : s.call(machineId)) {
				TriadCoords root = new TriadCoords(row);
				if (setAllocation(conn, jobId, ONE_BOARD, machineId, root)) {
					return true;
				}
			}
			return false;
		}
	}

	private boolean allocateDimensions(Connection conn, int jobId,
			int machineId, DimensionEstimate estimate, int userMaxDead)
			throws SQLException {
		int tolerance = userMaxDead + estimate.tolerance;
		try (Query getRectangles = query(conn, findRectangle);
				Query connected = query(conn, countConnected)) {
			for (Row rs : getRectangles.call(estimate.width, estimate.height,
					machineId, tolerance)) {
				TriadCoords root = new TriadCoords(rs);
				if (estimate.width * estimate.height > 1) {
					/*
					 * Check that a minimum number of boards are reachable from
					 * the proposed root board. If the root board is isolated,
					 * we don't care if the rest of the allocation works because
					 * the rest of the toolchain won't cope.
					 */
					int size = connectedSize(getRectangles, machineId, root,
							estimate);
					if (size < estimate.width * estimate.height - tolerance) {
						continue;
					}
				}
				if (setAllocation(conn, jobId, estimate.getRect(), machineId,
						root)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Find the number of boards that are reachable from the proposed root
	 * board.
	 *
	 * @param connected
	 *            The query prepared from {@link #countConnected}
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
	private int connectedSize(Query connected, int machineId, TriadCoords root,
			DimensionEstimate estimate) throws SQLException {
		int size = -1;
		for (Row row : connected.call(machineId, root.x, root.y, root.z,
				estimate.width, estimate.height)) {
			size = row.getInt("connected_size");
		}
		return size;
	}

	private boolean allocateCoords(Connection conn, int jobId, int machineId,
			TriadCoords root) throws SQLException {
		try (Query find = query(conn, findLocation)) {
			for (Row row : find.call(machineId, root.x, root.y, root.z)) {
				if (setAllocation(conn, jobId, ONE_BOARD, machineId,
						new TriadCoords(row))) {
					return true;
				}
			}
			return false;
		}
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
	 * @param conn
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
	private boolean setAllocation(Connection conn, int jobId, Rectangle rect,
			int machineId, TriadCoords root)
			throws SQLException {
		try (Query getConnectedBoardIDs = query(conn, getConnectedBoards);
				Update allocBoard = update(conn, ALLOCATE_BOARDS_BOARD);
				Update allocJob = update(conn, ALLOCATE_BOARDS_JOB)) {
			// Messy without RETURNING, but SQLite 3.35 not yet supported
			List<Integer> boardsToAllocate = new ArrayList<>();
			for (Row row : getConnectedBoardIDs.call(machineId, root.x, root.y,
					root.z, rect.width, rect.height, rect.depth)) {
				int boardId = row.getInt("board_id");
				boardsToAllocate.add(boardId);
				allocBoard.call(jobId, boardId);
			}
			if (boardsToAllocate.isEmpty()) {
				return false;
			}

			allocJob.call(rect.width, rect.height, rect.depth,
					boardsToAllocate.get(0), jobId);
			return setPower(conn, jobId, PowerState.ON, READY);
		}
	}

	@Override
	public boolean setPower(int jobId, PowerState power, JobState targetState)
			throws SQLException {
		boolean updated =
				db.execute(conn -> setPower(conn, jobId, power, targetState));
		if (updated) {
			epochs.nextMachineEpoch();
			epochs.nextJobsEpoch();
		}
		return updated;
	}

	private boolean setPower(Connection conn, int jobId, PowerState power,
			JobState targetState) throws SQLException {
		try (Query getJobBoards = query(conn, GET_JOB_BOARDS);
				Query getPerim = query(conn, getPerimeterLinks);
				Update issueChange = update(conn, issueChangeForJob);
				Update setStatePending = update(conn, SET_STATE_PENDING)) {
			List<Integer> boards = new ArrayList<>();
			for (Row row : getJobBoards.call(jobId)) {
				boards.add(row.getInt("board_id"));
			}
			if (boards.isEmpty()) {
				return false;
			}

			// Number of changes pending, one per board
			int numPending = 0;

			if (power == PowerState.ON) {
				/*
				 * This is a bit of a trickier case, as we need to say which
				 * links are to be switched on or, more particularly, which are
				 * to be switched off because they are links to boards that are
				 * not allocated to the job. Off-board links are shut off by
				 * default.
				 */
				Map<Integer, EnumSet<Direction>> perimeterLinks =
						new HashMap<>();
				for (Row row : getPerim.call(jobId)) {
					perimeterLinks
							.computeIfAbsent(row.getInt("board_id"),
									k -> EnumSet.noneOf(Direction.class))
							.add(row.getEnum("direction", Direction.class));
				}

				for (int boardId : boards) {
					EnumSet<Direction> toChange =
							perimeterLinks.getOrDefault(boardId, NO_PERIMETER);
					numPending += issueChange.call(jobId, boardId, true,
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
					numPending += issueChange.call(jobId, boardId, false, false,
							false, false, false, false, false);
				}
			}

			setStatePending.call(
					targetState == DESTROYED ? DESTROYED
							: numPending > 0 ? POWER : targetState,
					numPending, jobId);

			return numPending > 0;
		}
	}
}
