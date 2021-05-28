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
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.allocator.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.allocator.JobState.POWER;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

@Component
public class AllocatorTask extends SQLQueries {
	/**
	 * Time, in milliseconds, between runs of {@link #allocate()}. (5s)
	 */
	public static final long INTER_ALLOCATE_DELAY = 5000;

	/**
	 * Time, in milliseconds, between runs of {@link #cleanUp()}. (30s)
	 */
	private static final long INTER_DESTROY_DELAY = 30000;

	private static final Logger log = getLogger(AllocatorTask.class);

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private Epochs epochs;

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
		try (Connection conn = db.getConnection()) {
			transaction(conn, () -> {
				boolean changed = false;
				try (Query getTasks = query(conn, GET_TASKS);
						Update delete = update(conn, DELETE_TASK)) {
					for (Row rs : getTasks.call()) {
						int id = rs.getInt("req_id");
						boolean handled = true;
						try {
							handled = allocate(conn, rs);
							/*
							 * NB: having an exception counts as handled; we
							 * will nuke the task.
							 */
						} finally {
							if (handled) {
								changed = delete.call(id) > 0;
							}
						}
					}
				}
				if (changed) {
					epochs.nextJobsEpoch();
				}
			});
		}
	}

	@Scheduled(fixedDelay = INTER_DESTROY_DELAY)
	public void cleanUp() throws SQLException {
		try (Connection conn = db.getConnection()) {
			transaction(conn, () -> {
				boolean changed = false;
				try (Query find = query(conn, FIND_EXPIRED_JOBS)) {
					List<Integer> toKill = new ArrayList<>();
					for (Row rs : find.call(DESTROYED)) {
						toKill.add(rs.getInt("job_id"));
					}
					for (int id : toKill) {
						changed |= destroyJob(conn, id);
					}
				}
				if (changed) {
					epochs.nextJobsEpoch();
					epochs.nextMachineEpoch();
				}
			});
		}
	}

	public void destroyJob(int id) throws SQLException {
		try (Connection conn = db.getConnection()) {
			transaction(conn, () -> {
				if (destroyJob(conn, id)) {
					epochs.nextJobsEpoch();
					epochs.nextMachineEpoch();
				}
			});
		}
	}

	private boolean destroyJob(Connection conn, int id) throws SQLException {
		try (Update mark = update(conn, MARK_JOB_DESTROYED);
				Update killAlloc = update(conn, KILL_JOB_ALLOC_TASK);
				Update killPending = update(conn, KILL_JOB_PENDING);
				Update issueOff = update(conn, ISSUE_BOARD_OFF_FOR_JOB)) {
			boolean success = mark.call(DESTROYED, id, DESTROYED) > 0;
			if (success) {
				killAlloc.call(id);
				killPending.call(id);
				issueOff.call(id, PowerState.OFF, id, PowerState.OFF);
			}
			return success;
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
			/*
			 * Convert a number of boards into a close-to-square size to search
			 * for. With the big machine's level of resources, that's good
			 * enough. For now.
			 */
			// FIXME update for triads
			double w = ceil(sqrt(numBoards));
			double h = ceil(numBoards / w);
			int tolerance = ((int) w) * ((int) h) - numBoards;
			return allocateDimensions(conn, jobId, machineId, (int) w, (int) h,
					maxDeadBoards + tolerance);
		}

		Integer width = (Integer) task.getObject("width");
		Integer height = (Integer) task.getObject("height");
		if (width != null && height != null && width > 0 && height > 0) {
			if (height == 1 && width == 1) {
				return allocateOneBoard(conn, jobId, machineId);
			}
			return allocateDimensions(conn, jobId, machineId, width, height,
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
			for (Row rs : s.call(machineId)) {
				int x = rs.getInt("x");
				int y = rs.getInt("y");
				int z = rs.getInt("z");
				return setAllocation(conn, jobId, 1, 1, 1, machineId, x, y, z);
			}
			return false;
		}
	}

	private boolean allocateDimensions(Connection conn, int jobId,
			int machineId, int width, int height, int tolerance)
			throws SQLException {
		// FIXME for triads
		try (Query s = query(conn, findRectangle)) {
			for (Row rs : s.call(width, height, machineId, tolerance)) {
				int x = rs.getInt("x");
				int y = rs.getInt("y");
				int z = rs.getInt("z");
				if (width * height > 1) {
					/*
					 * Check that a minimum number of boards are reachable from
					 * the proposed root board.
					 */
					int size = -1;
					try (Query connected = query(conn, countConnected)) {
						for (Row row : connected.call(machineId, x, y, z, width,
								height)) {
							size = row.getInt("connected_size");
						}
					}
					if (size < width * height - tolerance) {
						continue;
					}
				}
				return setAllocation(conn, jobId, width, height, 3, machineId,
						x, y, z);
			}
			return false;
		}
	}

	private boolean allocateCoords(Connection conn, int jobId, int machineId,
			int x, int y, int z) throws SQLException {
		try (Query find = query(conn, findLocation)) {
			for (Row row : find.call(machineId, x, y, z)) {
				return setAllocation(conn, jobId, 1, 1, 1, machineId,
						row.getInt("x"), row.getInt("y"), row.getInt("z"));
			}
			return false;
		}
	}

	private boolean setAllocation(Connection conn, int jobId, int width,
			int height, int depth, int machineId, int rootX, int rootY,
			int rootZ) throws SQLException {
		try (Query getBoardID = query(conn, GET_BOARD_BY_COORDS);
				Update allocBoard = update(conn, ALLOCATE_BOARDS_BOARD);
				Update allocJob = update(conn, ALLOCATE_BOARDS_JOB);
				Update issueChange = update(conn, issueChangeForJob);
				Update setNumPending = update(conn, SET_NUM_PENDING);
				Query getPerim = query(conn, getPerimeterLinks)) {
			List<Integer> boardsToAllocate = new ArrayList<>();
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					for (int z = 0; z < depth; z++) {
						int boardId = -1;
						for (Row row : getBoardID.call(machineId,
								x + rootX, y + rootY, z + rootZ)) {
							boardId = row.getInt("board_id");
							boardsToAllocate.add(boardId);
						}
						allocBoard.call(jobId, boardId);
					}
				}
			}
			if (boardsToAllocate.isEmpty()) {
				return false;
			}

			allocJob.call(width, height, POWER, boardsToAllocate.size(),
					boardsToAllocate.get(0), jobId);

			/*
			 * Get the perimeter of the job, which is the set of links that lead
			 * from a board in the allocation to a board outside.
			 */
			Map<Integer, EnumSet<Direction>> perimeterChanges = new HashMap<>();
			for (Row row : getPerim.call(jobId)) {
				perimeterChanges
						.computeIfAbsent(row.getInt("board_id"),
								k -> EnumSet.noneOf(Direction.class))
						.add(row.getEnum("direction", Direction.class));
			}

			// Number of changes pending, one per board
			int numPending = 0;

			EnumSet<Direction> noPerimeter = EnumSet.noneOf(Direction.class);
			for (int boardId : boardsToAllocate) {
				EnumSet<Direction> toChange =
						perimeterChanges.getOrDefault(boardId, noPerimeter);
				numPending += issueChange.call(jobId, machineId, boardId, true,
						toChange.contains(Direction.N),
						toChange.contains(Direction.NE),
						toChange.contains(Direction.SE),
						toChange.contains(Direction.S),
						toChange.contains(Direction.SW),
						toChange.contains(Direction.NW));
			}
			setNumPending.call(numPending, jobId);
			if (numPending > 0) {
				epochs.nextMachineEpoch();
			}
			return true;
		}
	}

	// @formatter:off
	/*
	void issuePowerRequest(Connection conn, int jobId, PowerState power)
			throws SQLException {
		try (Query jobBoards = query(conn,
				"SELECT board_id, x, y, board_power FROM boards "
						+ "WHERE allocated_job = ?");
				Update issueChange = update(conn, issueChangeForJob);
				Update setNumPending = update(conn, SET_NUM_PENDING)) {
			// TODO Cancel queued changes
			Map<Integer, ChipLocation> boardsInJob = new HashMap<>();
			Map<ChipLocation, Integer> boardsAtPlaces = new HashMap<>();
			List<Integer> toSwitch = new ArrayList<>();
			List<LinkDirections> perimeter = new ArrayList<>();
			for (Row row : jobBoards.call(jobId)) {
				int b = row.getInt("board_id");
				ChipLocation loc =
						new ChipLocation(row.getInt("x"), row.getInt("y"));
				boardsInJob.put(b, loc);
				boardsAtPlaces.put(loc, b);
				if (row.getEnum("board_power", PowerState.calss) != power) {
					toSwitch.add(row.getInt("board_id"));
				}
			}
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					changed |= allocBoard.call(jobId, machineId, x + rootX,
							y + rootY) > 0;
					if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
						perimeter.add(new LinkDirections(width, height, x, y));
					}
				}
			}
			int numPending = 0;
			for (LinkDirections ld : perimeter) {
				 // Issues instructions to reconfigure the perimeter of the
				 // allocated rectangle, incrementing numPending for each.
				numPending += issueChange.call(jobId, machineId, rootX + ld.x,
						rootY + ld.y, true, ld.n, ld.s, ld.e, ld.w, ld.nw,
						ld.se);
			}
			setNumPending.call(numPending, jobId);
			if (numPending > 0) {
				epochs.nextMachineEpoch();
			}
		}
	}
	 */
	// @formatter:on

	// @formatter:on
	/**
	 * Represents link directions of a board.
	 *
	 * <pre>
	 *      ___
	 *  ___/ a \___
	 * / f \___/ b \
	 * \___/ x \___/
	 * / e \___/ c \
	 * \___/ d \___/
	 *     \___/
	 * </pre>
	 *
	 * Note that this is tilted over with respect to reality; to
	 * <em>actually</em> go "vertically north", you have to go first {@link #N}
	 * and then {@link #NW}, taking two boards to actually go straight north (by
	 * an offset of 12 chips).
	 *
	 * @author Donal Fellows
	 */
	public enum Direction {
		/** Northward, from {@code x} to {@code a}. */
		N,
		/** Northeast, from {@code x} to {@code b}. */
		NE,
		/** Southeast, from {@code x} to {@code c}. */
		SE,
		/** Southward, from {@code x} to {@code d}. */
		S,
		/** Southwest, from {@code x} to {@code e}. */
		SW,
		/** Northwest, from {@code x} to {@code f}. */
		NW
	}

	/**
	 * A mapping that says how to go from one board's coordinates (only the Z
	 * coordinate matters for this) to another when you move in a particular
	 * direction.
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

	// @formatter:off
	/**
	 * Represents link directions of a board.
	 * <pre>
	 *  ___     ___     ___     ___
	 * / . \___/ . \___/ . \___/ . \___
	 * \___/ . \___/ . \___/ . \___/ . \
	 * /0,1\___/1,1\___/2,1\___/3,1\___/
	 * \___/ . \___/ . \___/ . \___/ . \___
	 *     \_2_/ . \___/ . \___/ . \___/ . \
	 *     /0,0\_1_/1,0\___/2,0\___/3,0\___/
	 *     \_0_/   \___/   \___/   \___/

	 *
	 *          ___
	 *      ___/ b \___
	 *     / a \___/ c \
	 *     \___/ x \___/
	 *     / f \___/ d \
	 *     \___/ e \___/
	 *         \___/
	 *
	 *      _____
	 *     /x,y,2\_____
	 *     \_____/x,y,1\
	 *     /x,y,0\_____/
	 *     \_____/
	 *
	 *  /^\ /^\ /^\
	 * | a | b | c |
	 *  \ / \ / \ / \            n/^\e
	 *   | d | e | f |         nw|   |se
	 *    \ / \ / \ / \          w\./s
	 *     | g | h | i |
	 *      \./ \./ \./
	 * </pre>
	 *
	 * @author Donal Fellows
	 */
	// @formatter:on
	private static final class LinkDirections {
		/** Coordinates of the board; X direction. */
		final int x;

		/** Coordinates of the board; Y direction. */
		final int y;

		/** Whether to switch on the link in the N direction. */
		final boolean n;

		/** Whether to switch on the link in the S direction. */
		final boolean s;

		/** Whether to switch on the link in the E direction. */
		final boolean e;

		/** Whether to switch on the link in the W direction. */
		final boolean w;

		/** Whether to switch on the link in the NW direction. */
		final boolean nw;

		/** Whether to switch on the link in the SE direction. */
		final boolean se;

		LinkDirections(int width, int height, int x, int y) {
			this.x = x;
			this.y = y;
			nw = x > 0;
			s = y > 0;
			w = nw && s;
			se = x < width - 1;
			n = y < height - 1;
			e = se && n;
		}
	}
}
