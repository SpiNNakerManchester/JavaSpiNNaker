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

import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl;
import uk.ac.manchester.spinnaker.alloc.admin.UserControl;
import uk.ac.manchester.spinnaker.alloc.allocator.AllocatorTask;
import uk.ac.manchester.spinnaker.alloc.allocator.DirInfo;
import uk.ac.manchester.spinnaker.alloc.allocator.QuotaManager;
import uk.ac.manchester.spinnaker.alloc.allocator.Spalloc;
import uk.ac.manchester.spinnaker.storage.GeneratesID;
import uk.ac.manchester.spinnaker.storage.Parameter;
import uk.ac.manchester.spinnaker.storage.ResultColumn;
import uk.ac.manchester.spinnaker.storage.SingleRowResult;

/**
 * The literal SQL queries used in this package.
 * <p>
 * The schema they query against (defined in {@code spalloc.sql}) is: <br>
 * <img src="doc-files/schema.png" width="95%">
 *
 * @author Donal Fellows
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public abstract class SQLQueries {
	/** Get basic information about all machines. */
	@ResultColumn("machine_id")
	@ResultColumn("machine_name")
	@ResultColumn("width")
	@ResultColumn("height")
	protected static final String GET_ALL_MACHINES =
			"SELECT machine_id, machine_name, width, height FROM machines "
					+ "ORDER BY machine_name ASC";

	/** Get the machine names in alphabetical order. */
	@ResultColumn("machine_name")
	protected static final String LIST_MACHINE_NAMES =
			"SELECT machine_name FROM machines ORDER BY machine_name ASC";

	/** Get basic information about a specific machine. Looks up by ID. */
	@Parameter("machine_id")
	@ResultColumn("machine_id")
	@ResultColumn("machine_name")
	@ResultColumn("width")
	@ResultColumn("height")
	@SingleRowResult
	protected static final String GET_MACHINE_BY_ID =
			"SELECT machine_id, machine_name, width, height FROM machines "
					+ "WHERE machine_id = :machine_id LIMIT 1";

	/** Get basic information about a specific machine. Looks up by name. */
	@Parameter("machine_name")
	@ResultColumn("machine_id")
	@ResultColumn("machine_name")
	@ResultColumn("width")
	@ResultColumn("height")
	@SingleRowResult
	protected static final String GET_NAMED_MACHINE =
			"SELECT machine_id, machine_name, width, height FROM machines "
					+ "WHERE machine_name = :machine_name LIMIT 1";

	/** Count things on a machine. */
	@Parameter("machine_id")
	@ResultColumn("board_count")
	@ResultColumn("in_use")
	@ResultColumn("num_jobs")
	@SingleRowResult
	protected static final String COUNT_MACHINE_THINGS =
			"WITH args(m) AS (VALUES (:machine_id)), "
					+ "b AS (SELECT * from boards,args WHERE machine_id = m), "
					+ "bc AS (SELECT COUNT(*) AS c FROM b), "
					+ "iu AS (SELECT COUNT(*) AS c FROM b "
					+ "WHERE allocated_job IS NOT NULL), "
					+ "jc AS (SELECT COUNT(*) AS c FROM jobs,args "
					+ "WHERE machine_id = m) "
					+ "SELECT bc.c AS board_count, iu.c AS in_use, "
					+ "jc.c AS num_jobs FROM bc, iu, jc";

	/** Get basic information about jobs. Supports paging. */
	@Parameter("limit")
	@Parameter("offset")
	@ResultColumn("job_id")
	@ResultColumn("machine_id")
	@ResultColumn("job_state")
	@ResultColumn("keepalive_timestamp")
	protected static final String GET_JOB_IDS =
			"SELECT job_id, machine_id, job_state, keepalive_timestamp "
					+ "FROM jobs ORDER BY job_id DESC "
					+ "LIMIT :limit OFFSET :offset";

	/** Get basic information about live jobs. Supports paging. */
	@Parameter("limit")
	@Parameter("offset")
	@ResultColumn("job_id")
	@ResultColumn("machine_id")
	@ResultColumn("job_state")
	@ResultColumn("keepalive_timestamp")
	protected static final String GET_LIVE_JOB_IDS =
			"SELECT job_id, machine_id, job_state, keepalive_timestamp "
					+ "FROM jobs WHERE job_state != 4 " // DESTROYED
					+ "ORDER BY job_id DESC LIMIT :limit OFFSET :offset";

	/** Get basic information about a specific job. */
	@Parameter("job_id")
	@ResultColumn("machine_id")
	@ResultColumn("machine_name")
	@ResultColumn("width")
	@ResultColumn("height")
	@ResultColumn("depth")
	@ResultColumn("root_id")
	@ResultColumn("job_state")
	@ResultColumn("keepalive_timestamp")
	@ResultColumn("keepalive_host")
	@ResultColumn("keepalive_interval")
	@ResultColumn("create_timestamp")
	@ResultColumn("death_reason")
	@ResultColumn("death_timestamp")
	@ResultColumn("original_request")
	@ResultColumn("owner")
	@SingleRowResult
	protected static final String GET_JOB =
			"SELECT jobs.machine_id, machines.machine_name, "
					+ "jobs.width, jobs.height, jobs.depth, "
					+ "root_id, job_state, keepalive_timestamp, "
					+ "keepalive_host, keepalive_interval, create_timestamp, "
					+ "death_reason, death_timestamp, original_request, "
					+ "user_info.user_name AS owner FROM jobs "
					+ "JOIN user_info ON jobs.owner = user_info.user_id "
					+ "JOIN machines ON jobs.machine_id = machines.machine_id "
					+ "WHERE job_id = :job_id LIMIT 1";

	/** Get the chip dimensions of a job. */
	@Parameter("job_id")
	@ResultColumn("width")
	@ResultColumn("height")
	@SingleRowResult
	protected static final String GET_JOB_CHIP_DIMENSIONS =
			"WITH b AS (SELECT * FROM boards WHERE allocated_job = :job_id), "
			+ "c AS (SELECT root_x + chip_x AS x, root_y + chip_y AS y "
			+ "FROM b JOIN machines ON b.machine_id = machines.machine_id "
			+ "JOIN board_model_coords ON "
			+ "machines.board_model = board_model_coords.model) "
			+ "SELECT MAX(x) - MIN(x) + 1 AS width, "
			+ "MAX(y) - MIN(y) + 1 AS height FROM c LIMIT 1";

	/** Get what boards are allocated to a job (that is queued or ready). */
	@Parameter("job_id")
	@ResultColumn("board_id")
	protected static final String GET_JOB_BOARDS =
			"SELECT board_id FROM boards JOIN jobs "
					+ "ON boards.allocated_job = jobs.job_id "
					+ "WHERE boards.allocated_job = :job_id "
					// job is QUEUED or READY
					+ "AND (jobs.job_state IN (1, 3))";

	/** Gets information about live jobs. */
	@ResultColumn("job_id")
	@ResultColumn("machine_id")
	@ResultColumn("create_timestamp")
	@ResultColumn("keepalive_interval")
	@ResultColumn("job_state")
	@ResultColumn("allocation_size")
	@ResultColumn("keepalive_host")
	@ResultColumn("user_name")
	protected static final String LIST_LIVE_JOBS =
			"SELECT job_id, machine_id, create_timestamp, keepalive_interval, "
					+ "job_state, allocation_size, keepalive_host, user_name "
					+ "FROM jobs "
					+ "JOIN user_info ON jobs.owner = user_info.user_id "
					+ "WHERE job_state != 4"; // DESTROYED

	/** Counts the number of powered-on boards of a job. */
	@Parameter("job_id")
	@ResultColumn("c")
	@SingleRowResult
	protected static final String COUNT_POWERED_BOARDS =
			"SELECT COUNT(*) AS c FROM boards "
					+ "WHERE allocated_job = :job_id AND board_power";

	/** Get the coordinates of the root chip of a board. */
	@Parameter("board_id")
	@ResultColumn("root_x")
	@ResultColumn("root_y")
	@SingleRowResult
	protected static final String GET_ROOT_OF_BOARD =
			"SELECT root_x, root_y FROM boards WHERE board_id = :board_id "
					+ "LIMIT 1";

	/** Create a job. */
	@Parameter("machine_id")
	@Parameter("owner")
	@Parameter("keepalive_interval")
	@Parameter("original_request")
	@GeneratesID
	protected static final String INSERT_JOB = "INSERT INTO jobs("
			+ "machine_id, owner, keepalive_interval, original_request, "
			+ "keepalive_timestamp, create_timestamp, job_state) "
			+ "VALUES(:machine_id, :owner, :keepalive_interval, "
			+ ":original_request, strftime('%s','now'), strftime('%s','now'), "
			+ /* QUEUED */ "1)";

	/** Create a request to allocate a number of boards. */
	@Parameter("job_id")
	@Parameter("num_boards")
	@Parameter("max_dead_boards")
	@GeneratesID
	protected static final String INSERT_REQ_N_BOARDS =
			"INSERT INTO job_request(job_id, num_boards, max_dead_boards) "
					+ "VALUES (:job_id, :num_boards, :max_dead_boards)";

	/** Create a request to allocate a rectangle of boards. */
	@Parameter("job_id")
	@Parameter("width")
	@Parameter("height")
	@Parameter("max_dead_boards")
	@GeneratesID
	protected static final String INSERT_REQ_SIZE =
			"INSERT INTO job_request(job_id, width, height, max_dead_boards) "
					+ "VALUES (:job_id, :width, :height, :max_dead_boards)";

	/** Create a request to allocate a specific board. */
	@Parameter("job_id")
	@Parameter("x")
	@Parameter("y")
	@Parameter("z")
	@GeneratesID
	protected static final String INSERT_REQ_LOCATION =
			"INSERT INTO job_request(job_id, x, y, z) "
					+ "VALUES (:job_id, :x, :y, :z)";

	/** Get the address of the BMP of the root board of the machine. */
	@Parameter("machine_id")
	@ResultColumn("address")
	@SingleRowResult
	protected static final String GET_ROOT_BMP_ADDRESS =
			"SELECT bmp.address FROM bmp "
					+ "JOIN boards ON boards.bmp_id = bmp.bmp_id WHERE "
					+ "boards.machine_id = :machine_id "
					+ "AND boards.x = 0 AND boards.y = 0 LIMIT 1";

	/**
	 * Get the boards of a machine that can be used. Excludes disabled boards.
	 */
	@Parameter("machine_id")
	@ResultColumn("board_num")
	protected static final String GET_BOARD_NUMBERS =
			"SELECT board_num FROM boards WHERE machine_id = :machine_id "
					+ "AND (functioning IS NULL OR functioning != 0) "
					+ "ORDER BY board_num ASC";

	/**
	 * Get the boards (and related info) of a machine that are in service.
	 */
	// FIXME test
	@Parameter("machine_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("address")
	protected static final String GET_LIVE_BOARDS =
			"SELECT x, y, z, bmp.cabinet, bmp.frame, board_num, boards.address "
					+ "FROM boards JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "WHERE boards.machine_id = :machine_id "
					+ "AND functioning IS 1 ORDER BY z ASC, x ASC, y ASC";

	/**
	 * Get the boards (and related info) of a machine that have been disabled.
	 */
	@Parameter("machine_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("address")
	protected static final String GET_DEAD_BOARDS =
			"SELECT x, y, z, bmp.cabinet, bmp.frame, board_num, boards.address "
					+ "FROM boards JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "WHERE boards.machine_id = :machine_id "
					+ "AND functioning IS 0 ORDER BY z ASC, x ASC, y ASC";

	/**
	 * Get the coords of boards assigned to a job.
	 */
	@Parameter("job_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("address")
	protected static final String GET_JOB_BOARD_COORDS =
			"SELECT x, y, z, bmp.cabinet, bmp.frame, board_num, boards.address "
					+ "FROM boards JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "WHERE boards.allocated_job = :job_id "
					+ "ORDER BY z ASC, x ASC, y ASC";

	/** Get basic info about active jobs on a machine. */
	@Parameter("machine_id")
	@ResultColumn("job_id")
	@ResultColumn("owner_name")
	protected static final String GET_MACHINE_JOBS =
			"SELECT job_id, user_info.user_name AS owner_name FROM jobs "
					+ "JOIN user_info ON jobs.owner = user_info.user_id "
					+ "WHERE machine_id = :machine_id AND job_state != 4 "
					// job is not DESTROYED
					+ "ORDER BY job_id ASC";

	/** Get the boards that are available for allocation. */
	@Parameter("machine_id")
	@ResultColumn("board_num")
	protected static final String GET_AVAILABLE_BOARD_NUMBERS =
			"SELECT board_num FROM boards "
					+ "WHERE machine_id = :machine_id AND may_be_allocated "
					+ "ORDER BY board_num ASC";

	/**
	 * Get a machine's tags. Theoretically when selecting a machine by tags we
	 * should put the query in the DB, but it is awkward to move a list into SQL
	 * as part of a query and we don't really ever have that many machines or
	 * tags. So we just pull the collection of tags and do the match in Java.
	 */
	@Parameter("machine_id")
	@ResultColumn("tag")
	protected static final String GET_TAGS =
			"SELECT tag FROM tags WHERE machine_id = :machine_id";

	/** Update the keepalive timestamp. */
	@Parameter("keepalive_host")
	@Parameter("job_id")
	protected static final String UPDATE_KEEPALIVE =
			"UPDATE jobs SET keepalive_timestamp = strftime('%s','now'), "
					+ "keepalive_host = :keepalive_host WHERE job_id = :job_id "
					+ "AND job_state != 4"; // DESTROYED

	/** Mark a job as dead. */
	@Parameter("death_reason")
	@Parameter("job_id")
	protected static final String DESTROY_JOB =
			"UPDATE jobs SET job_state = 4, death_reason = :death_reason "
					// 4 = DESTROYED
					+ "WHERE job_id = :job_id AND job_state != 4";

	/**
	 * Get the number of boards that are allocated to a job that are switched
	 * on.
	 */
	@Parameter("job_id")
	@ResultColumn("total_on")
	@SingleRowResult
	protected static final String GET_BOARD_POWER =
			"SELECT sum(board_power) AS total_on FROM boards "
					+ "WHERE allocated_job = :job_id";

	/** Get connection info for board allocated to a job. */
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
					+ "FROM boards WHERE allocated_job = :job_id "
					+ "ORDER BY x ASC, y ASC";

	/** Get the coordinates of a board. */
	@Parameter("board_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("root_x")
	@ResultColumn("root_y")
	@SingleRowResult
	protected static final String GET_ROOT_COORDS =
			"SELECT x, y, z, root_x, root_y FROM boards "
					+ "WHERE board_id = :board_id LIMIT 1";

	/** Delete an allocation task. */
	@Parameter("request_id")
	protected static final String DELETE_TASK =
			"DELETE FROM job_request WHERE req_id = :request_id";

	/** Find a single free board. */
	@Parameter("machine_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@SingleRowResult
	protected static final String FIND_FREE_BOARD =
			"SELECT x, y, z FROM boards "
					+ "WHERE machine_id = :machine_id AND may_be_allocated "
					+ "ORDER BY power_off_timestamp ASC LIMIT 1";

	/**
	 * Tell a job that it is allocated. Doesn't set the state.
	 *
	 * @see #SET_STATE_PENDING
	 * @see AllocatorTask
	 */
	@Parameter("width")
	@Parameter("height")
	@Parameter("depth")
	@Parameter("board_id")
	@Parameter("num_boards")
	@Parameter("job_id")
	protected static final String ALLOCATE_BOARDS_JOB = "UPDATE jobs SET "
			+ "width = :width, height = :height, depth = :depth, "
			+ "num_pending = 0, root_id = :board_id, "
			+ "allocation_size = :num_boards WHERE job_id = :job_id";

	/** Get a board's ID by its coordinates. */
	@Parameter("machine_id")
	@Parameter("x")
	@Parameter("y")
	@Parameter("z")
	@ResultColumn("board_id")
	@SingleRowResult
	protected static final String GET_BOARD_BY_COORDS =
			"SELECT board_id FROM boards WHERE " + "machine_id = :machine_id "
					+ "AND x = :x AND y = :y AND z = :z "
					+ "AND may_be_allocated LIMIT 1";

	/**
	 * Tell a board that it is allocated.
	 *
	 * @see AllocatorTask
	 */
	@Parameter("job_id")
	@Parameter("board_id")
	protected static final String ALLOCATE_BOARDS_BOARD =
			"UPDATE boards SET allocated_job = :job_id "
					+ "WHERE board_id = :board_id";

	/**
	 * Set the power state of a board. Related timestamps are updated by
	 * trigger.
	 */
	@Parameter("board_power")
	@Parameter("board_id")
	protected static final String SET_BOARD_POWER =
			"UPDATE boards SET board_power = :board_power "
					+ "WHERE board_id = :board_id";

	/**
	 * Find jobs that have expired their keepalive interval.
	 *
	 * @see AllocatorTask
	 */
	@ResultColumn("job_id")
	protected static final String FIND_EXPIRED_JOBS = //
			"SELECT job_id FROM jobs " //
					+ "WHERE job_state != 4 " // DESTROYED
					+ "AND keepalive_timestamp + keepalive_interval < "
					+ "strftime('%s','now')";

	/**
	 * Set the state and number of pending changes for a job.
	 *
	 * @see AllocatorTask
	 */
	@Parameter("job_state")
	@Parameter("num_pending")
	@Parameter("job_id")
	protected static final String SET_STATE_PENDING =
			"UPDATE jobs SET job_state = :job_state, "
					+ "num_pending = :num_pending WHERE job_id = :job_id";

	/** Delete a request to allocate resources for a job. */
	@Parameter("job_id")
	protected static final String KILL_JOB_ALLOC_TASK =
			"DELETE FROM job_request WHERE job_id = :job_id";

	/** Delete a request to change the power of boards allocated to a job. */
	@Parameter("job_id")
	protected static final String KILL_JOB_PENDING =
			"DELETE FROM pending_changes WHERE job_id = :job_id";

	/**
	 * Delete a request to change the power of a board. Used once the change has
	 * been completed.
	 */
	@Parameter("change_id")
	protected static final String FINISHED_PENDING =
			"DELETE FROM pending_changes WHERE change_id = :change_id";

	/**
	 * Get descriptions of how to move from a board to its neighbours.
	 *
	 * @see DirInfo
	 */
	@ResultColumn("z")
	@ResultColumn("direction")
	@ResultColumn("dx")
	@ResultColumn("dy")
	@ResultColumn("dz")
	protected static final String LOAD_DIR_INFO =
			"SELECT z, direction, dx, dy, dz FROM movement_directions";

	/**
	 * Get how many requests to change the power state of a board are currently
	 * waiting to be processed.
	 */
	@ResultColumn("c")
	@SingleRowResult
	protected static final String COUNT_PENDING_CHANGES =
			"SELECT count(*) AS c FROM pending_changes";

	/**
	 * Get the requests (not already being processed) to change the power of a
	 * board allocated to a job.
	 */
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
	@ResultColumn("from_state")
	@ResultColumn("to_state")
	protected static final String GET_CHANGES = "SELECT * FROM pending_changes "
			+ "WHERE job_id = :job_id AND NOT in_progress";

	/**
	 * Set the progress status of a request to change the power state of a
	 * board.
	 */
	@Parameter("in_progress")
	@Parameter("change_id")
	protected static final String SET_IN_PROGRESS =
			"UPDATE pending_changes SET in_progress = :in_progress "
					+ "WHERE change_id = :change_id";

	/**
	 * Insert a BMP.
	 *
	 * @see MachineDefinitionLoader
	 */
	@Parameter("machine_id")
	@Parameter("address")
	@Parameter("cabinet")
	@Parameter("frame")
	@GeneratesID
	protected static final String INSERT_BMP =
			"INSERT INTO bmp(machine_id, address, cabinet, frame) "
					+ "VALUES(:machine_id, :address, :cabinet, :frame)";

	/**
	 * Insert a board.
	 *
	 * @see MachineDefinitionLoader
	 */
	@Parameter("machine_id")
	@Parameter("address")
	@Parameter("bmp_id")
	@Parameter("board_num")
	@Parameter("x")
	@Parameter("y")
	@Parameter("z")
	@Parameter("root_x")
	@Parameter("root_y")
	@Parameter("enabled")
	@GeneratesID
	protected static final String INSERT_BOARD = "INSERT INTO boards("
			+ "machine_id, address, bmp_id, board_num, x, y, z, "
			+ "root_x, root_y, functioning) VALUES("
			+ ":machine_id, :address, :bmp_id, :board_num, :x, :y, :z, "
			+ ":root_x, :root_y, :enabled)";

	/**
	 * Insert a link.
	 *
	 * @see MachineDefinitionLoader
	 */
	@Parameter("board_1")
	@Parameter("dir_1")
	@Parameter("board_2")
	@Parameter("dir_2")
	@Parameter("live")
	@GeneratesID
	protected static final String INSERT_LINK =
			"INSERT OR IGNORE INTO links(board_1, dir_1, board_2, dir_2, live) "
					+ "VALUES (:board_1, :dir_1, :board_2, :dir_2, :live)";

	/**
	 * Insert a machine.
	 *
	 * @see MachineDefinitionLoader
	 */
	@Parameter("name")
	@Parameter("width")
	@Parameter("height")
	@Parameter("depth")
	@GeneratesID
	protected static final String INSERT_MACHINE_SPINN_5 =
			"INSERT INTO machines(machine_name, "
					+ "width, height, depth, board_model) "
					+ "VALUES(:name, :width, :height, :depth, 5)";

	/**
	 * Insert a tag.
	 *
	 * @see MachineDefinitionLoader
	 */
	@Parameter("machine_id")
	@Parameter("tag")
	@GeneratesID
	protected static final String INSERT_TAG =
			"INSERT INTO tags(machine_id, tag) VALUES(:machine_id, :tag)";

	/** Get a board's ID given it's triad coordinates. */
	@Parameter("machine_name")
	@Parameter("x")
	@Parameter("y")
	@Parameter("z")
	@ResultColumn("board_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("address")
	@SingleRowResult
	protected static final String FIND_BOARD_BY_NAME_AND_XYZ =
			"SELECT board_id, boards.x, boards.y, boards.z, "
					+ "bmp.cabinet, bmp.frame, board_num, "
					+ "boards.address FROM boards JOIN machines "
					+ "ON boards.machine_id = machines.machine_id "
					+ "JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "WHERE machine_name = :machine_name "
					+ "AND x = :x AND y = :y AND z = :z LIMIT 1";

	/** Get a board's ID given it's physical coordinates. */
	@Parameter("machine_name")
	@Parameter("cabinet")
	@Parameter("frame")
	@Parameter("board")
	@ResultColumn("board_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("address")
	@SingleRowResult
	protected static final String FIND_BOARD_BY_NAME_AND_CFB =
			"SELECT board_id, boards.x, boards.y, boards.z, "
					+ "bmp.cabinet, bmp.frame, board_num, "
					+ "boards.address FROM boards JOIN machines "
					+ "ON boards.machine_id = machines.machine_id "
					+ "JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "WHERE machine_name = :machine_name "
					+ "AND bmp.cabinet = :cabinet AND bmp.frame = :frame "
					+ "AND boards.board_num = :board LIMIT 1";

	/** Get a board's ID given it's IP address. */
	@Parameter("machine_name")
	@Parameter("address")
	@ResultColumn("board_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("address")
	@SingleRowResult
	protected static final String FIND_BOARD_BY_NAME_AND_IP_ADDRESS =
			"SELECT board_id, boards.x, boards.y, boards.z, "
					+ "bmp.cabinet, bmp.frame, board_num, "
					+ "boards.address FROM boards JOIN machines "
					+ "ON boards.machine_id = machines.machine_id "
					+ "JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "WHERE machine_name = :machine_name "
					+ "AND boards.address = :address LIMIT 1";

	/**
	 * Get the value of a board's {@code functioning} column.
	 *
	 * @see MachineStateControl
	 */
	@Parameter("board_id")
	@ResultColumn("functioning")
	@SingleRowResult
	protected static final String GET_FUNCTIONING_FIELD =
			"SELECT functioning FROM boards "
					+ "WHERE board_id = :board_id LIMIT 1";

	/**
	 * Set the value of a board's {@code functioning} column. Enables or
	 * disables allocation of the board.
	 *
	 * @see MachineStateControl
	 */
	@Parameter("enabled")
	@Parameter("board_id")
	protected static final String SET_FUNCTIONING_FIELD =
			"UPDATE boards SET functioning = :enabled "
					+ "WHERE board_id = :board_id";

	/**
	 * Get the quota for a user.
	 *
	 * @see QuotaManager
	 */
	@Parameter("machine_id")
	@Parameter("user_name")
	@ResultColumn("quota")
	@ResultColumn("user_id")
	@SingleRowResult
	protected static final String GET_USER_QUOTA =
			"SELECT quota, user_info.user_id AS user_id FROM quotas "
					+ "JOIN user_info ON quotas.user_id = user_info.user_id "
					+ "WHERE quotas.machine_id = :machine_id "
					+ "AND user_info.user_name = :user_name LIMIT 1";

	/**
	 * Get the current non-consolidated usage for a user.
	 *
	 * @see QuotaManager
	 */
	@Parameter("machine_id")
	@Parameter("user_id")
	@ResultColumn("current_usage")
	@SingleRowResult
	protected static final String GET_CURRENT_USAGE =
			"SELECT SUM(usage) AS current_usage FROM jobs_usage "
					+ "WHERE machine_id = :machine_id AND owner = :user_id";

	/**
	 * Get usage of a job and the quota against which that applies.
	 *
	 * @see QuotaManager
	 */
	@Parameter("machine_id")
	@Parameter("job_id")
	@ResultColumn("usage")
	@ResultColumn("quota")
	@SingleRowResult
	protected static final String GET_JOB_USAGE_AND_QUOTA =
			"SELECT [usage], quota FROM jobs_usage JOIN quotas "
					+ "ON quotas.machine_id = jobs_usage.machine_id "
					+ "AND quotas.user_id = jobs_usage.owner "
					+ "WHERE jobs_usage.machine_id = :machine_id "
					+ "AND :job_id = :job_id AND [usage] IS NOT NULL "
					+ "AND quota IS NOT NULL LIMIT 1";

	/**
	 * Get resource usage info about completed jobs that have yet to be
	 * consolidated into the main quota table.
	 *
	 * @see QuotaManager
	 */
	@ResultColumn("job_id")
	@ResultColumn("quota_id")
	@ResultColumn("usage")
	protected static final String GET_CONSOLIDATION_TARGETS =
			"SELECT job_id, quota_id, [usage] FROM jobs_usage "
					+ "WHERE complete AND quota_id IS NOT NULL";

	/**
	 * Reduce a user's quota on a machine by a specified amount.
	 *
	 * @see QuotaManager
	 */
	@Parameter("usage")
	@Parameter("quota_id")
	protected static final String DECREMENT_QUOTA =
			"UPDATE quotas SET quota = quota - :usage "
					+ "WHERE quota_id = :quota_id AND quota IS NOT NULL";

	/**
	 * Mark a job as having had its resource usage consolidated.
	 *
	 * @see QuotaManager
	 */
	@Parameter("job_id")
	protected static final String MARK_CONSOLIDATED =
			"UPDATE jobs SET accounted_for = 1 WHERE job_id = :job_id";

	/**
	 * Get details about a user. This is pretty much everything except their
	 * password.
	 *
	 * @see UserControl
	 */
	@Parameter("user_id")
	@ResultColumn("user_id")
	@ResultColumn("user_name")
	@ResultColumn("has_password")
	@ResultColumn("trust_level")
	@ResultColumn("locked")
	@ResultColumn("disabled")
	@ResultColumn("last_successful_login_timestamp")
	@ResultColumn("last_fail_timestamp")
	@SingleRowResult
	protected static final String GET_USER_DETAILS =
			"SELECT user_id, user_name, "
					+ "encrypted_password IS NOT NULL AS has_password, "
					+ "trust_level, locked, disabled, "
					+ "last_successful_login_timestamp, "
					+ "last_fail_timestamp FROM user_info "
					+ "WHERE user_id = :user_id LIMIT 1";

	/**
	 * Get a local user's basic details.
	 *
	 * @see UserControl
	 */
	protected static final String GET_LOCAL_USER_DETAILS =
			"SELECT user_id, user_name FROM user_info "
					+ "WHERE user_name = :user_name "
					+ "AND encrypted_password IS NOT NULL LIMIT 1";

	/**
	 * Get a user's quotas.
	 *
	 * @see UserControl
	 */
	@Parameter("user_id")
	@ResultColumn("machine_name")
	@ResultColumn("quota")
	protected static final String GET_QUOTA_DETAILS =
			"SELECT machines.machine_name, quotas.quota "
					+ "FROM quotas JOIN machines "
					+ "ON quotas.machine_id = machines.machine_id "
					+ "WHERE quotas.user_id = :user_id";

	/**
	 * Test if a user account is locked or disabled.
	 *
	 * @see LocalAuthProviderImpl
	 */
	@Parameter("username")
	@ResultColumn("user_id")
	@ResultColumn("locked")
	@ResultColumn("disabled")
	@SingleRowResult
	protected static final String IS_USER_LOCKED =
			"SELECT user_id, locked, disabled FROM user_info "
					+ "WHERE user_name = :username";

	/**
	 * Get the permissions for a user.
	 *
	 * @see LocalAuthProviderImpl
	 */
	@Parameter("user_id")
	@ResultColumn("trust_level")
	@ResultColumn("password")
	@SingleRowResult
	protected static final String GET_USER_AUTHORITIES =
			"SELECT trust_level, encrypted_password AS password FROM user_info "
					+ "WHERE user_id = :user_id "
					+ "AND encrypted_password IS NOT NULL LIMIT 1";

	/**
	 * Note the login success.
	 *
	 * @see LocalAuthProviderImpl
	 */
	@Parameter("user_id")
	protected static final String MARK_LOGIN_SUCCESS = "UPDATE user_info SET "
			+ "last_successful_login_timestamp = strftime('%s','now'), "
			+ "failure_count = 0 WHERE user_id = :user_id";

	/**
	 * Note the login failure.
	 *
	 * @see LocalAuthProviderImpl
	 */
	@Parameter("failure_limit")
	@Parameter("user_id")
	@ResultColumn("locked")
	@SingleRowResult
	protected static final String MARK_LOGIN_FAILURE =
			"UPDATE user_info SET failure_count = failure_count + 1, "
					+ "last_fail_timestamp = strftime('%s','now'), "
					+ "locked = (failure_count + 1 >= :failure_limit) "
					+ "WHERE user_id = :user_id RETURNING locked";

	/**
	 * Unlock accounts.
	 *
	 * @see LocalAuthProviderImpl
	 */
	@Parameter("lock_interval")
	@ResultColumn("user_name")
	protected static final String UNLOCK_LOCKED_USERS =
			"UPDATE user_info SET failure_count = 0, last_fail_timestamp = 0, "
					+ "locked = 0 WHERE last_fail_timestamp + :lock_interval "
					+ "< strftime('%s','now') AND locked RETURNING user_name";

	/**
	 * Set a quota for a user on each defined machine.
	 *
	 * @see LocalAuthProviderImpl
	 */
	@Parameter("user_id")
	@Parameter("quota")
	protected static final String ADD_QUOTA_FOR_ALL_MACHINES =
			"INSERT OR IGNORE INTO quotas(user_id, machine_id, quota) "
					+ "SELECT :user_id, machine_id, :quota FROM machines";

	/**
	 * Delete a user.
	 *
	 * @see UserControl
	 */
	@Parameter("user_id")
	protected static final String DELETE_USER =
			"DELETE FROM user_info WHERE user_id = :user_id";

	/**
	 * Get the ID of a user. Used for safety checks.
	 *
	 * @see Spalloc
	 * @see UserControl
	 */
	@Parameter("user_name")
	@ResultColumn("user_id")
	@SingleRowResult
	protected static final String GET_USER_ID = "SELECT user_id FROM user_info "
			+ "WHERE user_name = :user_name LIMIT 1";

	/**
	 * Set a quota on a machine.
	 *
	 * @see UserControl
	 */
	@Parameter("quota")
	@Parameter("user_id")
	@Parameter("machine_name")
	protected static final String SET_USER_QUOTA =
			"UPDATE quotas SET quota = :quota WHERE user_id = :user_id "
					+ "AND machine_id = (SELECT machine_id FROM machines "
					+ "WHERE machine_name = :machine_name)";

	/**
	 * Set the amount a user is trusted.
	 *
	 * @see UserControl
	 */
	@Parameter("trust")
	@Parameter("user_id")
	protected static final String SET_USER_TRUST =
			"UPDATE user_info SET trust_level = :trust "
					+ "WHERE user_id = :user_id";

	/**
	 * Set whether a user is locked. Can be used to unlock a user early.
	 *
	 * @see UserControl
	 */
	@Parameter("locked")
	@Parameter("user_id")
	protected static final String SET_USER_LOCKED =
			"UPDATE user_info SET locked = :locked WHERE user_id = :user_id";

	/**
	 * Set whether a user is administratively disabled.
	 *
	 * @see UserControl
	 */
	@Parameter("disabled")
	@Parameter("user_id")
	protected static final String SET_USER_DISABLED =
			"UPDATE user_info SET disabled = :disabled "
					+ "WHERE user_id = :user_id";

	/**
	 * Whether a user's password matches. Be aware that the result is a boolean
	 * <em>or {@code null}</em>.
	 *
	 * @see LocalAuthProviderImpl
	 * @see UserControl
	 */
	@Parameter("password")
	@Parameter("user_id")
	@ResultColumn("matches")
	@SingleRowResult
	protected static final String IS_USER_PASS_MATCHED =
			"SELECT match_password(:password, encrypted_password) AS matches "
					+ "FROM user_info WHERE user_id = :user_id LIMIT 1";

	/**
	 * Set a user's password. Passwords are either encrypted (with bcrypt) or
	 * {@code null} to indicate that they should be using some other system
	 * (OIDC?) to authenticate them.
	 *
	 * @see UserControl
	 */
	@Parameter("password")
	@Parameter("user_id")
	protected static final String SET_USER_PASS = "UPDATE user_info "
			+ "SET encrypted_password = encode_password(:password) "
			+ "WHERE user_id = :user_id";

	/**
	 * Set a user's name.
	 *
	 * @see UserControl
	 */
	@Parameter("user_name")
	@Parameter("user_id")
	protected static final String SET_USER_NAME =
			"UPDATE user_info SET user_name = :user_name "
					+ "WHERE user_id = :user_id";

	/**
	 * Get a list of all users.
	 *
	 * @see UserControl
	 */
	@ResultColumn("user_id")
	@ResultColumn("user_name")
	protected static final String LIST_ALL_USERS =
			"SELECT user_id, user_name FROM user_info";

	/**
	 * Create a user. Passwords are either encrypted (with bcrypt) or
	 * {@code null} to indicate that they should be using some other system
	 * (OIDC?) to authenticate them.
	 *
	 * @see UserControl
	 */
	@Parameter("user_name")
	@Parameter("password")
	@Parameter("trust_level")
	@Parameter("disabled")
	@GeneratesID
	protected static final String CREATE_USER =
			"INSERT OR IGNORE INTO user_info(user_name, encrypted_password, "
					+ "trust_level, disabled) VALUES(:user_name, "
					+ "encode_password(:password), :trust_level, :disabled)";

	/**
	 * Create a quota (in board-seconds) for a user on a machine.
	 *
	 * @see UserControl
	 */
	@Parameter("user_id")
	@Parameter("quota")
	@Parameter("machine_name")
	@GeneratesID
	protected static final String CREATE_QUOTA =
			"INSERT INTO quotas(user_id, quota, machine_id) "
					+ "SELECT :user_id, :quota, machine_id FROM machines "
					+ "WHERE machine_name = :machine_name";

	// SQL loaded from files because it is too complicated otherwise!

	/**
	 * Find a rectangle of triads of boards that may be allocated. The
	 * {@code max_dead_boards} gives the amount of allowance for non-allocatable
	 * resources to be made within the rectangle.
	 *
	 * @see AllocatorTask
	 */
	@Parameter("width")
	@Parameter("height")
	@Parameter("machine_id")
	@Parameter("max_dead_boards")
	@ResultColumn("id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@ResultColumn("available")
	@Value("classpath:queries/find_rectangle.sql")
	protected Resource findRectangle;

	/**
	 * Find an allocatable board at a specific physical location.
	 *
	 * @see AllocatorTask
	 */
	@Parameter("machine_id")
	@Parameter("cabinet")
	@Parameter("frame")
	@Parameter("board")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("z")
	@SingleRowResult
	@Value("classpath:queries/find_location.sql")
	protected Resource findLocation;

	/** Create a request to change the power status of a board. */
	@Parameter("job_id")
	@Parameter("board_id")
	@Parameter("from_state")
	@Parameter("to_state")
	@Parameter("power")
	@Parameter("fpga_n")
	@Parameter("fpga_s")
	@Parameter("fpga_e")
	@Parameter("fpga_w")
	@Parameter("fpga_nw")
	@Parameter("fpga_se")
	@GeneratesID
	@Value("classpath:queries/issue_change_for_job.sql")
	protected Resource issueChangeForJob;

	/**
	 * Count the number of <em>connected</em> boards (i.e., have at least one
	 * path over enabled links to the root board of the allocation) within a
	 * rectangle.
	 * <p>
	 * Ideally this would be part of {@link #findRectangle}, but both that query
	 * and this one are entirely complicated enough already! Also, we don't
	 * expect to have this query reject many candidate allocations.
	 *
	 * @see AllocatorTask
	 */
	@Parameter("machine_id")
	@Parameter("x")
	@Parameter("y")
	@Parameter("width")
	@Parameter("height")
	@ResultColumn("connected_size")
	@SingleRowResult
	@Value("classpath:queries/allocation_connected.sql")
	protected Resource countConnected;

	/**
	 * Get the links on the perimeter of the allocation to a job. The perimeter
	 * is defined as being the links between a board that is part of the
	 * allocation and a board that is not; it's <em>not</em> a geometric
	 * definition, but rather a relational algebraic one.
	 *
	 * @see AllocatorTask
	 */
	@Parameter("job_id")
	@ResultColumn("board_id")
	@ResultColumn("direction")
	@Value("classpath:queries/perimeter.sql")
	protected Resource getPerimeterLinks;

	/**
	 * Locate a board (using a full set of coordinates) based on global chip
	 * coordinates.
	 *
	 * @see Spalloc
	 */
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
	@Value("classpath:queries/find_board_by_global_chip.sql")
	protected Resource findBoardByGlobalChip;

	/**
	 * Locate a board (using a full set of coordinates) based on
	 * allocation-local chip coordinates.
	 *
	 * @see Spalloc
	 */
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
	@Value("classpath:queries/find_board_by_job_chip.sql")
	protected Resource findBoardByJobChip;

	/**
	 * Locate a board (using a full set of coordinates) based on logical triad
	 * coordinates.
	 *
	 * @see Spalloc
	 */
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
	@Value("classpath:queries/find_board_by_logical_coords.sql")
	protected Resource findBoardByLogicalCoords;

	/**
	 * Locate a board (using a full set of coordinates) based on physical
	 * cabinet-frame-board coordinates.
	 *
	 * @see Spalloc
	 */
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
	@Value("classpath:queries/find_board_by_physical_coords.sql")
	protected Resource findBoardByPhysicalCoords;

	/**
	 * Locate a board (using a full set of coordinates) based on the IP address
	 * of its ethernet chip.
	 *
	 * @see Spalloc
	 */
	@Parameter("machine_id")
	@Parameter("address")
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
	@Value("classpath:queries/find_board_by_ip_address.sql")
	protected Resource findBoardByIPAddress;

	/**
	 * Get jobs on a machine that have changes that can be processed. (A policy
	 * of how long after switching a board on or off is applied.)
	 */
	@Parameter("machine_id")
	@ResultColumn("job_id")
	@Value("classpath:queries/get_jobs_with_changes.sql")
	protected Resource getJobsWithChanges;

	/**
	 * Get the set of boards at some coordinates within a triad rectangle that
	 * are connected (i.e., have at least one path over enableable links) to the
	 * root board.
	 *
	 * @see AllocatorTask
	 */
	@Parameter("machine_id")
	@Parameter("x")
	@Parameter("y")
	@Parameter("z")
	@Parameter("width")
	@Parameter("height")
	@Parameter("depth")
	@ResultColumn("board_id")
	@Value("classpath:queries/connected_boards_at_coords.sql")
	protected Resource getConnectedBoards;

	/** Get the links of a machine that have been disabled. */
	@Parameter("machine_id")
	@ResultColumn("board_1_x")
	@ResultColumn("board_1_y")
	@ResultColumn("board_1_z")
	@ResultColumn("board_1_c")
	@ResultColumn("board_1_f")
	@ResultColumn("board_1_b")
	@ResultColumn("board_1_addr")
	@ResultColumn("dir_1")
	@ResultColumn("board_2_x")
	@ResultColumn("board_2_y")
	@ResultColumn("board_2_z")
	@ResultColumn("board_2_c")
	@ResultColumn("board_2_f")
	@ResultColumn("board_2_b")
	@ResultColumn("board_2_addr")
	@ResultColumn("dir_2")
	@Value("classpath:queries/get_dead_links.sql")
	protected Resource getDeadLinks;

	/** Get the list of allocation tasks. */
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
	@ResultColumn("max_width")
	@ResultColumn("max_height")
	@Value("classpath:queries/get_allocation_tasks.sql")
	protected Resource getAllocationTasks;
}
