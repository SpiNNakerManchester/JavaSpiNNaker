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
import static java.lang.Math.sqrt;
import static java.util.Objects.requireNonNull;
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
import uk.ac.manchester.spinnaker.alloc.ServiceControl;

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

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private Epochs epochs;

	@Autowired
	private ServiceControl serviceControl;

	@PostConstruct
	private void setUp() throws SQLException {
		try (Connection conn = db.getConnection()) {
			DirInfo.load(conn);
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

		DimensionEstimate(int numBoards) {
			int numTriads = numBoards / TRIAD_DEPTH;
			if (numBoards % TRIAD_DEPTH > 0) {
				numTriads++;
			}
			width = (int) ceil(sqrt(numTriads));
			height = (int) ceil(numTriads / width);
			tolerance = (width * height * TRIAD_DEPTH) - numBoards;
			if (width < 1 || height < 1) {
				throw new IllegalArgumentException(
						"computed dimensions must be greater than zero");
			}
		}

		DimensionEstimate(int w, int h) {
			int numBoards = w * h;
			width = (int) ceil(w / HORIZONTAL_FACTOR);
			height = (int) ceil(h / VERTICAL_FACTOR);
			tolerance = (width * height * TRIAD_DEPTH) - numBoards;
			if (width < 1 || height < 1) {
				throw new IllegalArgumentException(
						"computed dimensions must be greater than zero");
			}
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
	boolean allocate(Connection conn, Row task) throws SQLException {
		int jobId = task.getInt("job_id");
		int machineId = task.getInt("machine_id");
		int maxDeadBoards = task.getInt("max_dead_boards");
		Integer numBoards = (Integer) task.getObject("num_boards");
		if (numBoards != null && numBoards > 0) {
			if (numBoards == 1) {
				return allocateOneBoard(conn, jobId, machineId);
			}
			DimensionEstimate estimate = new DimensionEstimate(numBoards);
			return allocateDimensions(conn, jobId, machineId, estimate, maxDeadBoards);
		}

		Integer width = (Integer) task.getObject("width");
		Integer height = (Integer) task.getObject("height");
		if (width != null && height != null && width > 0 && height > 0) {
			if (height == 1 && width == 1) {
				return allocateOneBoard(conn, jobId, machineId);
			}
			DimensionEstimate estimate = new DimensionEstimate(width, height);
			return allocateDimensions(conn, jobId, machineId, estimate,
					maxDeadBoards);
		}

		Integer x = (Integer) task.getObject("x");
		Integer y = (Integer) task.getObject("y");
		Integer z = (Integer) task.getObject("z");
		if (x != null && y != null && z != null) {
			// Ignores maxDeadBoards; is a single-board allocate
			return allocateCoords(conn, jobId, machineId, x, y, z);
		}

		log.warn("job {} could not be allocated; "
				+ "bad request will be cleared from queue", jobId);
		return true;
	}

	private boolean allocateOneBoard(Connection conn, int jobId, int machineId)
			throws SQLException {
		try (Query s = query(conn, FIND_FREE_BOARD)) {
			for (Row row : s.call(machineId)) {
				int x = row.getInt("x");
				int y = row.getInt("y");
				int z = row.getInt("z");
				if (setAllocation(conn, jobId, 1, 1, 1, machineId, x, y, z)) {
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
				int x = rs.getInt("x");
				int y = rs.getInt("y");
				int z = rs.getInt("z");
				if (estimate.width * estimate.height > 1) {
					/*
					 * Check that a minimum number of boards are reachable from
					 * the proposed root board. If the root board is isolated,
					 * we don't care if the rest of the allocation works because
					 * the rest of the toolchain won't cope.
					 */
					int size = connectedSize(getRectangles, machineId, x, y, z,
							estimate);
					if (size < estimate.width * estimate.height - tolerance) {
						continue;
					}
				}
				if (setAllocation(conn, jobId, estimate.width, estimate.height,
						TRIAD_DEPTH, machineId, x, y, z)) {
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
	 * @param x
	 *            Root logical X coordinate
	 * @param y
	 *            Root logical Y coordinate
	 * @param z
	 *            Root logical Z coordinate (usually, but not necessarily, 0)
	 * @param estimate
	 *            The planned allocation dimensions
	 * @return How many boards in the allocation are reachable.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	private int connectedSize(Query connected, int machineId, int x, int y,
			int z, DimensionEstimate estimate) throws SQLException {
		int size = -1;
		for (Row row : connected.call(machineId, x, y, z, estimate.width,
				estimate.height)) {
			size = row.getInt("connected_size");
		}
		return size;
	}

	private boolean allocateCoords(Connection conn, int jobId, int machineId,
			int x, int y, int z) throws SQLException {
		try (Query find = query(conn, findLocation)) {
			for (Row row : find.call(machineId, x, y, z)) {
				if (setAllocation(conn, jobId, 1, 1, 1, machineId,
						row.getInt("x"), row.getInt("y"), row.getInt("z"))) {
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
	 * @param width
	 *            Proposed rectangle size (X dimension)
	 * @param height
	 *            Proposed rectangle size (Y dimension)
	 * @param depth
	 *            Proposed rectangle size (Z dimension)
	 * @param machineId
	 *            What machine are we allocating on
	 * @param rootX
	 *            Proposed root X coordinate
	 * @param rootY
	 *            Proposed root Y coordinate
	 * @param rootZ
	 *            Proposed root Z coordinate
	 * @return Whether we have successfully allocated (the allocation is stored
	 *         in the DB)
	 * @throws SQLException
	 *             If something goes wrong with talking to the DB
	 */
	private boolean setAllocation(Connection conn, int jobId, int width,
			int height, int depth, int machineId, int rootX, int rootY,
			int rootZ) throws SQLException {
		try (Query getConnectedBoardIDs = query(conn, getConnectedBoards);
				Update allocBoard = update(conn, ALLOCATE_BOARDS_BOARD);
				Update allocJob = update(conn, ALLOCATE_BOARDS_JOB)) {
			// Messy without RETURNING, but SQLite 3.35 not yet supported
			List<Integer> boardsToAllocate = new ArrayList<>();
			for (Row row : getConnectedBoardIDs.call(machineId, rootX, rootY,
					rootZ, width, height, depth)) {
				int boardId = row.getInt("board_id");
				boardsToAllocate.add(boardId);
				allocBoard.call(jobId, boardId);
			}
			if (boardsToAllocate.isEmpty()) {
				return false;
			}

			allocJob.call(width, height, depth, boardsToAllocate.get(0), jobId);
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

	/**
	 * A mapping that says how to go from one board's coordinates (only the Z
	 * coordinate matters for this) to another when you move in a particular
	 * direction.
	 *
	 * <pre>
	 *  ___     ___     ___     ___
	 * / . \___/ . \___/ . \___/ . \___
	 * \___/ . \___/ . \___/ . \___/ . \
	 * /0,1\___/1,1\___/2,1\___/3,1\___/
	 * \___/ . \___/ . \___/ . \___/ . \___
	 *     \_2_/ . \___/ . \___/ . \___/ . \
	 *     /0,0\_1_/1,0\___/2,0\___/3,0\___/
	 *     \_0_/   \___/   \___/   \___/
	 * </pre>
	 *
	 * Bear in mind that 0,1,0 is <em>actually</em> 12 chips vertically and 0
	 * chips horizontally offset from 0,0,0; the hexagons are actually a
	 * distorted shape. This is closer:
	 * <pre>
	 *    __     __     __     __
	 *   /  |   /  |   /  |   /  |
	 *  /   |__/   |__/   |__/   |__
	 *  | 2 /  | 2 /  | 2 /  | 2 /  |
	 *  |__/   |__/   |__/   |__/   |
	 *  /  | 1 /  | 1 /  | 1 /  | 1 /
	 * /0,1|__/1,1|__/2,1|__/3,1|__/
	 * | 0 /  | 0 /  | 0 /  | 0 /  |
	 * |__/   |__/   |__/   |__/   |__
	 *    | 2 /  | 2 /  | 2 /  | 2 /  |
	 *    |__/   |__/   |__/   |__/   |
	 *    /  | 1 /  | 1 /  | 1 /  | 1 /
	 *   /0,0|__/1,0|__/2,0|__/3,0|__/
	 *   | 0 /  | 0 /  | 0 /  | 0 /
	 *   |__/   |__/   |__/   |__/
	 * </pre>
	 *
	 * @author Donal Fellows
	 */
	public final static class DirInfo {
		/**
		 * When your Z coordinate is this.
		 */
		public final int z;

		/** When you are moving in this direction. */
		public final Direction dir;

		/** Change your X coordinate by this. */
		public final int dx;

		/** Change your Y coordinate by this. */
		public final int dy;

		/** Change your Z coordinate by this. */
		public final int dz;

		private static final Map<Integer, Map<Direction, DirInfo>> map =
				new HashMap<>();

		private DirInfo(int z, Direction d, int dx, int dy, int dz) {
			this.z = z;
			this.dir = requireNonNull(d);
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;

			map.computeIfAbsent(z, key -> new HashMap<>()).put(d, this);
		}

		/**
		 * Obtain the correct motion information given a starting point and a
		 * direction.
		 *
		 * @param z
		 *            The starting Z coordinate. (Motions are independent of X
		 *            and Y.) Must be in range {@code 0..2}.
		 * @param direction
		 *            The direction to move in.
		 * @return How to move.
		 */
		public static DirInfo get(int z, Direction direction) {
			return map.get(z).get(direction);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof DirInfo)) {
				return false;
			}
			DirInfo di = (DirInfo) o;
			return z == di.z && dir == di.dir;
		}

		@Override
		public int hashCode() {
			return z ^ dir.hashCode();
		}

		private static void load(Connection conn) throws SQLException {
			if (map.isEmpty()) {
				try (Query di = query(conn, LOAD_DIR_INFO)) {
					for (Row row : di.call()) {
						new DirInfo(row.getInt("z"),
								row.getEnum("direction", Direction.class),
								row.getInt("dx"), row.getInt("dy"),
								row.getInt("dz"));
					}
				}
				log.info("created {} DirInfo instances",
						map.values().stream().mapToInt(Map::size).sum());
			}
		}
	}
}
