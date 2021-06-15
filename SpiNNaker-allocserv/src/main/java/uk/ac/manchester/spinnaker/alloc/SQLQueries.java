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
package uk.ac.manchester.spinnaker.alloc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import uk.ac.manchester.spinnaker.storage.GeneratesID;
import uk.ac.manchester.spinnaker.storage.Parameter;
import uk.ac.manchester.spinnaker.storage.ResultColumn;
import uk.ac.manchester.spinnaker.storage.SingleRowResult;

/**
 * The literal SQL queries used in this package.
 *
 * @author Donal Fellows
 */
public abstract class SQLQueries {
	@ResultColumn("machine_id")
	@ResultColumn("machine_name")
	@ResultColumn("width")
	@ResultColumn("height")
	protected static final String GET_ALL_MACHINES =
			"SELECT machine_id, machine_name, width, height FROM machines";

	@Parameter("machine_id")
	@ResultColumn("machine_id")
	@ResultColumn("machine_name")
	@ResultColumn("width")
	@ResultColumn("height")
	@SingleRowResult
	protected static final String GET_MACHINE_BY_ID =
			"SELECT machine_id, machine_name, width, height FROM machines "
					+ "WHERE machine_id = ? LIMIT 1";

	@Parameter("machine_name")
	@ResultColumn("machine_id")
	@ResultColumn("machine_name")
	@ResultColumn("width")
	@ResultColumn("height")
	@SingleRowResult
	protected static final String GET_NAMED_MACHINE =
			"SELECT machine_id, machine_name, width, height FROM machines "
					+ "WHERE machine_name = ? LIMIT 1";

	@Parameter("limit")
	@Parameter("offset")
	@ResultColumn("job_id")
	@ResultColumn("machine_id")
	@ResultColumn("job_state")
	@ResultColumn("keepalive_timestamp")
	protected static final String GET_JOB_IDS =
			"SELECT job_id, machine_id, job_state, keepalive_timestamp "
					+ "FROM jobs ORDER BY job_id DESC LIMIT ? OFFSET ?";

	@Parameter("limit")
	@Parameter("offset")
	@ResultColumn("job_id")
	@ResultColumn("machine_id")
	@ResultColumn("job_state")
	@ResultColumn("keepalive_timestamp")
	protected static final String GET_LIVE_JOB_IDS =
			"SELECT job_id, machine_id, job_state, keepalive_timestamp "
					+ "FROM jobs WHERE job_state != 4 " // DESTROYED
					+ "ORDER BY job_id DESC LIMIT ? OFFSET ?";

	@Parameter("job_id")
	@ResultColumn("machine_id")
	@ResultColumn("width")
	@ResultColumn("height")
	@ResultColumn("depth")
	@ResultColumn("root_id")
	@ResultColumn("job_state")
	@ResultColumn("keepalive_timestamp")
	@ResultColumn("keepalive_host")
	@ResultColumn("create_timestamp")
	@ResultColumn("death_reason")
	@ResultColumn("death_timestamp")
	@SingleRowResult
	protected static final String GET_JOB =
			"SELECT machine_id, width, height, depth, root_id, job_state, "
					+ "keepalive_timestamp, keepalive_host, create_timestamp, "
					+ "death_reason, death_timestamp "
					+ "FROM jobs WHERE job_id = ? LIMIT 1";

	@Parameter("job_id")
	@ResultColumn("board_id")
	protected static final String GET_JOB_BOARDS =
			"SELECT board_id FROM boards JOIN jobs "
					+ "ON boards.allocated_job = jobs.job_id "
					+ "WHERE boards.allocated_job = ? "
					+ "AND (jobs.job_state == 1 OR jobs.job_state == 3)";
					// QUEUED or READY

	@Parameter("board_id")
	@ResultColumn("root_x")
	@ResultColumn("root_y")
	@SingleRowResult
	protected static final String GET_ROOT_OF_BOARD =
			"SELECT root_x, root_y FROM boards WHERE board_id = ? LIMIT 1";

	@Parameter("machine_id")
	@Parameter("owner")
	@Parameter("keepalive_interval")
	@GeneratesID
	protected static final String INSERT_JOB = "INSERT INTO jobs("
			+ "machine_id, owner, keepalive_interval, keepalive_timestamp, "
			+ "create_timestamp, job_state) VALUES "
			+ "(?, ?, ?, strftime('%s','now'), strftime('%s','now'), "
			+ /* QUEUED */ "1)";

	@Parameter("job_id")
	@Parameter("num_boards")
	@Parameter("max_dead_boards")
	@GeneratesID
	protected static final String INSERT_REQ_N_BOARDS =
			"INSERT INTO job_request(job_id, num_boards, max_dead_boards) "
					+ "VALUES (?, ?, ?)";

	@Parameter("job_id")
	@Parameter("width")
	@Parameter("height")
	@Parameter("max_dead_boards")
	@GeneratesID
	protected static final String INSERT_REQ_SIZE =
			"INSERT INTO job_request(job_id, width, height, max_dead_boards) "
					+ "VALUES (?, ?, ?, ?)";

	@Parameter("job_id")
	@Parameter("x")
	@Parameter("y")
	@Parameter("z")
	@GeneratesID
	protected static final String INSERT_REQ_LOCATION =
			"INSERT INTO job_request(job_id, x, y, z) VALUES (?, ?, ?, ?)";

	@Parameter("machine_id")
	@Parameter("chip_x")
	@Parameter("chip_y")
	@ResultColumn("board_id")
	@ResultColumn("address")
	@ResultColumn("bmp_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("job_id")
	@ResultColumn("machine_name")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("chip_x")
	@ResultColumn("chip_y")
	@ResultColumn("board_chip_x")
	@ResultColumn("board_chip_y")
	@ResultColumn("job_root_chip_x")
	@ResultColumn("job_root_chip_y")
	@SingleRowResult
	protected static final String FIND_BOARD_BY_CHIP =
			"SELECT boards.board_id, boards.address, boards.bmp_id, "
					+ "boards.x, boards.y, boards.z, "
					+ "boards.allocated_job AS job_id, m.machine_name, "
					+ "bmp.cabinet, bmp.frame, boards.board_num, "
					+ "boards.root_x + bmc.chip_x AS chip_x, "
					+ "boards.root_y + bmc.chip_y AS chip_y, "
					+ "boards.root_x AS board_chip_x, "
					+ "boards.root_y AS board_chip_y, "
					+ "root.root_x AS job_root_chip_x, "
					+ "root.root_y AS job_root_chip_y "
					+ "FROM boards JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "JOIN machines AS m ON boards.machine_id = m.machine_id "
					+ "JOIN board_model_coords AS bmc "
					+ "ON m.board_model = bmc.model "
					+ "LEFT JOIN jobs ON jobs.job_id = boards.allocated_job "
					+ "LEFT JOIN boards AS root ON root.board_id = jobs.root_id "
					+ "WHERE boards.machine_id = ? "
					+ "AND chip_x = ? AND chip_y = ? LIMIT 1";

	@Parameter("machine_id")
	@Parameter("cabinet")
	@Parameter("frame")
	@Parameter("board")
	@ResultColumn("board_id")
	@ResultColumn("address")
	@ResultColumn("bmp_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("job_id")
	@ResultColumn("machine_name")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("chip_x")
	@ResultColumn("chip_y")
	@ResultColumn("board_chip_x")
	@ResultColumn("board_chip_y")
	@SingleRowResult
	protected static final String FIND_BOARD_BY_CFB =
			"SELECT boards.board_id, boards.address, boards.bmp_id, "
					+ "boards.x, boards.y, boards.z, "
					+ "boards.allocated_job AS job_id, m.machine_name, "
					+ "bmp.cabinet, bmp.frame, boards.board_num, "
					+ "boards.root_x AS chip_x, boards.root_y AS chip_y, "
					+ "boards.root_x AS board_chip_x, "
					+ "boards.root_y AS board_chip_y, "
					+ "root.root_x AS job_root_chip_x, "
					+ "root.root_y AS job_root_chip_y "
					+ "FROM boards JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "JOIN machines AS m ON boards.machine_id = m.machine_id "
					+ "LEFT JOIN jobs ON jobs.job_id = boards.allocated_job "
					+ "LEFT JOIN boards AS root ON root.board_id = jobs.root_id "
					+ "WHERE boards.machine_id = ? AND bmp.cabinet = ? "
					+ "AND bmp.frame = ? AND boards.board_num = ? LIMIT 1";

	@Parameter("machine_id")
	@Parameter("x")
	@Parameter("y")
	@Parameter("z")
	@ResultColumn("board_id")
	@ResultColumn("address")
	@ResultColumn("bmp_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("job_id")
	@ResultColumn("machine_name")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("chip_x")
	@ResultColumn("chip_y")
	@ResultColumn("board_chip_x")
	@ResultColumn("board_chip_y")
	@SingleRowResult
	protected static final String FIND_BOARD_BY_XYZ =
			"SELECT boards.board_id, boards.address, boards.bmp_id, "
					+ "boards.x, boards.y, boards.z, "
					+ "boards.allocated_job AS job_id, m.machine_name, "
					+ "bmp.cabinet, bmp.frame, boards.board_num, "
					+ "boards.root_x AS chip_x, boards.root_y AS chip_y, "
					+ "boards.root_x AS board_chip_x, "
					+ "boards.root_y AS board_chip_y, "
					+ "root.root_x AS job_root_chip_x, "
					+ "root.root_y AS job_root_chip_y "
					+ "FROM boards JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "JOIN machines AS m ON boards.machine_id = m.machine_id "
					+ "LEFT JOIN jobs ON jobs.job_id = boards.allocated_job "
					+ "LEFT JOIN boards AS root ON root.board_id = jobs.root_id "
					+ "WHERE boards.machine_id = ? AND boards.x = ? "
					+ "AND boards.y = ? AND boards.z = ? LIMIT 1";

	@Parameter("machine_id")
	@ResultColumn("address")
	@SingleRowResult
	protected static final String GET_ROOT_BMP_ADDRESS =
			"SELECT bmp.address FROM bmp "
					+ "JOIN boards ON boards.bmp_id = bmp.bmp_id WHERE "
					+ "boards.machine_id = ? AND boards.x = 0 AND boards.y = 0 "
					+ "LIMIT 1";

	@Parameter("machine_id")
	@ResultColumn("board_num")
	protected static final String GET_BOARD_NUMBERS =
			"SELECT board_num FROM boards WHERE machine_id = ? "
					+ "AND (functioning IS NULL OR functioning != 0) "
					+ "ORDER BY board_num ASC";

	@Parameter("machine_id")
	@ResultColumn("board_num")
	protected static final String GET_DEAD_BOARD_NUMBERS =
			"SELECT board_num FROM boards WHERE machine_id = ? "
					+ "AND functioning IS 0 "
					+ "ORDER BY board_num ASC";

	@Parameter("machine_id")
	@ResultColumn("board_num")
	protected static final String GET_AVAILABLE_BOARD_NUMBERS =
			"SELECT board_num FROM boards WHERE machine_id = ? "
					+ "AND may_be_allocated "
					+ "ORDER BY board_num ASC";

	@Parameter("machine_id")
	@ResultColumn("tag")
	protected static final String GET_TAGS =
			"SELECT tag FROM tags WHERE machine_id = ?";

	@Parameter("keepalive_host")
	@Parameter("job_id")
	protected static final String UPDATE_KEEPALIVE =
			"UPDATE jobs SET keepalive_timestamp = strftime('%s','now'), "
					+ "keepalive_host = ? WHERE job_id = ? "
					+ "AND job_state != 4"; // DESTROYED

	@Parameter("death_reason")
	@Parameter("job_id")
	protected static final String DESTROY_JOB =
			"UPDATE jobs SET job_state = 4, death_reason = ? "
					+ "WHERE job_id = ? AND job_state != 4";

	@Parameter("job_id")
	@ResultColumn("total_on")
	@SingleRowResult
	protected static final String GET_BOARD_POWER =
			"SELECT sum(board_power) AS total_on FROM boards "
					+ "WHERE allocated_job = ?";

	@Parameter("job_id")
	@ResultColumn("board_id")
	@ResultColumn("address")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("root_x")
	@ResultColumn("root_y")
	protected static final String GET_BOARD_CONNECT_INFO =
			"SELECT board_id, address, x, y, z, root_x, root_y "
					+ "FROM boards WHERE allocated_job = ? "
					+ "ORDER BY x ASC, y ASC";

	@Parameter("board_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("root_x")
	@ResultColumn("root_y")
	@SingleRowResult
	protected static final String GET_ROOT_COORDS =
			"SELECT x, y, z, root_x, root_y FROM boards "
					+ "WHERE board_id = ? LIMIT 1";

	/** How we get the list of allocation tasks. */
	@ResultColumn("req_id")
	@ResultColumn("job_id")
	@ResultColumn("num_boards")
	@ResultColumn("width")
	@ResultColumn("height")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("machine_id")
	@ResultColumn("max_dead_boards")
	protected static final String GET_TASKS =
			"SELECT job_request.req_id, job_request.job_id,"
					+ "  job_request.num_boards,"
					+ "  job_request.width, job_request.height,"
					+ "  job_request.x, job_request.y, job_request.z,"
					+ "  jobs.machine_id AS machine_id,"
					+ "  job_request.max_dead_boards "
					+ "FROM job_request JOIN jobs"
					+ "  ON job_request.job_id = jobs.job_id ORDER BY req_id";

	/** How we delete an allocation task. */
	@Parameter("request_id")
	protected static final String DELETE_TASK =
			"DELETE FROM job_request WHERE req_id = ?";

	/** How do we find a single free board. */
	@Parameter("machine_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@SingleRowResult
	protected static final String FIND_FREE_BOARD =
			"SELECT x, y, z FROM boards "
					+ "WHERE machine_id = ? AND may_be_allocated > 0 "
					+ "ORDER BY power_off_timestamp ASC LIMIT 1";

	/**
	 * How we tell a job that it is allocated. Doesn't set the state.
	 *
	 * @see #SET_STATE_PENDING
	 */
	@Parameter("width")
	@Parameter("height")
	@Parameter("depth")
	@Parameter("board_id")
	@Parameter("job_id")
	protected static final String ALLOCATE_BOARDS_JOB = "UPDATE jobs SET "
			+ "width = ?, height = ?, depth = ?, num_pending = 0, root_id = ? "
			+ "WHERE job_id = ?";

	/** Get a board's ID by its coordinates. */
	@Parameter("machine_id")
	@Parameter("x")
	@Parameter("y")
	@Parameter("z")
	@ResultColumn("board_id")
	@SingleRowResult
	protected static final String GET_BOARD_BY_COORDS =
			"SELECT board_id FROM boards "
					+ "WHERE machine_id = ? AND x = ? AND y = ? AND z = ? "
					+ "AND may_be_allocated > 0 LIMIT 1";

	/** How we tell a board that it is allocated. */
	@Parameter("job_id")
	@Parameter("board_id")
	protected static final String ALLOCATE_BOARDS_BOARD =
			"UPDATE boards SET allocated_job = ? WHERE board_id = ?";

	@Parameter("board_power")
	@Parameter("board_id")
	protected static final String SET_BOARD_POWER =
			"UPDATE boards SET board_power = ? WHERE board_id = ?";

	@ResultColumn("job_id")
	protected static final String FIND_EXPIRED_JOBS = //
			"SELECT job_id FROM jobs " //
					+ "WHERE job_state != 4 " // DESTROYED
					+ "AND keepalive_timestamp + keepalive_interval < "
					+ "strftime('%s','now')";

	/** How we set the state and number of pending changes for a job. */
	@Parameter("job_state")
	@Parameter("num_pending")
	@Parameter("job_id")
	protected static final String SET_STATE_PENDING =
			"UPDATE jobs SET job_state = ?, num_pending = ? WHERE job_id = ?";

	@Parameter("job_id")
	protected static final String KILL_JOB_ALLOC_TASK =
			"DELETE FROM job_request WHERE job_id = ?";

	@Parameter("job_id")
	protected static final String KILL_JOB_PENDING =
			"DELETE FROM pending_changes WHERE job_id = ?";

	@Parameter("change_id")
	protected static final String FINISHED_PENDING =
			"DELETE FROM pending_changes WHERE change_id = ?";

	/**
	 * Get descriptions of how to move from a board to its neighbours.
	 */
	@ResultColumn("z")
	@ResultColumn("direction")
	@ResultColumn("dx")
	@ResultColumn("dy")
	@ResultColumn("dz")
	protected static final String LOAD_DIR_INFO =
			"SELECT z, direction, dx, dy, dz FROM movement_directions";

	@ResultColumn("c")
	@SingleRowResult
	protected static final String COUNT_PENDING_CHANGES =
			"SELECT count(*) AS c FROM pending_changes";

	@Parameter("job_id")
	@ResultColumn("change_id")
	@ResultColumn("job_id")
	@ResultColumn("board_id")
	@ResultColumn("power")
	@ResultColumn("fpga_n")
	@ResultColumn("fpga_s")
	@ResultColumn("fpga_ne")
	@ResultColumn("fpga_nw")
	@ResultColumn("fpga_se")
	@ResultColumn("fpga_sw")
	@ResultColumn("in_progress")
	protected static final String GET_CHANGES = "SELECT * FROM pending_changes "
			+ "WHERE job_id = ? AND NOT in_progress";

	@Parameter("in_progress")
	@Parameter("change_id")
	protected static final String SET_IN_PROGRESS =
			"UPDATE pending_changes SET in_progress = ? WHERE change_id = ?";

	// SQL loaded from files because it is too complicated otherwise!

	@Parameter("width")
	@Parameter("height")
	@Parameter("machine_id")
	@Parameter("max_dead_boards")
	@ResultColumn("id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("available")
	@Value("classpath:find_rectangle.sql")
	protected Resource findRectangle;

	@Parameter("machine_id")
	@Parameter("cabinet")
	@Parameter("frame")
	@Parameter("board")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@SingleRowResult
	@Value("classpath:find_location.sql")
	protected Resource findLocation;

	@Parameter("job_id")
	@Parameter("board_id")
	@Parameter("power")
	@Parameter("fpga_n")
	@Parameter("fpga_s")
	@Parameter("fpga_e")
	@Parameter("fpga_w")
	@Parameter("fpga_nw")
	@Parameter("fpga_se")
	@GeneratesID
	@Value("classpath:issue_change_for_job.sql")
	protected Resource issueChangeForJob;

	@Parameter("machine_id")
	@Parameter("x")
	@Parameter("y")
	@Parameter("width")
	@Parameter("height")
	@ResultColumn("connected_size")
	@SingleRowResult
	@Value("classpath:allocation_connected.sql")
	protected Resource countConnected;

	@Parameter("job_id")
	@ResultColumn("board_id")
	@ResultColumn("direction")
	@Value("classpath:perimeter.sql")
	protected Resource getPerimeterLinks;

	@Parameter("job_id")
	@Parameter("root_board_id")
	@Parameter("chip_x")
	@Parameter("chip_y")
	@ResultColumn("board_id")
	@ResultColumn("address")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("job_id")
	@ResultColumn("machine_name")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("chip_x")
	@ResultColumn("chip_y")
	@ResultColumn("board_chip_x")
	@ResultColumn("board_chip_y")
	@SingleRowResult
	@Value("classpath:find_board_by_job_chip.sql")
	protected Resource findBoardByJobChip;

	@Parameter("machine_id")
	@Parameter("on_delay")
	@Parameter("off_delay")
	@ResultColumn("job_id")
	@Value("classpath:get_jobs_with_changes.sql")
	protected Resource getJobsWithChanges;

	@Parameter("machine_id")
	@Parameter("x")
	@Parameter("y")
	@Parameter("z")
	@Parameter("width")
	@Parameter("height")
	@Parameter("depth")
	@ResultColumn("board_id")
	@Value("classpath:connected_boards_at_coords.sql")
	protected Resource getConnectedBoards;
}
