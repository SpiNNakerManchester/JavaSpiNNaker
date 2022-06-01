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
package uk.ac.manchester.spinnaker.alloc.db;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.BASIC_MACHINE_INFO;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.BOARD_COORDS_REQUIRED_COLUMNS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.GROUP_COLUMNS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.MEMBER_COLUMNS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.MSC_BOARD_COORDS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_BOARD;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_BOARD_SERIAL;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_GROUP;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_JOB;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_MACHINE;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_MEMBER;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_NAME;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_USER;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.USER_COLUMNS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assertCanMakeBoardLocation;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assertSetEquals;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.set;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
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
class DQLTest extends SQLQueries {
	@Autowired
	private DatabaseEngine mainDBEngine;

	private DatabaseEngine memdb;

	private Connection c;

	@BeforeAll
	void getMemoryDatabase() {
		assumeTrue(mainDBEngine != null, "spring-configured DB engine absent");
		memdb = mainDBEngine.getInMemoryDB();
	}

	@BeforeEach
	void getConnection() {
		c = memdb.getConnection();
		assumeTrue(c != null, "connection not generated");
	}

	@AfterEach
	void closeConnection() {
		c.close();
	}

	@Test
	void getAllMachines() {
		try (Query q = c.query(GET_ALL_MACHINES)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(BASIC_MACHINE_INFO, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(false).isPresent());
			});
		}
	}

	@Test
	void listMachineNames() {
		try (Query q = c.query(LIST_MACHINE_NAMES)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("machine_name", "in_service"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(false).isPresent());
			});
		}
	}

	@Test
	void getMachineById() {
		try (Query q = c.query(GET_MACHINE_BY_ID)) {
			assertEquals(2, q.getNumArguments());
			assertSetEquals(BASIC_MACHINE_INFO, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, false).isPresent());
			});
		}
	}

	@Test
	void getNamedMachine() {
		try (Query q = c.query(GET_NAMED_MACHINE)) {
			assertEquals(2, q.getNumArguments());
			assertSetEquals(BASIC_MACHINE_INFO, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME, false).isPresent());
			});
		}
	}

	@Test
	void getMachineJobs() {
		try (Query q = c.query(GET_MACHINE_JOBS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("job_id", "owner_name"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getMachineReports() {
		try (Query q = c.query(GET_MACHINE_REPORTS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(
					set("board_id", "report_id", "report_timestamp",
							"reported_issue", "reporter_name"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getJobIds() {
		try (Query q = c.query(GET_JOB_IDS)) {
			assertEquals(2, q.getNumArguments());
			assertSetEquals(set("job_id", "machine_id", "job_state",
					"keepalive_timestamp"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(0, 0).isPresent());
			});
		}
	}

	@Test
	void getLiveJobIds() {
		try (Query q = c.query(GET_LIVE_JOB_IDS)) {
			assertEquals(2, q.getNumArguments());
			assertSetEquals(set("job_id", "machine_id", "job_state",
					"keepalive_timestamp"), q.getRowColumnNames());
			c.transaction(() -> {
				q.call(0, 0);
				// Must not throw
			});
		}
	}

	@Test
	void getJob() {
		try (Query q = c.query(GET_JOB)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("job_id", "machine_id", "machine_name", "width",
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
		try (Query q = c.query(GET_JOB_BOARDS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("board_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getJobBoardCoords() {
		try (Query q = c.query(GET_JOB_BOARD_COORDS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(BOARD_COORDS_REQUIRED_COLUMNS,
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getJobChipDimensions() {
		try (Query q = c.query(GET_JOB_CHIP_DIMENSIONS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("width", "height"), q.getRowColumnNames());
			c.transaction(() -> {
				Row row = q.call1(NO_JOB).get();
				// These two are actually NULL when there's no job
				assertEquals(0, row.getInt("width"));
				assertEquals(0, row.getInt("height"));
			});
		}
	}

	@Test
	void getRootOfBoard() {
		try (Query q = c.query(GET_ROOT_OF_BOARD)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("root_x", "root_y"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getRootBMPAddress() {
		try (Query q = c.query(GET_ROOT_BMP_ADDRESS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("address"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getBMPAddress() {
		try (Query q = c.query(GET_BMP_ADDRESS)) {
			assertEquals(3, q.getNumArguments());
			assertSetEquals(set("address"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, 0, 0).isPresent());
			});
		}
	}

	@Test
	void getBoardAddress() {
		try (Query q = c.query(GET_BOARD_ADDRESS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("address"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBoardPowerInfo() {
		try (Query q = c.query(GET_BOARD_POWER_INFO)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("board_power", "power_off_timestamp",
					"power_on_timestamp"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBoardJob() {
		try (Query q = c.query(GET_BOARD_JOB)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("allocated_job"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBoardReports() {
		try (Query q = c.query(GET_BOARD_REPORTS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(
					set("board_id", "report_id", "report_timestamp",
							"reported_issue", "reporter_name"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBoardNumbers() {
		try (Query q = c.query(GET_BOARD_NUMBERS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("board_num"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getBmpBoardNumbers() {
		try (Query q = c.query(GET_BMP_BOARD_NUMBERS)) {
			assertEquals(3, q.getNumArguments());
			assertSetEquals(set("board_num"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, 0, 0).isPresent());
			});
		}
	}

	@Test
	void getLiveBoards() {
		try (Query q = c.query(GET_LIVE_BOARDS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(BOARD_COORDS_REQUIRED_COLUMNS,
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getDeadBoards() {
		try (Query q = c.query(GET_DEAD_BOARDS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(BOARD_COORDS_REQUIRED_COLUMNS,
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getDeadLinks() {
		try (Query q = c.query(getDeadLinks)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(
					set("board_1_x", "board_1_y", "board_1_z", "board_1_c",
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
		try (Query q = c.query(GET_AVAILABLE_BOARD_NUMBERS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("board_num"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getTags() {
		try (Query q = c.query(GET_TAGS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("tag"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getSumBoardsPowered() {
		// This query always produces one row
		try (Query q = c.query(GET_SUM_BOARDS_POWERED)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("total_on"), q.getRowColumnNames());
			c.transaction(() -> {
				Row row = q.call1(NO_JOB).get();
				assertEquals(0, row.getInt("total_on"));
			});
		}
	}

	@Test
	void getBoardConnectInfo() {
		try (Query q = c.query(GET_BOARD_CONNECT_INFO)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("board_id", "address", "x", "y", "z", "root_x",
					"root_y"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getRootCoords() {
		try (Query q = c.query(GET_ROOT_COORDS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("x", "y", "z", "root_x", "root_y"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getAllocationTasks() {
		try (Query q = c.query(getAllocationTasks)) {
			assertEquals(0, q.getNumArguments());
			assertSetEquals(set("req_id", "job_id", "num_boards", "width",
					"height", "board_id", "machine_id", "max_dead_boards",
					"max_height", "max_width", "job_state", "importance"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1().isPresent());
			});
		}
	}

	@Test
	void findFreeBoard() {
		try (Query q = c.query(FIND_FREE_BOARD)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("x", "y", "z"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getBoardByCoords() {
		try (Query q = c.query(GET_BOARD_BY_COORDS)) {
			assertEquals(4, q.getNumArguments());
			assertSetEquals(set("board_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, -1, -1, -1).isPresent());
			});
		}
	}

	@Test
	void findExpiredJobs() {
		try (Query q = c.query(FIND_EXPIRED_JOBS)) {
			assertEquals(0, q.getNumArguments());
			assertSetEquals(set("job_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1().isPresent());
			});
		}
	}

	@Test
	void loadDirInfo() {
		try (Query q = c.query(LOAD_DIR_INFO)) {
			assertEquals(0, q.getNumArguments());
			assertSetEquals(set("z", "direction", "dx", "dy", "dz"),
					q.getRowColumnNames());
			c.transaction(() -> {
				q.call();
			});
		}
	}

	@Test
	void getChanges() {
		try (Query q = c.query(GET_CHANGES)) {
			assertEquals(1, q.getNumArguments());
			Set<String> colNames = q.getRowColumnNames();
			assertSetEquals(
					set("change_id", "job_id", "board_id", "power", "fpga_n",
							"fpga_s", "fpga_e", "fpga_w", "fpga_nw", "fpga_se",
							"in_progress", "from_state", "to_state",
							"board_num", "cabinet", "frame", "bmp_id"),
					colNames);
			// Ensure that this link is maintained
			for (Direction d : Direction.values()) {
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
		try (Query q = c.query(findRectangle)) {
			assertEquals(4, q.getNumArguments());
			assertSetEquals(set("id", "x", "y", "z", "available"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(-1, -1, NO_MACHINE, 0).isPresent());
			});
		}
	}

	@Test
	void findRectangleAt() {
		try (Query q = c.query(findRectangleAt)) {
			assertEquals(5, q.getNumArguments());
			assertSetEquals(set("id", "x", "y", "z", "available"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(
						q.call1(NO_BOARD, -1, -1, NO_MACHINE, 0).isPresent());
			});
		}
	}

	@Test
	void findLocation() {
		try (Query q = c.query(findLocation)) {
			assertEquals(2, q.getNumArguments());
			assertSetEquals(set("x", "y", "z"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void countConnected() {
		try (Query q = c.query(countConnected)) {
			assertEquals(5, q.getNumArguments());
			assertSetEquals(set("connected_size"), q.getRowColumnNames());
			c.transaction(() -> {
				Row row = q.call1(NO_MACHINE, -1, -1, -1, -1).get();
				assertEquals(0, row.getInt("connected_size"));
			});
		}
	}

	@Test
	void countPendingChanges() {
		try (Query q = c.query(COUNT_PENDING_CHANGES)) {
			assertEquals(0, q.getNumArguments());
			assertSetEquals(set("c"), q.getRowColumnNames());
			c.transaction(() -> {
				Row row = q.call1().get();
				assertEquals(0, row.getInt("c"));
			});
		}
	}

	@Test
	void getPerimeterLinks() {
		try (Query q = c.query(getPerimeterLinks)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("board_id", "direction"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB).isPresent());
			});
		}
	}

	@Test
	void findBoardByGlobalChip() {
		try (Query q = c.query(findBoardByGlobalChip)) {
			assertEquals(3, q.getNumArguments());
			assertSetEquals(set("board_id", "address", "bmp_id", "x", "y", "z",
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
		try (Query q = c.query(findBoardByJobChip)) {
			assertEquals(4, q.getNumArguments());
			assertSetEquals(
					set("board_id", "address", "x", "y", "z", "job_id",
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
		try (Query q = c.query(findBoardByLogicalCoords)) {
			assertEquals(4, q.getNumArguments());
			assertSetEquals(set("board_id", "address", "bmp_id", "x", "y", "z",
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
		try (Query q = c.query(findBoardByPhysicalCoords)) {
			assertEquals(4, q.getNumArguments());
			assertSetEquals(set("board_id", "address", "bmp_id", "x", "y", "z",
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
		try (Query q = c.query(findBoardByIPAddress)) {
			assertEquals(2, q.getNumArguments());
			assertSetEquals(set("board_id", "address", "bmp_id", "x", "y", "z",
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
		try (Query q = c.query(getJobsWithChanges)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("job_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getConnectedBoards() {
		try (Query q = c.query(getConnectedBoards)) {
			assertEquals(7, q.getNumArguments());
			assertSetEquals(set("board_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MACHINE, -1, -1, -1, -1, -1, -1)
						.isPresent());
			});
		}
	}

	@Test
	void findBoardByNameAndXYZ() {
		try (Query q = c.query(FIND_BOARD_BY_NAME_AND_XYZ)) {
			assertEquals(4, q.getNumArguments());
			assertSetEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME, -1, -1, -1).isPresent());
			});
		}
	}

	@Test
	void findBoardById() {
		try (Query q = c.query(FIND_BOARD_BY_ID)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void findBoardByNameAndCFB() {
		try (Query q = c.query(FIND_BOARD_BY_NAME_AND_CFB)) {
			assertEquals(4, q.getNumArguments());
			assertSetEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME, -1, -1, -1).isPresent());
			});
		}
	}

	@Test
	void findBoardByNameAndIPAddress() {
		try (Query q = c.query(FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
			assertEquals(2, q.getNumArguments());
			assertSetEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME, "256.256.256.256").isPresent());
			});
		}
	}

	@Test
	void getFunctioningField() {
		try (Query q = c.query(GET_FUNCTIONING_FIELD)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("functioning"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getGroupQuota() {
		try (Query q = c.query(GET_GROUP_QUOTA)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("quota"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_GROUP).isPresent());
			});
		}
	}

	@Test
	void listAllGroups() {
		try (Query q = c.query(LIST_ALL_GROUPS)) {
			assertEquals(0, q.getNumArguments());
			assertSetEquals(GROUP_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				// Not sure what default state is, but this should not error
				assertNotNull(q.call().toList());
			});
		}
	}

	@Test
	void listAllGroupsOfType() {
		try (Query q = c.query(LIST_ALL_GROUPS_OF_TYPE)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(GROUP_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				// Not sure what default state is, but this should not error
				assertNotNull(q.call(GroupType.INTERNAL).toList());
			});
		}
	}

	@Test
	void getGroupById() {
		try (Query q = c.query(GET_GROUP_BY_ID)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(GROUP_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_GROUP).isPresent());
			});
		}
	}

	@Test
	void getGroupByName() {
		try (Query q = c.query(GET_GROUP_BY_NAME)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(GROUP_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getUsersOfGroup() {
		try (Query q = c.query(GET_USERS_OF_GROUP)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(MEMBER_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_GROUP).isPresent());
			});
		}
	}

	@Test
	void getMembership() {
		try (Query q = c.query(GET_MEMBERSHIP)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(MEMBER_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_MEMBER).isPresent());
			});
		}
	}

	@Test
	void getMembershipsOfUser() {
		try (Query q = c.query(GET_MEMBERSHIPS_OF_USER)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(MEMBER_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_USER).isPresent());
			});
		}
	}

	@Test
	void getUserQuota() {
		try (Query q = c.query(GET_USER_QUOTA)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("quota_total", "user_id"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getCurrentUsage() {
		try (Query q = c.query(GET_CURRENT_USAGE)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("current_usage"), q.getRowColumnNames());
			c.transaction(() -> {
				assertNull(q.call1(NO_GROUP).get().getObject("current_usage"));
			});
		}
	}

	@Test
	void getJobUsageAndQuota() {
		try (Query q = c.query(GET_JOB_USAGE_AND_QUOTA)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("quota", "usage"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getConsolidationTargets() {
		try (Query q = c.query(GET_CONSOLIDATION_TARGETS)) {
			assertEquals(0, q.getNumArguments());
			assertSetEquals(set("job_id", "group_id", "usage"),
					q.getRowColumnNames());
			c.transaction(() -> {
				// Empty DB has no consolidation targets
				assertFalse(q.call1().isPresent());
			});
		}
	}

	@Test
	void isUserLocked() {
		try (Query q = c.query(IS_USER_LOCKED)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("disabled", "locked", "user_id"),
					q.getRowColumnNames());
			c.transaction(() -> {
				// Empty DB has no consolidation targets
				assertFalse(q.call1("").isPresent());
			});
		}
	}

	@Test
	void getUserAuthorities() {
		try (Query q = c.query(GET_USER_AUTHORITIES)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(
					set("trust_level", "encrypted_password", "openid_subject"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_USER).isPresent());
			});
		}
	}

	@Test
	void listAllUsers() {
		try (Query q = c.query(LIST_ALL_USERS)) {
			assertEquals(0, q.getNumArguments());
			assertSetEquals(set("user_id", "user_name", "openid_subject"),
					q.getRowColumnNames());
			c.transaction(() -> {
				// Testing DB has no users by default
				assertFalse(q.call1().isPresent());
			});
		}
	}

	@Test
	void listAllUsersOfType() {
		try (Query q = c.query(LIST_ALL_USERS_OF_TYPE)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("user_id", "user_name", "openid_subject"),
					q.getRowColumnNames());
			c.transaction(() -> {
				// Testing DB has no users by default
				assertFalse(q.call1(false).isPresent());
			});
		}
	}

	@Test
	void getUserId() {
		try (Query q = c.query(GET_USER_ID)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("user_id"), q.getRowColumnNames());
			c.transaction(() -> {
				// Testing DB has no users by default
				assertFalse(q.call1(NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getUserDetails() {
		try (Query q = c.query(GET_USER_DETAILS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(USER_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_USER).isPresent());
			});
		}
	}

	@Test
	void getUserDetailsByName() {
		try (Query q = c.query(GET_USER_DETAILS_BY_NAME)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(USER_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getUserDetailsBySubject() {
		try (Query q = c.query(GET_USER_DETAILS_BY_SUBJECT)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(USER_COLUMNS, q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getGroupByNameAndMember() {
		try (Query q = c.query(GET_GROUP_BY_NAME_AND_MEMBER)) {
			assertEquals(2, q.getNumArguments());
			assertSetEquals(set("group_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME, NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getGroupsOfUser() {
		try (Query q = c.query(GET_GROUPS_OF_USER)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("group_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_NAME).isPresent());
			});
		}
	}

	@Test
	void countMachineThings() {
		try (Query q = c.query(COUNT_MACHINE_THINGS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("board_count", "in_use", "num_jobs"),
					q.getRowColumnNames());
			c.transaction(() -> {
				Row r = q.call1(NO_MACHINE).get();
				assertEquals(0, r.getInt("board_count"));
				assertEquals(0, r.getInt("in_use"));
				assertEquals(0, r.getInt("num_jobs"));
			});
		}
	}

	@Test
	void countPoweredBoards() {
		try (Query q = c.query(COUNT_POWERED_BOARDS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("c"), q.getRowColumnNames());
			c.transaction(() -> {
				assertEquals(0, q.call1(NO_JOB).get().getInt("c"));
			});
		}
	}

	@Test
	void listLiveJobs() {
		try (Query q = c.query(LIST_LIVE_JOBS)) {
			assertEquals(0, q.getNumArguments());
			assertSetEquals(
					set("allocation_size", "create_timestamp", "job_id",
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
		try (Query q = c.query(GET_LOCAL_USER_DETAILS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("user_id", "user_name", "encrypted_password"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_USER).isPresent());
			});
		}
	}

	@Test
	void getReportedBoards() {
		try (Query q = c.query(getReportedBoards)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(
					set("board_id", "num_reports", "x", "y", "z", "address"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(0).isPresent());
			});
		}
	}

	@Test
	void getBlacklistedChips() {
		try (Query q = c.query(GET_BLACKLISTED_CHIPS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("x", "y", "notes"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD_SERIAL).isPresent());
			});
		}
	}

	@Test
	void getBlacklistedCores() {
		try (Query q = c.query(GET_BLACKLISTED_CORES)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("x", "y", "p", "notes"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD_SERIAL).isPresent());
			});
		}
	}

	@Test
	void getBlacklistedLinks() {
		try (Query q = c.query(GET_BLACKLISTED_LINKS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("x", "y", "direction", "notes"),
					q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(NO_BOARD_SERIAL).isPresent());
			});
		}
	}
}
