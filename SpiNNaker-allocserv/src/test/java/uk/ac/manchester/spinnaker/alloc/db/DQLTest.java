/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.db;

import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_BLACKLIST_OP;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_BOARD;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_GROUP;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_JOB;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_MACHINE;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_MEMBER;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_NAME;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_USER;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;

/**
 * Tests of queries. Ensures that the SQL and the schema remain synchronized.
 *
 * @author Donal Fellows
 */
@SpringBootTest
@TestInstance(PER_CLASS)
@ActiveProfiles("unittest")
class DQLTest extends SimpleDBTestBase {
	private static final List<String> BASIC_USER_COLUMNS =
			List.of("user_id", "user_name", "openid_subject");

	/** Columns to inflate a BoardCoords. */
	private static final List<String> BOARD_COLUMNS = List.of("board_id", "x",
			"y", "z", "cabinet", "frame", "board_num", "address");

	private static final List<String> FULL_BOARD_COLUMNS = List.of("board_id",
			"x", "y", "z", "cabinet", "frame", "board_num", "address",
			"machine_name", "bmp_serial_id", "physical_serial_id");

	private static final List<String> LOCATED_BOARD = List.of("board_id",
			"bmp_id", "job_id", "machine_name", "address", "x", "y", "z",
			"cabinet", "frame", "board_num", "chip_x", "chip_y", "board_chip_x",
			"board_chip_y", "job_root_chip_x", "job_root_chip_y");

	private static final List<String> LOCATED_BOARD_2 = List.of("board_id",
			"address", "x", "y", "z", "job_id", "machine_name", "cabinet",
			"frame", "board_num", "chip_x", "chip_y", "board_chip_x",
			"board_chip_y", "job_root_chip_x", "job_root_chip_y");

	/** The columns to inflate a MachineImpl. */
	private static final List<String> MACHINE_COLUMNS = List.of("machine_id",
			"machine_name", "width", "height", "in_service");

	/** Columns to inflate a JobImpl. */
	private static final List<String> JOB_COLUMNS = List.of("job_id",
			"machine_id", "machine_name", "width", "height", "depth", "root_id",
			"job_state", "keepalive_timestamp", "keepalive_host",
			"keepalive_interval", "create_timestamp", "death_reason",
			"death_timestamp", "original_request", "owner");

	// Not currently used due to MySQL connector bug
	@SuppressWarnings("unused")
	private static final List<String> MINI_JOB_COLUMNS =
			List.of("job_id", "machine_id", "job_state", "keepalive_timestamp");

	private static final List<String> USER_COLUMNS =
			List.of("user_id", "user_name", "has_password", "trust_level",
					"locked", "disabled", "last_successful_login_timestamp",
					"last_fail_timestamp", "openid_subject", "is_internal");

	private static final List<String> GROUP_COLUMNS =
			List.of("group_id", "group_name", "quota", "group_type");

	private static final List<String> MEMBER_COLUMNS = List.of("membership_id",
			"user_id", "group_id", "user_name", "group_name");

	@Test
	void getAllMachines() {
		try (var q = c.query(GET_ALL_MACHINES)) {
			c.transaction(() -> {
				assertEquals(List.of("allow_out_of_service"),
						q.getParameters());
				assertEquals(MACHINE_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, false));
			});
		}
	}

	@Test
	void listMachineNames() {
		try (var q = c.query(LIST_MACHINE_NAMES)) {
			c.transaction(() -> {
				assertEquals(List.of("allow_out_of_service"),
						q.getParameters());
				assertEquals(List.of("machine_name", "in_service"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, false));
			});
		}
	}

	@Test
	void getMachineById() {
		try (var q = c.query(GET_MACHINE_BY_ID)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id", "allow_out_of_service"),
						q.getParameters());
				assertEquals(MACHINE_COLUMNS, q.getColumns());
				assertEquals(empty(),
						q.call1(Row::toString, NO_MACHINE, false));
			});
		}
	}

	@Test
	void getNamedMachine() {
		try (var q = c.query(GET_NAMED_MACHINE)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_name", "allow_out_of_service"),
						q.getParameters());
				assertEquals(MACHINE_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_NAME, false));
			});
		}
	}

	@Test
	void getMachineWraps() {
		try (var q = c.query(GET_MACHINE_WRAPS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(List.of("horizontal_wrap", "vertical_wrap"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getMachineJobs() {
		try (var q = c.query(GET_MACHINE_JOBS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(List.of("job_id", "owner_name"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getMachineReports() {
		try (var q = c.query(GET_MACHINE_REPORTS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(
						List.of("board_id", "report_id", "reported_issue",
								"report_timestamp", "reporter_name"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getJobIds() {
		try (var q = c.query(GET_JOB_IDS)) {
			c.transaction(() -> {
				assertEquals(List.of("limit", "offset"), q.getParameters());
				// Disabled: https://bugs.mysql.com/bug.php?id=103437
				// assertEquals(MINI_JOB_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, 0, 0));
			});
		}
	}

	@Test
	void getLiveJobIds() {
		try (var q = c.query(GET_LIVE_JOB_IDS)) {
			c.transaction(() -> {
				assertEquals(List.of("limit", "offset"), q.getParameters());
				// Disabled: https://bugs.mysql.com/bug.php?id=103437
				// assertEquals(MINI_JOB_COLUMNS, q.getColumns());
				q.call(Row.integer("job_id"), 0, 0);
				// Must not throw
			});
		}
	}

	@Test
	void getJob() {
		try (var q = c.query(GET_JOB)) {
			c.transaction(() -> {
				assertEquals(List.of("job_id"), q.getParameters());
				assertEquals(JOB_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_JOB));
			});
		}
	}

	@Test
	void getJobBoards() {
		try (var q = c.query(GET_JOB_BOARDS)) {
			c.transaction(() -> {
				assertEquals(List.of("job_id"), q.getParameters());
				assertEquals(List.of("board_id"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_JOB));
			});
		}
	}

	@Test
	void getJobBoardCoords() {
		try (var q = c.query(GET_JOB_BOARD_COORDS)) {
			c.transaction(() -> {
				assertEquals(List.of("job_id"), q.getParameters());
				assertEquals(BOARD_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_JOB));
			});
		}
	}

	@Test
	void getJobChipDimensions() {
		try (var q = c.query(GET_JOB_CHIP_DIMENSIONS)) {
			c.transaction(() -> {
				assertEquals(List.of("job_id"), q.getParameters());
				assertEquals(List.of("width", "height"), q.getColumns());
				var dims = q.call1(r -> new MachineDimensions(r.getInt("width"),
						r.getInt("height")), NO_JOB).orElseThrow();
				// These two are actually NULL when there's no job
				assertEquals(0, dims.width);
				assertEquals(0, dims.height);
			});
		}
	}

	@Test
	void getRootOfBoard() {
		try (var q = c.query(GET_ROOT_OF_BOARD)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id"), q.getParameters());
				assertEquals(List.of("root_x", "root_y"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD));
			});
		}
	}

	@Test
	void getRootBMPAddress() {
		try (var q = c.query(GET_ROOT_BMP_ADDRESS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(List.of("address"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getBMPAddress() {
		try (var q = c.query(GET_BMP_ADDRESS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id", "cabinet", "frame"),
						q.getParameters());
				assertEquals(List.of("address"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE, 0, 0));
			});
		}
	}

	@Test
	void getBoardAddress() {
		try (var q = c.query(GET_BOARD_ADDRESS)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id"), q.getParameters());
				assertEquals(List.of("address"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD));
			});
		}
	}

	@Test
	void getBoardPowerInfo() {
		try (var q = c.query(GET_BOARD_POWER_INFO)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id"), q.getParameters());
				assertEquals(List.of("board_power", "power_off_timestamp",
						"power_on_timestamp"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD));
			});
		}
	}

	@Test
	void getBoardJob() {
		try (var q = c.query(GET_BOARD_JOB)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id"), q.getParameters());
				assertEquals(List.of("allocated_job"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD));
			});
		}
	}

	@Test
	void getBoardReports() {
		try (var q = c.query(GET_BOARD_REPORTS)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id"), q.getParameters());
				assertEquals(
						List.of("board_id", "report_id", "reported_issue",
								"report_timestamp", "reporter_name"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD));
			});
		}
	}

	@Test
	void getBoardNumbers() {
		try (var q = c.query(GET_BOARD_NUMBERS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(List.of("board_num"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getBmpBoardNumbers() {
		try (var q = c.query(GET_BMP_BOARD_NUMBERS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id", "cabinet", "frame"),
						q.getParameters());
				assertEquals(List.of("board_num"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE, 0, 0));
			});
		}
	}

	@Test
	void getLiveBoards() {
		try (var q = c.query(GET_LIVE_BOARDS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(BOARD_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getDeadBoards() {
		try (var q = c.query(GET_DEAD_BOARDS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(BOARD_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getAllBoards() {
		try (var q = c.query(GET_ALL_BOARDS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(BOARD_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getAllBoardsOfAllMachines() {
		try (var q = c.query(GET_ALL_BOARDS_OF_ALL_MACHINES)) {
			c.transaction(() -> {
				assertEquals(List.of(), q.getParameters());
				assertEquals(BOARD_COLUMNS, q.getColumns());
				// As long as this doesn't throw, the test passes
				return q.call1(Row::toString).isPresent();
			});
		}
	}

	@Test
	void getDeadLinks() {
		try (var q = c.query(getDeadLinks)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(List.of("board_1_x", "board_1_y", "board_1_z",
						"board_1_c", "board_1_f", "board_1_b", "board_1_addr",
						"dir_1", "board_2_x", "board_2_y", "board_2_z",
						"board_2_c", "board_2_f", "board_2_b", "board_2_addr",
						"dir_2"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getAvailableBoardNumbers() {
		try (var q = c.query(GET_AVAILABLE_BOARD_NUMBERS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(List.of("board_num"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getTags() {
		try (var q = c.query(GET_TAGS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(List.of("tag"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getSumBoardsPowered() {
		// This query always produces one row
		try (var q = c.query(GET_SUM_BOARDS_POWERED)) {
			c.transaction(() -> {
				assertEquals(List.of("job_id"), q.getParameters());
				assertEquals(List.of("total_on"), q.getColumns());
				assertEquals(0,
						q.call1(integer("total_on"), NO_JOB).orElseThrow());
			});
		}
	}

	@Test
	void getBoardConnectInfo() {
		try (var q = c.query(GET_BOARD_CONNECT_INFO)) {
			c.transaction(() -> {
				assertEquals(List.of("job_id"), q.getParameters());
				assertEquals(List.of("board_id", "address", "x", "y", "z",
						"root_x", "root_y"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_JOB));
			});
		}
	}

	@Test
	void getRootCoords() {
		try (var q = c.query(GET_ROOT_COORDS)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id"), q.getParameters());
				assertEquals(List.of("x", "y", "z", "root_x", "root_y"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD));
			});
		}
	}

	@Test
	void getAllocationTasks() {
		try (var q = c.query(getAllocationTasks)) {
			c.transaction(() -> {
				assertEquals(List.of("job_state"), q.getParameters());
				assertEquals(List.of("req_id", "job_id", "num_boards", "width",
						"height", "board_id", "machine_id", "max_dead_boards",
						"max_width", "max_height", "job_state", "importance"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, QUEUED));
			});
		}
	}

	@Test
	void findFreeBoard() {
		try (var q = c.query(FIND_FREE_BOARD)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(List.of("x", "y", "z"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getBoardByCoords() {
		try (var q = c.query(GET_BOARD_BY_COORDS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id", "x", "y", "z"),
						q.getParameters());
				assertEquals(List.of("board_id"), q.getColumns());
				assertEquals(empty(),
						q.call1(Row::toString, NO_MACHINE, -1, -1, -1));
			});
		}
	}

	@Test
	void findExpiredJobs() {
		try (var q = c.query(FIND_EXPIRED_JOBS)) {
			c.transaction(() -> {
				assertEquals(List.of(), q.getParameters());
				assertEquals(List.of("job_id"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString));
			});
		}
	}

	@Test
	void loadDirInfo() {
		try (var q = c.query(LOAD_DIR_INFO)) {
			c.transaction(() -> {
				assertEquals(List.of(), q.getParameters());
				assertEquals(List.of("z", "direction", "dx", "dy", "dz"),
						q.getColumns());
				q.call(Row::toString);
			});
		}
	}

	@Test
	void getChanges() {
		try (var q = c.query(GET_CHANGES)) {
			c.transaction(() -> {
				assertEquals(List.of("job_id"), q.getParameters());
				assertEquals(List.of("change_id", "job_id", "board_id", "power",
						"fpga_n", "fpga_s", "fpga_e", "fpga_w", "fpga_se",
						"fpga_nw", "in_progress", "from_state", "to_state",
						"board_num", "bmp_id", "cabinet", "frame"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_JOB));
			});
		}
	}

	@Test
	void findRectangle() {
		try (var q = c.query(findRectangle)) {
			c.transaction(() -> {
				assertEquals(List.of("width", "height", "machine_id",
						"max_dead_boards"), q.getParameters());
				assertEquals(List.of("id", "x", "y", "z", "available"),
						q.getColumns());
				assertEquals(empty(),
						q.call1(Row::toString, -1, -1, NO_MACHINE, 0));
			});
		}
	}

	@Test
	void findRectangleAt() {
		try (var q = c.query(findRectangleAt)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id", "width", "height",
						"machine_id", "max_dead_boards"), q.getParameters());
				assertEquals(List.of("id", "x", "y", "z", "available"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD, -1, -1,
						NO_MACHINE, 0));
			});
		}
	}

	@Test
	void findLocation() {
		try (var q = c.query(findLocation)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id", "board_id"),
						q.getParameters());
				assertEquals(List.of("x", "y", "z"), q.getColumns());
				assertEquals(empty(),
						q.call1(Row::toString, NO_MACHINE, NO_BOARD));
			});
		}
	}

	@Test
	void countConnected() {
		try (var q = c.query(countConnected)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id", "x", "y", "width", "height"),
						q.getParameters());
				assertEquals(List.of("connected_size"), q.getColumns());
				assertEquals(0, q.call1(integer("connected_size"), NO_MACHINE,
						-1, -1, -1, -1).orElseThrow());
			});
		}
	}

	@Test
	void countPendingChanges() {
		try (var q = c.query(COUNT_PENDING_CHANGES)) {
			c.transaction(() -> {
				assertEquals(List.of(), q.getParameters());
				assertEquals(List.of("c"), q.getColumns());
				assertEquals(0, q.call1(integer("c")).orElseThrow());
			});
		}
	}

	@Test
	void getPerimeterLinks() {
		try (var q = c.query(getPerimeterLinks)) {
			c.transaction(() -> {
				assertEquals(List.of("job_id"), q.getParameters());
				assertEquals(List.of("board_id", "direction"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_JOB));
			});
		}
	}

	@Test
	void findBoardByGlobalChip() {
		try (var q = c.query(findBoardByGlobalChip)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id", "x", "y"),
						q.getParameters());
				assertEquals(LOCATED_BOARD, q.getColumns());
				assertEquals(empty(),
						q.call1(Row::toString, NO_MACHINE, -1, -1));
			});
		}
	}

	@Test
	void findBoardByJobChip() {
		try (var q = c.query(findBoardByJobChip)) {
			c.transaction(() -> {
				assertEquals(List.of("job_id", "board_id", "x", "y"),
						q.getParameters());
				assertEquals(LOCATED_BOARD_2, q.getColumns());
				assertEquals(empty(),
						q.call1(Row::toString, NO_JOB, NO_BOARD, -1, -1));
			});
		}
	}

	@Test
	void findBoardByLogicalCoords() {
		try (var q = c.query(findBoardByLogicalCoords)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id", "x", "y", "z"),
						q.getParameters());
				assertEquals(LOCATED_BOARD, q.getColumns());
				assertEquals(empty(),
						q.call1(Row::toString, NO_MACHINE, -1, -1, -1));
			});
		}
	}

	@Test
	void findBoardByPhysicalCoords() {
		try (var q = c.query(findBoardByPhysicalCoords)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id", "cabinet", "frame", "board"),
						q.getParameters());
				assertEquals(LOCATED_BOARD, q.getColumns());
				assertEquals(empty(),
						q.call1(Row::toString, NO_MACHINE, -1, -1, -1));
			});
		}
	}

	@Test
	void findBoardByIPAddress() {
		try (var q = c.query(findBoardByIPAddress)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id", "address"),
						q.getParameters());
				assertEquals(LOCATED_BOARD, q.getColumns());
				assertEquals(empty(),
						q.call1(Row::toString, NO_MACHINE, "127.0.0.1"));
			});
		}
	}

	@Test
	void getJobsWithChanges() {
		try (var q = c.query(getJobsWithChanges)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(List.of("job_id"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getConnectedBoards() {
		try (var q = c.query(getConnectedBoards)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id", "x", "y", "z", "width",
						"height", "depth"), q.getParameters());
				assertEquals(List.of("board_id"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE, -1, -1,
						-1, -1, -1, -1));
			});
		}
	}

	@Test
	void findBoardByNameAndXYZ() {
		try (var q = c.query(FIND_BOARD_BY_NAME_AND_XYZ)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_name", "x", "y", "z"),
						q.getParameters());
				assertEquals(FULL_BOARD_COLUMNS, q.getColumns());
				assertEquals(empty(),
						q.call1(Row::toString, NO_NAME, -1, -1, -1));
			});
		}
	}

	@Test
	void findBoardById() {
		try (var q = c.query(FIND_BOARD_BY_ID)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id"), q.getParameters());
				assertEquals(FULL_BOARD_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD));
			});
		}
	}

	@Test
	void findBoardByNameAndCFB() {
		try (var q = c.query(FIND_BOARD_BY_NAME_AND_CFB)) {
			c.transaction(() -> {
				assertEquals(
						List.of("machine_name", "cabinet", "frame", "board"),
						q.getParameters());
				assertEquals(FULL_BOARD_COLUMNS, q.getColumns());
				assertEquals(empty(),
						q.call1(Row::toString, NO_NAME, -1, -1, -1));
			});
		}
	}

	@Test
	void findBoardByNameAndIPAddress() {
		try (var q = c.query(FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_name", "address"),
						q.getParameters());
				assertEquals(FULL_BOARD_COLUMNS, q.getColumns());
				assertEquals(empty(),
						q.call1(Row::toString, NO_NAME, "256.256.256.256"));
			});
		}
	}

	@Test
	void getFunctioningField() {
		try (var q = c.query(GET_FUNCTIONING_FIELD)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id"), q.getParameters());
				assertEquals(List.of("functioning"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD));
			});
		}
	}

	@Test
	void getGroupQuota() {
		try (var q = c.query(GET_GROUP_QUOTA)) {
			c.transaction(() -> {
				assertEquals(List.of("group_id"), q.getParameters());
				assertEquals(List.of("quota"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_GROUP));
			});
		}
	}

	@Test
	void listAllGroups() {
		try (var q = c.query(LIST_ALL_GROUPS)) {
			c.transaction(() -> {
				assertEquals(List.of(), q.getParameters());
				assertEquals(GROUP_COLUMNS, q.getColumns());
				// Not sure what default state is, but this should not error
				assertNotNull(q.call(Row::toString));
			});
		}
	}

	@Test
	void listAllGroupsOfType() {
		try (var q = c.query(LIST_ALL_GROUPS_OF_TYPE)) {
			c.transaction(() -> {
				assertEquals(List.of("type"), q.getParameters());
				assertEquals(GROUP_COLUMNS, q.getColumns());
				// Not sure what default state is, but this should not error
				assertNotNull(q.call(Row::toString, GroupType.INTERNAL));
			});
		}
	}

	@Test
	void getGroupById() {
		try (var q = c.query(GET_GROUP_BY_ID)) {
			c.transaction(() -> {
				assertEquals(List.of("group_id"), q.getParameters());
				assertEquals(GROUP_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_GROUP));
			});
		}
	}

	@Test
	void getGroupByName() {
		try (var q = c.query(GET_GROUP_BY_NAME)) {
			c.transaction(() -> {
				assertEquals(List.of("group_name"), q.getParameters());
				assertEquals(GROUP_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_NAME));
			});
		}
	}

	@Test
	void getUsersOfGroup() {
		try (var q = c.query(GET_USERS_OF_GROUP)) {
			c.transaction(() -> {
				assertEquals(List.of("group_id"), q.getParameters());
				assertEquals(List.of("membership_id", "group_id", "group_name",
						"user_id", "user_name"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_GROUP));
			});
		}
	}

	@Test
	void getMembership() {
		try (var q = c.query(GET_MEMBERSHIP)) {
			c.transaction(() -> {
				assertEquals(List.of("membership_id"), q.getParameters());
				assertEquals(MEMBER_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MEMBER));
			});
		}
	}

	@Test
	void getMembershipsOfUser() {
		try (var q = c.query(GET_MEMBERSHIPS_OF_USER)) {
			c.transaction(() -> {
				assertEquals(List.of("user_id"), q.getParameters());
				assertEquals(MEMBER_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_USER));
			});
		}
	}

	@Test
	void getUserQuota() {
		try (var q = c.query(GET_USER_QUOTA)) {
			c.transaction(() -> {
				assertEquals(List.of("user_name"), q.getParameters());
				assertEquals(List.of("quota_total", "user_id"), q.getColumns());
				// Still get a quota, it is just 0
				assertNotEquals(empty(), q.call1(Row::toString, NO_NAME));
			});
		}
	}

	@Test
	void getCurrentUsage() {
		try (var q = c.query(GET_CURRENT_USAGE)) {
			c.transaction(() -> {
				assertEquals(List.of("group_id"), q.getParameters());
				assertEquals(List.of("current_usage"), q.getColumns());
				assertEquals(0, q.call1(integer("current_usage"), NO_GROUP)
						.orElseThrow());
			});
		}
	}

	@Test
	void getJobUsageAndQuota() {
		try (var q = c.query(GET_JOB_USAGE_AND_QUOTA)) {
			c.transaction(() -> {
				assertEquals(List.of("job_id"), q.getParameters());
				assertEquals(List.of("quota_used", "quota"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_JOB));
			});
		}
	}

	@Test
	void getConsolidationTargets() {
		try (var q = c.query(GET_CONSOLIDATION_TARGETS)) {
			c.transaction(() -> {
				assertEquals(List.of(), q.getParameters());
				assertEquals(List.of("job_id", "group_id", "quota_used"),
						q.getColumns());
				// Empty DB has no consolidation targets
				assertEquals(empty(), q.call1(Row::toString));
			});
		}
	}

	@Test
	void isUserLocked() {
		try (var q = c.query(IS_USER_LOCKED)) {
			c.transaction(() -> {
				assertEquals(List.of("username"), q.getParameters());
				assertEquals(List.of("user_id", "locked", "disabled"),
						q.getColumns());
				// Testing DB has no users by default
				assertEquals(empty(), q.call1(Row::toString, ""));
			});
		}
	}

	@Test
	void getUserAuthorities() {
		try (var q = c.query(GET_USER_AUTHORITIES)) {
			c.transaction(() -> {
				assertEquals(List.of("user_id"), q.getParameters());
				assertEquals(List.of("trust_level", "encrypted_password",
						"openid_subject"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_USER));
			});
		}
	}

	@Test
	void listAllUsers() {
		try (var q = c.query(LIST_ALL_USERS)) {
			c.transaction(() -> {
				assertEquals(List.of(), q.getParameters());
				assertEquals(BASIC_USER_COLUMNS, q.getColumns());
				// Testing DB has no users by default
				assertEquals(empty(), q.call1(Row::toString));
			});
		}
	}

	@Test
	void listAllUsersOfType() {
		try (var q = c.query(LIST_ALL_USERS_OF_TYPE)) {
			c.transaction(() -> {
				assertEquals(List.of("internal"), q.getParameters());
				assertEquals(BASIC_USER_COLUMNS, q.getColumns());
				// Testing DB has no users by default
				assertEquals(empty(), q.call1(Row::toString, false));
			});
		}
	}

	@Test
	void getUserId() {
		try (var q = c.query(GET_USER_ID)) {
			c.transaction(() -> {
				assertEquals(List.of("user_name"), q.getParameters());
				assertEquals(List.of("user_id"), q.getColumns());
				// Testing DB has no users by default
				assertEquals(empty(), q.call1(Row::toString, NO_NAME));
			});
		}
	}

	@Test
	void getUserDetails() {
		try (var q = c.query(GET_USER_DETAILS)) {
			c.transaction(() -> {
				assertEquals(List.of("user_id"), q.getParameters());
				assertEquals(USER_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_USER));
			});
		}
	}

	@Test
	void getUserDetailsByName() {
		try (var q = c.query(GET_USER_DETAILS_BY_NAME)) {
			c.transaction(() -> {
				assertEquals(List.of("user_name"), q.getParameters());
				assertEquals(USER_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_NAME));
			});
		}
	}

	@Test
	void getUserDetailsBySubject() {
		try (var q = c.query(GET_USER_DETAILS_BY_SUBJECT)) {
			c.transaction(() -> {
				assertEquals(List.of("openid_subject"), q.getParameters());
				assertEquals(USER_COLUMNS, q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_NAME));
			});
		}
	}

	@Test
	void getGroupByNameAndMember() {
		try (var q = c.query(GET_GROUP_BY_NAME_AND_MEMBER)) {
			c.transaction(() -> {
				assertEquals(List.of("user_name", "group_name"),
						q.getParameters());
				assertEquals(List.of("group_id"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_NAME, NO_NAME));
			});
		}
	}

	@Test
	void getGroupsAndQuotasOfUser() {
		try (var q = c.query(GET_GROUPS_AND_QUOTAS_OF_USER)) {
			c.transaction(() -> {
				assertEquals(List.of("user_name"), q.getParameters());
				assertEquals(List.of("group_id", "quota"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_NAME));
			});
		}
	}

	@Test
	void countMachineThings() {
		try (var q = c.query(COUNT_MACHINE_THINGS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(List.of("board_count", "in_use", "num_jobs"),
						q.getColumns());
				var r = q.call1((row) -> Map.of(
						"board_count", row.getInt("board_count"),
						"in_use", row.getInt("in_use"),
						"num_jobs", row.getInt("num_jobs")), NO_MACHINE)
						.orElseThrow();
				assertEquals(0, r.get("board_count"));
				assertEquals(0, r.get("in_use"));
				assertEquals(0, r.get("num_jobs"));
			});
		}
	}

	@Test
	void countPoweredBoards() {
		try (var q = c.query(COUNT_POWERED_BOARDS)) {
			c.transaction(() -> {
				assertEquals(List.of("job_id"), q.getParameters());
				assertEquals(List.of("c"), q.getColumns());
				assertEquals(0, q.call1(integer("c"), NO_JOB).orElseThrow());
			});
		}
	}

	@Test
	void listLiveJobs() {
		try (var q = c.query(LIST_LIVE_JOBS)) {
			c.transaction(() -> {
				assertEquals(List.of(), q.getParameters());
				assertEquals(List.of("job_id", "machine_id", "create_timestamp",
						"keepalive_interval", "job_state", "allocation_size",
						"keepalive_host", "user_name", "machine_name"),
						q.getColumns());
				// No jobs right now
				assertEquals(empty(), q.call1(Row::toString));
			});
		}
	}

	@Test
	void getLocalUserDetails() {
		try (var q = c.query(GET_LOCAL_USER_DETAILS)) {
			c.transaction(() -> {
				assertEquals(List.of("user_name"), q.getParameters());
				assertEquals(
						List.of("user_id", "user_name", "encrypted_password"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_USER));
			});
		}
	}

	@Test
	void getReportedBoards() {
		try (var q = c.query(getReportedBoards)) {
			c.transaction(() -> {
				assertEquals(List.of("threshold"), q.getParameters());
				assertEquals(List.of("board_id", "num_reports", "x", "y", "z",
						"address"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, 0));
			});
		}
	}

	@Test
	void isBoardBlacklistCurrent() {
		try (var q = c.query(IS_BOARD_BLACKLIST_CURRENT)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id"), q.getParameters());
				assertEquals(List.of("current"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD));
			});
		}
	}

	@Test
	void getBlacklistedChips() {
		try (var q = c.query(GET_BLACKLISTED_CHIPS)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id"), q.getParameters());
				assertEquals(List.of("x", "y", "notes"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD));
			});
		}
	}

	@Test
	void getBlacklistedCores() {
		try (var q = c.query(GET_BLACKLISTED_CORES)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id"), q.getParameters());
				assertEquals(List.of("x", "y", "p", "notes"), q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD));
			});
		}
	}

	@Test
	void getBlacklistedLinks() {
		try (var q = c.query(GET_BLACKLISTED_LINKS)) {
			c.transaction(() -> {
				assertEquals(List.of("board_id"), q.getParameters());
				assertEquals(List.of("x", "y", "direction", "notes"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BOARD));
			});
		}
	}

	@Test
	void getBlacklistReads() {
		try (var q = c.query(GET_BLACKLIST_READS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(
						List.of("op_id", "board_id", "bmp_serial_id",
								"board_num", "cabinet", "frame"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getBlacklistWrites() {
		try (var q = c.query(GET_BLACKLIST_WRITES)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(
						List.of("op_id", "board_id", "bmp_serial_id",
								"board_num", "cabinet", "frame", "data"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getSerialInfoReqs() {
		try (var q = c.query(GET_SERIAL_INFO_REQS)) {
			c.transaction(() -> {
				assertEquals(List.of("machine_id"), q.getParameters());
				assertEquals(
						List.of("op_id", "board_id", "bmp_serial_id",
								"board_num", "cabinet", "frame"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_MACHINE));
			});
		}
	}

	@Test
	void getTempInfoReqs() {
		try (var q = c.query(GET_TEMP_INFO_REQS)) {
			//assertEquals(1, q.getNumArguments());
			//assertEquals(Set.of("board_id", "board_num", "cabinet", "frame",
			//		"op_id", "bmp_serial_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertEquals(q.call1((row) -> 1, NO_MACHINE), empty());
			});
		}
	}

	@Test
	void getCompletedBlacklistOp() {
		try (var q = c.query(GET_COMPLETED_BLACKLIST_OP)) {
			c.transaction(() -> {
				assertEquals(List.of("op_id"), q.getParameters());
				assertEquals(
						List.of("board_id", "op", "data", "failure", "failed"),
						q.getColumns());
				assertEquals(empty(), q.call1(Row::toString, NO_BLACKLIST_OP));
			});
		}
	}
}
