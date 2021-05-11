package uk.ac.manchester.spinnaker.alloc;

import static java.lang.Math.ceil;
import static java.lang.Math.sqrt;
import static org.apache.commons.logging.LogFactory.getLog;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.readSQL;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.runQuery;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.runUpdate;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.JobState.POWER;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;

public class AllocatorTask {
	private static final Log log = getLog(AllocatorTask.class);
	@Autowired
	DatabaseEngine db;

	/**
	 * Time, in seconds, between runs of {@link #allocate()}.
	 */
	public static final long INTER_ALLOCATE_DELAY = 5000;

	public static final String GET_TASKS =
			"SELECT req_id, job_id, num_boards, width, height, x, y, z, "
					+ "jobs.machine_id AS machine_id"
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
	private boolean allocate(Connection conn, ResultSet task)
			throws SQLException {
		int jobId = task.getInt("job_id");
		int machineId = task.getInt("machine_id");
		Integer numBoards = (Integer) task.getObject("num_boards");
		if (numBoards != null) {
			if (numBoards.intValue() == 1) {
				return allocateDimensions(conn, jobId, machineId, 1, 1, 0);
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
					tolerance);
		}

		Integer width = (Integer) task.getObject("width");
		Integer height = (Integer) task.getObject("height");
		if (width != null && height != null) {
			int numDeadBoards = 0;// FIXME
			return allocateDimensions(conn, jobId, machineId, width, height,
					numDeadBoards);
		}

		Integer cabinet = (Integer) task.getObject("x");
		Integer frame = (Integer) task.getObject("y");
		Integer board = (Integer) task.getObject("z");
		if (cabinet != null && frame != null && board != null) {
			return allocateCoords(conn, jobId, machineId, cabinet, frame,
					board);
		}

		log.warn("job " + jobId + " could not be allocated; "
				+ "bad request will be cleared from queue");
		return true;
	}

	private boolean allocateDimensions(Connection conn, int jobId,
			int machineId, int width, int height, int tolerance)
			throws SQLException {
		return transaction(conn, () -> {
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
		});
	}

	private boolean allocateCoords(Connection conn, int jobId, int machineId,
			int cabinet, int frame, int board) throws SQLException {
		return transaction(conn, () -> {
			try (PreparedStatement s =
					conn.prepareStatement(readSQL(findLocation));
					ResultSet rs =
							runQuery(s, machineId, cabinet, frame, board)) {
				while (rs.next()) {
					int x = rs.getInt("x");
					int y = rs.getInt("y");
					setAllocation(conn, jobId, 1, 1, machineId, x, y);
					return true;
				}
				return false;
			}
		});
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
			List<int[]> perimeter = new ArrayList<>();
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					runUpdate(allocBoard, jobId, machineId, x + rootX,
							y + rootY);
					if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
						perimeter.add(new int[] {
							x, y
						});
					}
				}
			}
			runUpdate(allocJob, width, height, POWER.ordinal(),
					perimeter.size(), machineId, rootX, rootY, jobId);
			int numPending = 0;
			for (int[] coord : perimeter) {
				/*
				 * Issues instructions to reconfigure the perimeter of the
				 * allocated rectangle, incrementing numPending for each.
				 *
				 * @formatter:off
				 *  /^\ /^\ /^\
				 * | a | b | c |
				 *  \ / \ / \ / \            n/^\e
				 *   | d | e | f |         nw|   |se
				 *    \ / \ / \ / \          w\./s
				 *     | g | h | i |
				 *      \./ \./ \./
				 * @formatter:on
				 */
				int x = coord[0], y = coord[1];
				boolean nw = x > 0;
				boolean s = y > 0;
				boolean w = nw && s;
				boolean se = x < width - 1;
				boolean n = y < height - 1;
				boolean e = se && n;
				numPending += runUpdate(issueChange, jobId, machineId, rootX,
						rootY, true, n, s, e, w, nw, se);
			}
			runUpdate(setNumPending, numPending, jobId);
		}
	}
}
