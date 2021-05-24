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

	@Parameter("job_state:DESTROYED")
	@Parameter("limit")
	@Parameter("offset")
	@ResultColumn("job_id")
	@ResultColumn("machine_id")
	@ResultColumn("job_state")
	@ResultColumn("keepalive_timestamp")
	protected static final String GET_LIVE_JOB_IDS =
			"SELECT job_id, machine_id, job_state, keepalive_timestamp "
					+ "FROM jobs WHERE job_state != ? "
					+ "ORDER BY job_id DESC LIMIT ? OFFSET ?";

	@Parameter("job_id")
	@ResultColumn("machine_id")
	@ResultColumn("width")
	@ResultColumn("height")
	@ResultColumn("root_id")
	@ResultColumn("job_state")
	@ResultColumn("keepalive_timestamp")
	@ResultColumn("keepalive_host")
	@ResultColumn("create_timestamp")
	@ResultColumn("death_reason")
	@ResultColumn("death_timestamp")
	@SingleRowResult
	protected static final String GET_JOB =
			"SELECT machine_id, width, height, root_id, job_state, "
					+ "keepalive_timestamp, keepalive_host, create_timestamp, "
					+ "death_reason, death_timestamp "
					+ "FROM jobs WHERE job_id = ? LIMIT 1";

	@Parameter("machine_id")
	@Parameter("owner")
	@Parameter("keepalive_interval")
	@Parameter("keepalive_timestamp")
	@Parameter("create_timestamp")
	@GeneratesID
	protected static final String INSERT_JOB = "INSERT INTO jobs("
			+ "machine_id, owner, keepalive_interval, keepalive_timestamp, "
			+ "create_timestamp) " //
			+ "VALUES (?, ?, ?, ?, ?)";

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
	@Parameter("cabinet")
	@Parameter("frame")
	@Parameter("board")
	@GeneratesID
	protected static final String INSERT_REQ_LOCATION =
			"INSERT INTO job_request(job_id, cabinet, frame, board) "
					+ "VALUES (?, ?, ?, ?)";

	@Parameter("machine_id")
	@Parameter("chip_x")
	@Parameter("chip_y")
	@ResultColumn("board_id")
	@ResultColumn("address")
	@ResultColumn("bmp_id")
	@ResultColumn("board_nnum")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("job_id")
	@ResultColumn("machine_name")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("chip_x")
	@ResultColumn("chip_y")
	@SingleRowResult
	protected static final String FIND_BOARD_BY_CHIP =
			"SELECT boards.board_id, address, bmp_id, board_num, x, y, "
					+ "job_id, m.machine_name, bmp.cabinet, bmp.frame, "
					+ "boards.board_num, root_x + bmc.chip_x AS chip_x,"
					+ "root_y + bmc.chip_y AS chip_y FROM boards "
					+ "JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "JOIN machines AS m ON boards.machine_id = m.machine_id "
					+ "JOIN board_model_coords AS bmc "
					+ "ON m.board_model = bmc.model "
					+ "WHERE boards.machine_id = ? "
					+ "AND chip_x = ? AND chip_y = ? LIMIT 1";

	@Parameter("machine_id")
	@Parameter("cabinet")
	@Parameter("frame")
	@Parameter("board")
	@ResultColumn("board_id")
	@ResultColumn("address")
	@ResultColumn("bmp_id")
	@ResultColumn("board_nnum")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("job_id")
	@ResultColumn("machine_name")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("chip_x")
	@ResultColumn("chip_y")
	@SingleRowResult
	protected static final String FIND_BOARD_BY_CFB =
			"SELECT boards.board_id, address, bmp_id, board_num, x, y, "
					+ "job_id, m.machine_name, bmp.cabinet, bmp.frame, "
					+ "boards.board_num, root_x AS chip_x, root_y AS chip_y "
					+ "FROM boards JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "JOIN machines AS m ON boards.machine_id = m.machine_id "
					+ "WHERE boards.machine_id = ? AND bmp.cabinet = ? "
					+ "AND bmp.frame = ? AND boards.board_num = ? LIMIT 1";

	@Parameter("machine_id")
	@Parameter("x")
	@Parameter("y")
	@Parameter("z")
	@ResultColumn("board_id")
	@ResultColumn("address")
	@ResultColumn("bmp_id")
	@ResultColumn("board_nnum")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("job_id")
	@ResultColumn("machine_name")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("chip_x")
	@ResultColumn("chip_y")
	@SingleRowResult
	protected static final String FIND_BOARD_BY_XYZ =
			"SELECT boards.board_id, address, bmp_id, board_num, x, y, "
					+ "job_id, m.machine_name, bmp.cabinet, bmp.frame, "
					+ "boards.board_num, root_x AS chip_x, root_y AS chip_y "
					+ "FROM boards JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "JOIN machines AS m ON boards.machine_id = m.machine_id "
					+ "WHERE boards.machine_id = ? AND boards.x = ? "
					+ "AND boards.y = ? AND 0 = ? LIMIT 1";

	@Parameter("machine_id")
	@ResultColumn("address")
	@SingleRowResult
	protected static final String GET_ROOT_BMP_ADDRESS =
			"SELECT bmp.address FROM bmp "
					+ "JOIN boards ON boards.bmp_id = bmp.bmp_id WHERE "
					+ "boards.machine_id = ? AND boards.x = 0 AND boards.y = 0 "
					+ "LIMIT 1";

	@Parameter("machine_id")
	@ResultColumn("tag")
	protected static final String GET_TAGS =
			"SELECT tag FROM tags WHERE machine_id = ?";

	@Parameter("keepalive_timestamp")
	@Parameter("keepalive_host")
	@Parameter("job_id")
	@Parameter("job_state:DESTROYED")
	protected static final String UPDATE_KEEPALIVE =
			"UPDATE jobs SET keepalive_timestamp = ?, keepalive_host = ? "
					+ "WHERE job_id = ? AND job_state != ?";

	@Parameter("job_state:DESTROYED")
	@Parameter("death_reason")
	@Parameter("death_timestamp")
	@Parameter("job_id")
	@Parameter("job_state:DESTROYED")
	protected static final String DESTROY_JOB = "UPDATE jobs SET "
			+ "job_state = ?, death_reason = ?, death_timestamp = ? "
			+ "WHERE job_id = ? AND job_state != ?";

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
	@ResultColumn("root_x")
	@ResultColumn("root_y")
	protected static final String GET_BOARD_CONNECT_INFO =
			"SELECT board_id, address, x, y, root_x, root_y "
					+ "FROM boards WHERE allocated_job = ? "
					+ "ORDER BY x ASC, y ASC";

	@Parameter("board_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("root_x")
	@ResultColumn("root_y")
	@SingleRowResult
	protected static final String GET_ROOT_COORDS =
			"SELECT x, y, root_x, root_y FROM boards "
					+ "WHERE board_id = ? LIMIT 1";

	/** How we get the list of allocation tasks. */
	@ResultColumn("req_id")
	@ResultColumn("job_id")
	@ResultColumn("num_boards")
	@ResultColumn("width")
	@ResultColumn("height")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board")
	@ResultColumn("machine_id")
	@ResultColumn("max_dead_boards")
	protected static final String GET_TASKS =
			"SELECT job_request.req_id, job_request.job_id,"
					+ "  job_request.num_boards,"
					+ "	 job_request.width, job_request.height,"
					+ "  job_request.cabinet, job_request.frame,"
					+ "	 job_request.board, jobs.machine_id AS machine_id,"
					+ "  job_request.max_dead_boards "
					+ "FROM job_request JOIN jobs"
					+ "  ON job_request.job_id = jobs.job_id ORDER BY req_id";

	/** How we delete an allocation task. */
	@Parameter("request_id")
	protected static final String DELETE_TASK =
			"DELETE FROM job_request WHERE req_id = ?";

	/** How we tell a job that it is allocated. */
	@Parameter("width")
	@Parameter("height")
	@Parameter("job_state")
	@Parameter("num_pending")
	@Parameter("machine_id")
	@Parameter("root_x")
	@Parameter("root_y")
	@Parameter("job_id")
	protected static final String ALLOCATE_BOARDS_JOB = "UPDATE jobs SET "
			+ "width = ?, height = ?, job_state = ?, num_pending = ?,"
			+ "root_id = (SELECT board_id FROM boards WHERE "
			+ "machine_id = ? AND root_x = ? AND root_y = ?) "
			+ "WHERE job_id = ?";

	/** How we tell a board that it is allocated. */
	@Parameter("job_id")
	@Parameter("machine_id")
	@Parameter("root_x")
	@Parameter("root_y")
	protected static final String ALLOCATE_BOARDS_BOARD =
			"UPDATE boards SET allocated_job = ? "
					+ "WHERE machine_id = ? AND root_x = ? AND root_y = ? "
					+ "AND may_be_allocated > 0";

	@Parameter("job_state:DESTROYED")
	@Parameter("now")
	@ResultColumn("job_id")
	protected static final String FIND_EXPIRED_JOBS = //
			"SELECT job_id FROM jobs " //
					+ "WHERE job_state != ? "
					+ "AND keepalive_timestamp + keepalive_interval < ?";

	/** How we set the number of pending changes for a job. */
	@Parameter("num_pending")
	@Parameter("job_id")
	protected static final String SET_NUM_PENDING =
			"UPDATE jobs SET num_pending = ? WHERE job_id = ?";

	@Parameter("job_state:DESTROYED")
	@Parameter("timestamp:NOW")
	@Parameter("job_id")
	@Parameter("job_state:DESTROYED")
	protected static final String MARK_JOB_DESTROYED =
			"UPDATE jobs SET job_state = ?, death_timestamp = ? "
					+ "WHERE job_id = ? AND job_state != ?";

	@Parameter("job_id")
	protected static final String KILL_JOB_ALLOC_TASK =
			"DELETE FROM job_request WHERE job_id = ?";

	@Parameter("job_id")
	protected static final String KILL_JOB_PENDING =
			"DELETE FROM pending_changes WHERE job_id = ?";

	@Parameter("job_id")
	@Parameter("power")
	@Parameter("job_id")
	@Parameter("power_state:OFF")
	protected static final String ISSUE_BOARD_OFF_FOR_JOB =
			"INSERT INTO pending_changes(job_id, \"power\", board_id) "
					+ "SELECT ?, ?, board_id FROM boards "
					+ "WHERE job_id = ? AND power_state != ?";

	// SQL loaded from files because it is too complicated otherwise!

	@Parameter("width")
	@Parameter("height")
	@Parameter("machine_id")
	@Parameter("max_dead_boards")
	@ResultColumn("id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("available")
	@Value("classpath:find_rectangle.sql")
	protected Resource findRectangle;

	@Parameter("machine_id")
	@Parameter("cabinet")
	@Parameter("frame")
	@Parameter("board")
	@ResultColumn("x")
	@ResultColumn("y")
	@SingleRowResult
	@Value("classpath:find_location.sql")
	protected Resource findLocation;

	@Parameter("job_id")
	@Parameter("machine_id")
	@Parameter("x")
	@Parameter("y")
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
}
