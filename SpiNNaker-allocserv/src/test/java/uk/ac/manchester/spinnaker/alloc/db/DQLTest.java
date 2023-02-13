/*
 * Copyright (c) 2021-2023 The University of Manchester
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

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.BASIC_MACHINE_INFO;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.BOARD_COORDS_REQUIRED_COLUMNS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.GROUP_COLUMNS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.MEMBER_COLUMNS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.MSC_BOARD_COORDS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_BLACKLIST_OP;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_BOARD;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_GROUP;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_JOB;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_MACHINE;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_MEMBER;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_NAME;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_USER;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.USER_COLUMNS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assertCanMakeBoardLocation;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType;

/**
 * Tests of queries. Ensures that the SQL and the schema remain synchronized.
 *
 * @author Donal Fellows
 */
@SpringBootTest
@TestInstance(PER_CLASS)
@ActiveProfiles("unittest")
class DQLTest extends MemDBTestBase {
	@Test
	void getAllMachines() {
		try (var q = c.query(GET_ALL_MACHINES)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(BASIC_MACHINE_INFO, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(false).isPresent());
			});
		}
	}

	@Test
	void listMachineNames() {
		try (var q = c.query(LIST_MACHINE_NAMES)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("machine_name", "in_service"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(false).isPresent());
			});
		}
	}

	@Test
	void getMachineById() {
		try (var q = c.query(GET_MACHINE_BY_ID)) {
			assertEquals(2, q.getNumArguments());
			assertEquals(BASIC_MACHINE_INFO, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, false).isPresent());
			});
		}
	}

	@Test
	void getNamedMachine() {
		try (var q = c.query(GET_NAMED_MACHINE)) {
			assertEquals(2, q.getNumArguments());
			assertEquals(BASIC_MACHINE_INFO, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME, false).isPresent());
			});
		}
	}

	@Test
	void getMachineJobs() {
		try (var q = c.query(GET_MACHINE_JOBS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("job_id", "owner_name"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getMachineReports() {
		try (var q = c.query(GET_MACHINE_REPORTS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(
					Set.of("board_id", "report_id", "report_timestamp",
							"reported_issue", "reporter_name"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getJobIds() {
		try (var q = c.query(GET_JOB_IDS)) {
			assertEquals(2, q.getNumArguments());
			assertEquals(Set.of("job_id", "machine_id", "job_state",
					"keepalive_timestamp"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(0, 0).isPresent());
			});
		}
	}

	@Test
	void getLiveJobIds() {
		try (var q = c.query(GET_LIVE_JOB_IDS)) {
			assertEquals(2, q.getNumArguments());
			assertEquals(Set.of("job_id", "machine_id", "job_state",
					"keepalive_timestamp"), q.getRowColumnNames());
			c.transaction(() -> {
				q.call(0, 0).map(Row.integer("job_id")).toList();
				// Must not throw
			});
		}
	}

	@Test
	void getJob() {
		try (var q = c.query(GET_JOB)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("job_id", "machine_id", "machine_name", "width",
					"height", "depth", "root_id", "job_state",
					"keepalive_timestamp", "keepalive_host",
					"keepalive_interval", "create_timestamp", "death_reason",
					"death_timestamp", "original_request", "owner"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getJobBoards() {
		try (var q = c.query(GET_JOB_BOARDS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("board_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getJobBoardCoords() {
		try (var q = c.query(GET_JOB_BOARD_COORDS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(BOARD_COORDS_REQUIRED_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getJobChipDimensions() {
		try (var q = c.query(GET_JOB_CHIP_DIMENSIONS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("width", "height"), q.getRowColumnNames());
			c.transaction(() -> {
				var row = q.call1(NO_JOB).orElseThrow();
				// These two are actually NULL when there's no job
				assertEquals(0, row.getInt("width"));
				assertEquals(0, row.getInt("height"));
			});
		}
	}

	@Test
	void getRootOfBoard() {
		try (var q = c.query(GET_ROOT_OF_BOARD)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("root_x", "root_y"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getRootBMPAddress() {
		try (var q = c.query(GET_ROOT_BMP_ADDRESS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("address"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getBMPAddress() {
		try (var q = c.query(GET_BMP_ADDRESS)) {
			assertEquals(3, q.getNumArguments());
			assertEquals(Set.of("address"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, 0, 0).isPresent());
			});
		}
	}

	@Test
	void getBoardAddress() {
		try (var q = c.query(GET_BOARD_ADDRESS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("address"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBoardPowerInfo() {
		try (var q = c.query(GET_BOARD_POWER_INFO)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("board_power", "power_off_timestamp",
					"power_on_timestamp"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBoardJob() {
		try (var q = c.query(GET_BOARD_JOB)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("allocated_job"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBoardReports() {
		try (var q = c.query(GET_BOARD_REPORTS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(
					Set.of("board_id", "report_id", "report_timestamp",
							"reported_issue", "reporter_name"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBoardNumbers() {
		try (var q = c.query(GET_BOARD_NUMBERS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("board_num"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getBmpBoardNumbers() {
		try (var q = c.query(GET_BMP_BOARD_NUMBERS)) {
			assertEquals(3, q.getNumArguments());
			assertEquals(Set.of("board_num"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, 0, 0).isPresent());
			});
		}
	}

	@Test
	void getLiveBoards() {
		try (var q = c.query(GET_LIVE_BOARDS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(BOARD_COORDS_REQUIRED_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getDeadBoards() {
		try (var q = c.query(GET_DEAD_BOARDS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(BOARD_COORDS_REQUIRED_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getAllBoards() {
		try (var q = c.query(GET_ALL_BOARDS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(BOARD_COORDS_REQUIRED_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getAllBoardsOfAllMachines() {
		try (var q = c.query(GET_ALL_BOARDS_OF_ALL_MACHINES)) {
			assertEquals(0, q.getNumArguments());
			assertEquals(BOARD_COORDS_REQUIRED_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				// As long as this doesn't throw, the test passes
				return q.call1().isPresent();
			});
		}
	}

	@Test
	void getDeadLinks() {
		try (var q = c.query(getDeadLinks)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(
					Set.of("board_1_x", "board_1_y", "board_1_z", "board_1_c",
							"board_1_f", "board_1_b", "board_1_addr", "dir_1",
							"board_2_x", "board_2_y", "board_2_z", "board_2_c",
							"board_2_f", "board_2_b", "board_2_addr", "dir_2"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getAvailableBoardNumbers() {
		try (var q = c.query(GET_AVAILABLE_BOARD_NUMBERS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("board_num"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getTags() {
		try (var q = c.query(GET_TAGS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("tag"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getSumBoardsPowered() {
		// This query always produces one row
		try (var q = c.query(GET_SUM_BOARDS_POWERED)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("total_on"), q.getRowColumnNames());
			c.transaction(() -> {
				var row = q.call1(NO_JOB).orElseThrow();
				assertEquals(0, row.getInt("total_on"));
			});
		}
	}

	@Test
	void getBoardConnectInfo() {
		try (var q = c.query(GET_BOARD_CONNECT_INFO)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("board_id", "address", "x", "y", "z", "root_x",
					"root_y"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getRootCoords() {
		try (var q = c.query(GET_ROOT_COORDS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("x", "y", "z", "root_x", "root_y"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getAllocationTasks() {
		try (var q = c.query(getAllocationTasks)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("req_id", "job_id", "num_boards", "width",
					"height", "board_id", "machine_id", "max_dead_boards",
					"max_height", "max_width", "job_state", "importance"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(QUEUED).isPresent());
			});
		}
	}

	@Test
	void findFreeBoard() {
		try (var q = c.query(FIND_FREE_BOARD)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("x", "y", "z"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getBoardByCoords() {
		try (var q = c.query(GET_BOARD_BY_COORDS)) {
			assertEquals(4, q.getNumArguments());
			assertEquals(Set.of("board_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, -1, -1, -1).isPresent());
			});
		}
	}

	@Test
	void findExpiredJobs() {
		try (var q = c.query(FIND_EXPIRED_JOBS)) {
			assertEquals(0, q.getNumArguments());
			assertEquals(Set.of("job_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1().isPresent());
			});
		}
	}

	@Test
	void loadDirInfo() {
		try (var q = c.query(LOAD_DIR_INFO)) {
			assertEquals(0, q.getNumArguments());
			assertEquals(Set.of("z", "direction", "dx", "dy", "dz"),
					q.getRowColumnNames());
			c.transaction(() -> {
				q.call();
			});
		}
	}

	@Test
	void getChanges() {
		try (var q = c.query(GET_CHANGES)) {
			assertEquals(1, q.getNumArguments());
			var colNames = q.getRowColumnNames();
			assertEquals(
					Set.of("change_id", "job_id", "board_id", "power", "fpga_n",
							"fpga_s", "fpga_e", "fpga_w", "fpga_nw", "fpga_se",
							"in_progress", "from_state", "to_state",
							"board_num", "cabinet", "frame", "bmp_id"),
					colNames);
			// Ensure that this link is maintained
			for (var d : Direction.values()) {
				assertTrue(colNames.contains(d.columnName),
						() -> format("%s must contain %s", colNames,
								d.columnName));
			}
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB).isPresent());
			});
		}
	}

	@Test
	void findRectangle() {
		try (var q = c.query(findRectangle)) {
			assertEquals(4, q.getNumArguments());
			assertEquals(Set.of("id", "x", "y", "z", "available"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(-1, -1, NO_MACHINE, 0).isPresent());
			});
		}
	}

	@Test
	void findRectangleAt() {
		try (var q = c.query(findRectangleAt)) {
			assertEquals(5, q.getNumArguments());
			assertEquals(Set.of("id", "x", "y", "z", "available"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(
						q.call1(NO_BOARD, -1, -1, NO_MACHINE, 0).isPresent());
			});
		}
	}

	@Test
	void findLocation() {
		try (var q = c.query(findLocation)) {
			assertEquals(2, q.getNumArguments());
			assertEquals(Set.of("x", "y", "z"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void countConnected() {
		try (var q = c.query(countConnected)) {
			assertEquals(5, q.getNumArguments());
			assertEquals(Set.of("connected_size"), q.getRowColumnNames());
			c.transaction(() -> {
				var row = q.call1(NO_MACHINE, -1, -1, -1, -1).orElseThrow();
				assertEquals(0, row.getInt("connected_size"));
			});
		}
	}

	@Test
	void countPendingChanges() {
		try (var q = c.query(COUNT_PENDING_CHANGES)) {
			assertEquals(0, q.getNumArguments());
			assertEquals(Set.of("c"), q.getRowColumnNames());
			c.transaction(() -> {
				var row = q.call1().orElseThrow();
				assertEquals(0, row.getInt("c"));
			});
		}
	}

	@Test
	void getPerimeterLinks() {
		try (var q = c.query(getPerimeterLinks)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("board_id", "direction"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB).isPresent());
			});
		}
	}

	@Test
	void findBoardByGlobalChip() {
		try (var q = c.query(findBoardByGlobalChip)) {
			assertEquals(3, q.getNumArguments());
			assertEquals(Set.of("board_id", "address", "bmp_id", "x", "y", "z",
					"job_id", "machine_name", "cabinet", "frame", "board_num",
					"chip_x", "chip_y", "board_chip_x", "board_chip_y",
					"job_root_chip_x", "job_root_chip_y"),
					q.getRowColumnNames());
			assertCanMakeBoardLocation(q);
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, -1, -1).isPresent());
			});
		}
	}

	@Test
	void findBoardByJobChip() {
		try (var q = c.query(findBoardByJobChip)) {
			assertEquals(4, q.getNumArguments());
			assertEquals(
					Set.of("board_id", "address", "x", "y", "z", "job_id",
							"machine_name", "cabinet", "frame", "board_num",
							"chip_x", "chip_y", "board_chip_x", "board_chip_y",
							"job_root_chip_x", "job_root_chip_y"),
					q.getRowColumnNames());
			assertCanMakeBoardLocation(q);
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB, NO_BOARD, -1, -1).isPresent());
			});
		}
	}

	@Test
	void findBoardByLogicalCoords() {
		try (var q = c.query(findBoardByLogicalCoords)) {
			assertEquals(4, q.getNumArguments());
			assertEquals(Set.of("board_id", "address", "bmp_id", "x", "y", "z",
					"job_id", "machine_name", "cabinet", "frame", "board_num",
					"chip_x", "chip_y", "board_chip_x", "board_chip_y",
					"job_root_chip_x", "job_root_chip_y"),
					q.getRowColumnNames());
			assertCanMakeBoardLocation(q);
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, -1, -1, -1).isPresent());
			});
		}
	}

	@Test
	void findBoardByPhysicalCoords() {
		try (var q = c.query(findBoardByPhysicalCoords)) {
			assertEquals(4, q.getNumArguments());
			assertEquals(Set.of("board_id", "address", "bmp_id", "x", "y", "z",
					"job_id", "machine_name", "cabinet", "frame", "board_num",
					"chip_x", "chip_y", "board_chip_x", "board_chip_y",
					"job_root_chip_x", "job_root_chip_y"),
					q.getRowColumnNames());
			assertCanMakeBoardLocation(q);
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, -1, -1, -1).isPresent());
			});
		}
	}

	@Test
	void findBoardByIPAddress() {
		try (var q = c.query(findBoardByIPAddress)) {
			assertEquals(2, q.getNumArguments());
			assertEquals(Set.of("board_id", "address", "bmp_id", "x", "y", "z",
					"job_id", "machine_name", "cabinet", "frame", "board_num",
					"chip_x", "chip_y", "board_chip_x", "board_chip_y",
					"job_root_chip_x", "job_root_chip_y"),
					q.getRowColumnNames());
			assertCanMakeBoardLocation(q);
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, "127.0.0.1").isPresent());
			});
		}
	}

	@Test
	void getJobsWithChanges() {
		try (var q = c.query(getJobsWithChanges)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("job_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getConnectedBoards() {
		try (var q = c.query(getConnectedBoards)) {
			assertEquals(7, q.getNumArguments());
			assertEquals(Set.of("board_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, -1, -1, -1, -1, -1, -1)
						.isPresent());
			});
		}
	}

	@Test
	void findBoardByNameAndXYZ() {
		try (var q = c.query(FIND_BOARD_BY_NAME_AND_XYZ)) {
			assertEquals(4, q.getNumArguments());
			assertEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME, -1, -1, -1).isPresent());
			});
		}
	}

	@Test
	void findBoardById() {
		try (var q = c.query(FIND_BOARD_BY_ID)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void findBoardByNameAndCFB() {
		try (var q = c.query(FIND_BOARD_BY_NAME_AND_CFB)) {
			assertEquals(4, q.getNumArguments());
			assertEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME, -1, -1, -1).isPresent());
			});
		}
	}

	@Test
	void findBoardByNameAndIPAddress() {
		try (var q = c.query(FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
			assertEquals(2, q.getNumArguments());
			assertEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME, "256.256.256.256").isPresent());
			});
		}
	}

	@Test
	void getFunctioningField() {
		try (var q = c.query(GET_FUNCTIONING_FIELD)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("functioning"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getGroupQuota() {
		try (var q = c.query(GET_GROUP_QUOTA)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("quota"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_GROUP).isPresent());
			});
		}
	}

	@Test
	void listAllGroups() {
		try (var q = c.query(LIST_ALL_GROUPS)) {
			assertEquals(0, q.getNumArguments());
			assertEquals(GROUP_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				// Not sure what default state is, but this should not error
				assertNotNull(q.call().toList());
			});
		}
	}

	@Test
	void listAllGroupsOfType() {
		try (var q = c.query(LIST_ALL_GROUPS_OF_TYPE)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(GROUP_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				// Not sure what default state is, but this should not error
				assertNotNull(q.call(GroupType.INTERNAL).toList());
			});
		}
	}

	@Test
	void getGroupById() {
		try (var q = c.query(GET_GROUP_BY_ID)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(GROUP_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_GROUP).isPresent());
			});
		}
	}

	@Test
	void getGroupByName() {
		try (var q = c.query(GET_GROUP_BY_NAME)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(GROUP_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getUsersOfGroup() {
		try (var q = c.query(GET_USERS_OF_GROUP)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(MEMBER_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_GROUP).isPresent());
			});
		}
	}

	@Test
	void getMembership() {
		try (var q = c.query(GET_MEMBERSHIP)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(MEMBER_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MEMBER).isPresent());
			});
		}
	}

	@Test
	void getMembershipsOfUser() {
		try (var q = c.query(GET_MEMBERSHIPS_OF_USER)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(MEMBER_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_USER).isPresent());
			});
		}
	}

	@Test
	void getUserQuota() {
		try (var q = c.query(GET_USER_QUOTA)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("quota_total", "user_id"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getCurrentUsage() {
		try (var q = c.query(GET_CURRENT_USAGE)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("current_usage"), q.getRowColumnNames());
			c.transaction(() -> {
				assertNull(q.call1(NO_GROUP).orElseThrow()
						.getObject("current_usage"));
			});
		}
	}

	@Test
	void getJobUsageAndQuota() {
		try (var q = c.query(GET_JOB_USAGE_AND_QUOTA)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("quota", "usage"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getConsolidationTargets() {
		try (var q = c.query(GET_CONSOLIDATION_TARGETS)) {
			assertEquals(0, q.getNumArguments());
			assertEquals(Set.of("job_id", "group_id", "usage"),
					q.getRowColumnNames());
			c.transaction(() -> {
				// Empty DB has no consolidation targets
				assertFalse(q.call1().isPresent());
			});
		}
	}

	@Test
	void isUserLocked() {
		try (var q = c.query(IS_USER_LOCKED)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("disabled", "locked", "user_id"),
					q.getRowColumnNames());
			c.transaction(() -> {
				// Empty DB has no consolidation targets
				assertFalse(q.call1("").isPresent());
			});
		}
	}

	@Test
	void getUserAuthorities() {
		try (var q = c.query(GET_USER_AUTHORITIES)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("trust_level", "encrypted_password",
					"openid_subject"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_USER).isPresent());
			});
		}
	}

	@Test
	void listAllUsers() {
		try (var q = c.query(LIST_ALL_USERS)) {
			assertEquals(0, q.getNumArguments());
			assertEquals(Set.of("user_id", "user_name", "openid_subject"),
					q.getRowColumnNames());
			c.transaction(() -> {
				// Testing DB has no users by default
				assertFalse(q.call1().isPresent());
			});
		}
	}

	@Test
	void listAllUsersOfType() {
		try (var q = c.query(LIST_ALL_USERS_OF_TYPE)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("user_id", "user_name", "openid_subject"),
					q.getRowColumnNames());
			c.transaction(() -> {
				// Testing DB has no users by default
				assertFalse(q.call1(false).isPresent());
			});
		}
	}

	@Test
	void getUserId() {
		try (var q = c.query(GET_USER_ID)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("user_id"), q.getRowColumnNames());
			c.transaction(() -> {
				// Testing DB has no users by default
				assertFalse(q.call1(NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getUserDetails() {
		try (var q = c.query(GET_USER_DETAILS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(USER_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_USER).isPresent());
			});
		}
	}

	@Test
	void getUserDetailsByName() {
		try (var q = c.query(GET_USER_DETAILS_BY_NAME)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(USER_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getUserDetailsBySubject() {
		try (var q = c.query(GET_USER_DETAILS_BY_SUBJECT)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(USER_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getGroupByNameAndMember() {
		try (var q = c.query(GET_GROUP_BY_NAME_AND_MEMBER)) {
			assertEquals(2, q.getNumArguments());
			assertEquals(Set.of("group_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME, NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getGroupsAndQuotasOfUser() {
		try (var q = c.query(GET_GROUPS_AND_QUOTAS_OF_USER)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("group_id", "quota"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME).isPresent());
			});
		}
	}

	@Test
	void countMachineThings() {
		try (var q = c.query(COUNT_MACHINE_THINGS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("board_count", "in_use", "num_jobs"),
					q.getRowColumnNames());
			c.transaction(() -> {
				var r = q.call1(NO_MACHINE).orElseThrow();
				assertEquals(0, r.getInt("board_count"));
				assertEquals(0, r.getInt("in_use"));
				assertEquals(0, r.getInt("num_jobs"));
			});
		}
	}

	@Test
	void countPoweredBoards() {
		try (var q = c.query(COUNT_POWERED_BOARDS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("c"), q.getRowColumnNames());
			c.transaction(() -> {
				assertEquals(0, q.call1(NO_JOB).orElseThrow().getInt("c"));
			});
		}
	}

	@Test
	void listLiveJobs() {
		try (var q = c.query(LIST_LIVE_JOBS)) {
			assertEquals(0, q.getNumArguments());
			assertEquals(
					Set.of("allocation_size", "create_timestamp", "job_id",
							"job_state", "keepalive_host", "keepalive_interval",
							"machine_id", "machine_name", "user_name"),
					q.getRowColumnNames());
			c.transaction(() -> {
				// No jobs right now
				assertFalse(q.call1().isPresent());
			});
		}
	}

	@Test
	void getLocalUserDetails() {
		try (var q = c.query(GET_LOCAL_USER_DETAILS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("user_id", "user_name", "encrypted_password"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_USER).isPresent());
			});
		}
	}

	@Test
	void getReportedBoards() {
		try (var q = c.query(getReportedBoards)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(
					Set.of("board_id", "num_reports", "x", "y", "z", "address"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(0).isPresent());
			});
		}
	}

	@Test
	void isBoardBlacklistCurrent() {
		try (var q = c.query(IS_BOARD_BLACKLIST_CURRENT)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("current"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBlacklistedChips() {
		try (var q = c.query(GET_BLACKLISTED_CHIPS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("x", "y", "notes"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBlacklistedCores() {
		try (var q = c.query(GET_BLACKLISTED_CORES)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("x", "y", "p", "notes"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBlacklistedLinks() {
		try (var q = c.query(GET_BLACKLISTED_LINKS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("x", "y", "direction", "notes"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBlacklistReads() {
		try (var q = c.query(GET_BLACKLIST_READS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("board_id", "board_num", "cabinet", "frame",
					"op_id", "bmp_serial_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getBlacklistWrites() {
		try (var q = c.query(GET_BLACKLIST_WRITES)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("board_id", "board_num", "cabinet", "frame",
					"op_id", "bmp_serial_id", "data"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getSerialInfoReqs() {
		try (var q = c.query(GET_SERIAL_INFO_REQS)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("board_id", "board_num", "cabinet", "frame",
					"op_id", "bmp_serial_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getCompletedBlacklistOp() {
		try (var q = c.query(GET_COMPLETED_BLACKLIST_OP)) {
			assertEquals(1, q.getNumArguments());
			assertEquals(Set.of("board_id", "op", "data", "failure", "failed"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BLACKLIST_OP).isPresent());
			});
		}
	}
}
