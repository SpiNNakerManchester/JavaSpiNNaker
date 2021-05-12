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
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.readSQL;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.runQuery;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.runUpdate;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.allocator.JobState.POWER;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;

@Component
public class AllocatorTask {
	private static final Logger log = getLogger(AllocatorTask.class);
	@Autowired
	DatabaseEngine db;

	/**
	 * Time, in milliseconds, between runs of {@link #allocate()}.
	 */
	public static final long INTER_ALLOCATE_DELAY = 5000;

	public static final String GET_TASKS =
			"SELECT req_id, job_id, num_boards, width, height, x, y, z, "
					+ "jobs.machine_id AS machine_id, max_dead_boards"
					+ "FROM job_request JOIN jobs USING (job_id) "
					+ "ORDER_BY req_id";
	public static final String DELETE_TASK =
			"DELETE FROM job_request WHERE req_id = ";

	public static final String ALLOCATE_BOARDS_JOB = "UPDATE jobs SET "
			+ "width = ?, height = ?, job_state = ?, num_pending = ?,"
			+ "root_id = (SELECT board_id FROM boards WHERE "
			+ "machine_id = ? AND root_x = ? AND root_y = ?) "
			+ "WHERE job_id = ?";

	public static final String ALLOCATE_BOARDS_BOARD =
			"UPDATE jobs SET allocated_job = ? "
					+ "WHERE machine_id = ? AND root_x = ? AND root_y = ? "
					+ "AND may_be_allocated > 0";

	public static final String GET_FREE_BOARD_AT =
			"SELECT board_id, root_x, root_y FROM boards "
					+ "WHERE machine_id = ? AND allocated_job IS NULL "
					+ "AND functioning > 0 ORDER BY root_x ASC, root_y ASC";
	private static final String SET_NUM_PENDING =
			"UPDATE jobs SET num_pending = ? WHERE job_id = ?";

	@Value("classpath:find_rectangle.sql")
	private Resource findRectangle;

	@Value("classpath:find_location.sql")
	private Resource findLocation;

	@Value("classpath:issue_change_for_job.sql")
	private Resource issueChangeForJob;

	/**
	 * Allocate all current requests for resources.
	 *
	 * @throws SQLException
	 *             If anything goes wrong at the DB level
	 */
	@Scheduled(fixedDelay = INTER_ALLOCATE_DELAY)
	public void allocate() throws SQLException {
		try (Connection conn = db.getConnection();
				Statement s = conn.createStatement();
				ResultSet rs = s.executeQuery(GET_TASKS)) {
			while (rs.next()) {
				int id = rs.getInt("req_id");
				boolean handled = true;
				try {
					handled = allocate(conn, rs);
					/*
					 * NB: having an exception counts as handled; we will nuke
					 * the task.
					 */
				} finally {
					if (handled) {
						s.executeUpdate(DELETE_TASK + id);
					}
				}
			}
		}
	}

	/**
	 * Perform the allocation for a particular task. Note that allocation does
	 * not actually send any messages to the board's BMP (though it does
	 * schedule them). That's an entirely different thing.
	 * <p>
	 * Called <em>without</em> a transaction open.
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
			return transaction(conn, () -> {
				return allocateDimensions(conn, jobId, machineId, (int) w,
						(int) h, maxDeadBoards + tolerance);
			});
		}

		Integer width = (Integer) task.getObject("width");
		Integer height = (Integer) task.getObject("height");
		if (width != null && height != null && width > 0 && height > 0) {
			if (height == 1 && width == 1) {
				return allocateOneBoard(conn, jobId, machineId);
			}
			return transaction(conn, () -> {
				return allocateDimensions(conn, jobId, machineId, width, height,
						maxDeadBoards);
			});
		}

		Integer cabinet = (Integer) task.getObject("cabinet");
		Integer frame = (Integer) task.getObject("frame");
		Integer board = (Integer) task.getObject("board");
		if (cabinet != null && frame != null && board != null) {
			return transaction(conn, () -> {
				// Ignores maxDeadBoards; is a single-board allocate
				return allocateCoords(conn, jobId, machineId, cabinet, frame,
						board);
			});
		}

		log.warn("job {} could not be allocated; "
				+ "bad request will be cleared from queue", jobId);
		return true;
	}

	// Creates a transaction
	private boolean allocateOneBoard(Connection conn, int jobId, int machineId)
			throws SQLException {
		return transaction(conn, () -> {
			return allocateDimensions(conn, jobId, machineId, 1, 1, 0);
		});
	}

	// Must be called inside a transaction
	private boolean allocateDimensions(Connection conn, int jobId,
			int machineId, int width, int height, int tolerance)
			throws SQLException {
		try (PreparedStatement s =
				conn.prepareStatement(readSQL(findRectangle));
				ResultSet rs =
						runQuery(s, width, height, machineId, tolerance)) {
			while (rs.next()) {
				int x = rs.getInt("x");
				int y = rs.getInt("y");
				setAllocation(conn, jobId, 1, 1, machineId, x, y);
				return true;
			}
			return false;
		}
	}

	// Must be called inside a transaction
	private boolean allocateCoords(Connection conn, int jobId, int machineId,
			int cabinet, int frame, int board) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(readSQL(findLocation));
				ResultSet rs = runQuery(s, machineId, cabinet, frame, board)) {
			while (rs.next()) {
				int x = rs.getInt("x");
				int y = rs.getInt("y");
				setAllocation(conn, jobId, 1, 1, machineId, x, y);
				return true;
			}
			return false;
		}
	}

	// Must be called inside a transaction
	private void setAllocation(Connection conn, int jobId, int width,
			int height, int machineId, int rootX, int rootY)
			throws SQLException {
		try (PreparedStatement allocBoard =
				conn.prepareStatement(ALLOCATE_BOARDS_BOARD);
				PreparedStatement allocJob =
						conn.prepareStatement(ALLOCATE_BOARDS_JOB);
				PreparedStatement issueChange =
						conn.prepareStatement(readSQL(issueChangeForJob));
				PreparedStatement setNumPending =
						conn.prepareStatement(SET_NUM_PENDING)) {
			List<LinkDirections> perimeter = new ArrayList<>();
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					runUpdate(allocBoard, jobId, machineId, x + rootX,
							y + rootY);
					if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
						perimeter.add(new LinkDirections(width, height, x, y));
					}
				}
			}
			runUpdate(allocJob, width, height, POWER.ordinal(),
					perimeter.size(), machineId, rootX, rootY, jobId);
			int numPending = 0;
			for (LinkDirections ld : perimeter) {
				/*
				 * Issues instructions to reconfigure the perimeter of the
				 * allocated rectangle, incrementing numPending for each.
				 */
				numPending += runUpdate(issueChange, jobId, machineId,
						rootX + ld.x, rootY + ld.y, true, ld.n, ld.s, ld.e,
						ld.w, ld.nw, ld.se);
			}
			runUpdate(setNumPending, numPending, jobId);
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
	static class LinkDirections {
		int x;
		int y;
		boolean nw;
		boolean s;
		boolean w;
		boolean se;
		boolean n;
		boolean e;

		LinkDirections(int width, int height, int x, int y) {
			nw = x > 0;
			s = y > 0;
			w = nw && s;
			se = x < width - 1;
			n = y < height - 1;
			e = se && n;
		}
	}
}
