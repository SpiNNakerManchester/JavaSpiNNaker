package uk.ac.manchester.spinnaker.alloc;

import static org.apache.commons.logging.LogFactory.getLog;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.runQuery;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.runUpdate;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
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
			+ "width = ?, height = ?, job_state = 1, num_pending = ?,"
			+ "root_id = (SELECT board_id FROM boards WHERE "
			+ "machine_id = ? AND root_x = ? AND root_y = ?) "
			+ "WHERE job_id = ?";

	public static final String ALLOCATE_BOARDS_BOARD =
			"UPDATE jobs SET allocated_job = ? "
					+ "WHERE machine_id = ? AND root_x = ? AND root_y = ?";

	public static final String GET_FREE_BOARD =
			"SELECT board_id, root_x, root_y FROM boards "
					+ "WHERE machine_id = ? AND allocated_job IS NULL "
					+ "AND functioning > 0 ORDER BY root_x ASC, root_y ASC";

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
				return allocateSingleBoard(conn, jobId, machineId);
			}
			return allocateBoards(conn, jobId, machineId, numBoards);
		}

		Integer width = (Integer) task.getObject("width");
		Integer height = (Integer) task.getObject("height");
		if (width != null && height != null) {
			return allocateDimensions(conn, jobId, machineId, width, height);
		}

		Integer x = (Integer) task.getObject("x");
		Integer y = (Integer) task.getObject("y");
		Integer z = (Integer) task.getObject("z");
		if (x != null && y != null && z != null) {
			return allocateCoords(conn, jobId, machineId, x, y, z);
		}

		log.warn("job " + jobId + " could not be allocated; "
				+ "bad request will be cleared from queue");
		return true;
	}

	private boolean allocateSingleBoard(Connection conn, int jobId,
			int machineId) throws SQLException {
		return transaction(conn, () -> {
			try (PreparedStatement s = conn.prepareStatement(GET_FREE_BOARD);
					ResultSet rs = runQuery(s, machineId)) {
				while (rs.next()) {
					int x = rs.getInt("root_x");
					int y = rs.getInt("root_y");
					setAllocation(conn, jobId, 1, 1, machineId, x, y);
					return true;
				}
			}
			return false;
		});
	}

	private boolean allocateBoards(Connection conn, int jobId, int machineId,
			int numBoards) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean allocateDimensions(Connection conn, int jobId,
			int machineId, int width, int height) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean allocateCoords(Connection conn, int jobId, int machineId,
			int x, int y, int z) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	private static void setAllocation(Connection conn, int jobId, int width,
			int height, int machineId, int rootX, int rootY)
			throws SQLException {
		try (PreparedStatement allocBoard =
				conn.prepareStatement(ALLOCATE_BOARDS_BOARD);
				PreparedStatement allocJob =
						conn.prepareStatement(ALLOCATE_BOARDS_JOB)) {
			int numPending = 0;
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					runUpdate(allocBoard, jobId, machineId, x + rootX,
							y + rootY);
				}
			}
			/*
			 * TODO issue instructions to reconfigure the perimeter of the
			 * allocated rectangle, incrementing numPending for each.
			 */
			runUpdate(allocJob, width, height, numPending, machineId, rootX,
					rootY, jobId);
		}
	}
}
