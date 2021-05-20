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
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.allocator.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.allocator.JobState.POWER;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;

@Component
public class AllocatorTask {
	private static final Logger log = getLogger(AllocatorTask.class);

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private Epochs epochs;

	/**
	 * Time, in milliseconds, between runs of {@link #allocate()}. (5s)
	 */
	public static final long INTER_ALLOCATE_DELAY = 5000;

	/**
	 * Time, in milliseconds, between runs of {@link #cleanUp()}. (30s)
	 */
	private static final long INTER_DESTROY_DELAY = 30000;

	/** How we get the list of allocation tasks. */
	public static final String GET_TASKS =
			"SELECT job_request.req_id, job_request.job_id,"
					+ "  job_request.num_boards,"
					+ "	 job_request.width, job_request.height,"
					+ "  job_request.cabinet, job_request.frame,"
					+ "	 job_request.board, jobs.machine_id AS machine_id,"
					+ "  job_request.max_dead_boards "
					+ "FROM job_request JOIN jobs"
					+ "  ON job_request.job_id = jobs.job_id ORDER BY req_id";

	/** How we delete an allocation task. */
	public static final String DELETE_TASK =
			"DELETE FROM job_request WHERE req_id = ?";

	/** How we tell a job that it is allocated. */
	public static final String ALLOCATE_BOARDS_JOB = "UPDATE jobs SET "
			+ "width = ?, height = ?, job_state = ?, num_pending = ?,"
			+ "root_id = (SELECT board_id FROM boards WHERE "
			+ "machine_id = ? AND root_x = ? AND root_y = ?) "
			+ "WHERE job_id = ?";

	/** How we tell a board that it is allocated. */
	public static final String ALLOCATE_BOARDS_BOARD =
			"UPDATE boards SET allocated_job = ? "
					+ "WHERE machine_id = ? AND root_x = ? AND root_y = ? "
					+ "AND may_be_allocated > 0";

	public static final String FIND_EXPIRED_JOBS =
			"SELECT job_id FROM jobs WHERE "
					+ "job_state != ? AND keepalive_timestamp < ? - 30000";

	/** How we set the number of pending changes for a job. */
	public static final String SET_NUM_PENDING =
			"UPDATE jobs SET num_pending = ? WHERE job_id = ?";

	@Value("classpath:find_rectangle.sql")
	private Resource findRectangle;

	@Value("classpath:find_location.sql")
	private Resource findLocation;

	@Value("classpath:issue_change_for_job.sql")
	private Resource issueChangeForJob;

	@Value("classpath:allocation_connected.sql")
	private Resource countConnected;

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
					for (ResultSet rs : getTasks.call()) {
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
		Date now = new Date();
		try (Connection conn = db.getConnection()) {
			transaction(conn, () -> {
				boolean changed = false;
				try (Query find = query(conn, FIND_EXPIRED_JOBS)) {
					List<Integer> toKill = new ArrayList<>();
					for (ResultSet rs : find.call(DESTROYED, now)) {
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

	private static final String MARK_JOB_DESTROYED =
			"UPDATE jobs SET job_state = ?, death_timestamp = ? "
					+ "WHERE job_id = ? AND job_state != ?";

	private static final String KILL_JOB_ALLOC_TASK =
			"DELETE FROM job_request WHERE job_id = ?";

	private static final String KILL_JOB_PENDING =
			"DELETE FROM pending_changes WHERE job_id = ?";

	private static final String ISSUE_BOARD_OFF_FOR_JOB =
			"INSERT INTO pending_changes(job_id, \"power\", board_id) "
					+ "SELECT ?, ?, board_id FROM boards "
					+ "WHERE job_id = ? AND power_state != ?";

	private boolean destroyJob(Connection conn, int id) throws SQLException {
		Date now = new Date();
		try (Update mark = update(conn, MARK_JOB_DESTROYED);
				Update killAlloc = update(conn, KILL_JOB_ALLOC_TASK);
				Update killPending = update(conn, KILL_JOB_PENDING);
				Update issueOff = update(conn, ISSUE_BOARD_OFF_FOR_JOB)) {
			boolean success = mark.call(DESTROYED, now, id, DESTROYED) > 0;
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
	boolean allocate(Connection conn, ResultSet task) throws SQLException {
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

		Integer cabinet = (Integer) task.getObject("cabinet");
		Integer frame = (Integer) task.getObject("frame");
		Integer board = (Integer) task.getObject("board");
		if (cabinet != null && frame != null && board != null) {
			// Ignores maxDeadBoards; is a single-board allocate
			return allocateCoords(conn, jobId, machineId, cabinet, frame,
					board);
		}

		log.warn("job {} could not be allocated; "
				+ "bad request will be cleared from queue", jobId);
		return true;
	}

	private boolean allocateOneBoard(Connection conn, int jobId, int machineId)
			throws SQLException {
		return allocateDimensions(conn, jobId, machineId, 1, 1, 0);
	}

	private boolean allocateDimensions(Connection conn, int jobId,
			int machineId, int width, int height, int tolerance)
			throws SQLException {
		try (Query s = query(conn, findRectangle)) {
			for (ResultSet rs : s.call(width, height, machineId, tolerance)) {
				int x = rs.getInt("x");
				int y = rs.getInt("y");
				if (width * height > 1) {
					/*
					 * Check that a minimum number of boards are reachable from
					 * the proposed root board.
					 */
					int size = -1;
					try (Query connected = query(conn, countConnected)) {
						for (ResultSet row : connected.call(machineId, x, y,
								width, height)) {
							size = row.getInt("connected_size");
						}
					}
					if (size < width * height - tolerance) {
						continue;
					}
				}
				setAllocation(conn, jobId, width, height, machineId, x, y);
				return true;
			}
			return false;
		}
	}

	private boolean allocateCoords(Connection conn, int jobId, int machineId,
			int cabinet, int frame, int board) throws SQLException {
		try (Query find = query(conn, findLocation)) {
			for (ResultSet rs : find.call(machineId, cabinet, frame, board)) {
				int x = rs.getInt("x");
				int y = rs.getInt("y");
				setAllocation(conn, jobId, 1, 1, machineId, x, y);
				return true;
			}
			return false;
		}
	}

	private void setAllocation(Connection conn, int jobId, int width,
			int height, int machineId, int rootX, int rootY)
			throws SQLException {
		try (Update allocBoard = update(conn, ALLOCATE_BOARDS_BOARD);
				Update allocJob = update(conn, ALLOCATE_BOARDS_JOB);
				Update issueChange = update(conn, issueChangeForJob);
				Update setNumPending = update(conn, SET_NUM_PENDING)) {
			boolean changed = false;
			List<LinkDirections> perimeter = new ArrayList<>();
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					changed |= allocBoard.call(jobId, machineId, x + rootX,
							y + rootY) > 0;
					if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
						perimeter.add(new LinkDirections(width, height, x, y));
					}
				}
			}
			allocJob.call(width, height, POWER, perimeter.size(), machineId,
					rootX, rootY, jobId);
			int numPending = 0;
			for (LinkDirections ld : perimeter) {
				/*
				 * Issues instructions to reconfigure the perimeter of the
				 * allocated rectangle, incrementing numPending for each.
				 */
				numPending += issueChange.call(jobId, machineId, rootX + ld.x,
						rootY + ld.y, true, ld.n, ld.s, ld.e, ld.w, ld.nw,
						ld.se);
			}
			setNumPending.call(numPending, jobId);
			if (changed || numPending > 0) {
				epochs.nextMachineEpoch();
			}
		}
	}

	// @formatter:off
	/**
	 * Represents link directions of a board.
	 * <pre>
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
