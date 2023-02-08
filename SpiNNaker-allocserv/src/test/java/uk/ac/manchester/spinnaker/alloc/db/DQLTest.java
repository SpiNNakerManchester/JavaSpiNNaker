/*
 * Copyright (c) 2021-2022 The University of Manchester
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
import static uk.ac.manchester.spinnaker.alloc.db.Row.object;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;

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
	@Test
	void getAllMachines() {
		try (var q = c.query(GET_ALL_MACHINES)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, false).isPresent());
			});
		}
	}

	@Test
	void listMachineNames() {
		try (var q = c.query(LIST_MACHINE_NAMES)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, false).isPresent());
			});
		}
	}

	@Test
	void getMachineById() {
		try (var q = c.query(GET_MACHINE_BY_ID)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE,
						false).isPresent());
			});
		}
	}

	@Test
	void getNamedMachine() {
		try (var q = c.query(GET_NAMED_MACHINE)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_NAME, false).isPresent());
			});
		}
	}

	@Test
	void getMachineJobs() {
		try (var q = c.query(GET_MACHINE_JOBS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getMachineReports() {
		try (var q = c.query(GET_MACHINE_REPORTS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getJobIds() {
		try (var q = c.query(GET_JOB_IDS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, 0, 0).isPresent());
			});
		}
	}

	@Test
	void getLiveJobIds() {
		try (var q = c.query(GET_LIVE_JOB_IDS)) {
			c.transaction(() -> {
				q.call(Row.integer("job_id"), 0, 0);
				// Must not throw
			});
		}
	}

	@Test
	void getJob() {
		try (var q = c.query(GET_JOB)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getJobBoards() {
		try (var q = c.query(GET_JOB_BOARDS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getJobBoardCoords() {
		try (var q = c.query(GET_JOB_BOARD_COORDS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getJobChipDimensions() {
		try (var q = c.query(GET_JOB_CHIP_DIMENSIONS)) {
			c.transaction(() -> {
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
				assertFalse(q.call1((row) -> 1, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getRootBMPAddress() {
		try (var q = c.query(GET_ROOT_BMP_ADDRESS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getBMPAddress() {
		try (var q = c.query(GET_BMP_ADDRESS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE, 0,
						0).isPresent());
			});
		}
	}

	@Test
	void getBoardAddress() {
		try (var q = c.query(GET_BOARD_ADDRESS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBoardPowerInfo() {
		try (var q = c.query(GET_BOARD_POWER_INFO)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBoardJob() {
		try (var q = c.query(GET_BOARD_JOB)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBoardReports() {
		try (var q = c.query(GET_BOARD_REPORTS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBoardNumbers() {
		try (var q = c.query(GET_BOARD_NUMBERS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getBmpBoardNumbers() {
		try (var q = c.query(GET_BMP_BOARD_NUMBERS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE, 0, 0).isPresent());
			});
		}
	}

	@Test
	void getLiveBoards() {
		try (var q = c.query(GET_LIVE_BOARDS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getDeadBoards() {
		try (var q = c.query(GET_DEAD_BOARDS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getAllBoards() {
		try (var q = c.query(GET_ALL_BOARDS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getAllBoardsOfAllMachines() {
		try (var q = c.query(GET_ALL_BOARDS_OF_ALL_MACHINES)) {
			c.transaction(() -> {
				// As long as this doesn't throw, the test passes
				return q.call1((row) -> 1).isPresent();
			});
		}
	}

	@Test
	void getDeadLinks() {
		try (var q = c.query(getDeadLinks)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getAvailableBoardNumbers() {
		try (var q = c.query(GET_AVAILABLE_BOARD_NUMBERS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getTags() {
		try (var q = c.query(GET_TAGS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getSumBoardsPowered() {
		// This query always produces one row
		try (var q = c.query(GET_SUM_BOARDS_POWERED)) {
			c.transaction(() -> {
				var row = q.call1(integer("total_on"), NO_JOB).orElseThrow();
				assertEquals(0, row);
			});
		}
	}

	@Test
	void getBoardConnectInfo() {
		try (var q = c.query(GET_BOARD_CONNECT_INFO)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getRootCoords() {
		try (var q = c.query(GET_ROOT_COORDS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getAllocationTasks() {
		try (var q = c.query(getAllocationTasks)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, QUEUED).isPresent());
			});
		}
	}

	@Test
	void findFreeBoard() {
		try (var q = c.query(FIND_FREE_BOARD)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getBoardByCoords() {
		try (var q = c.query(GET_BOARD_BY_COORDS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE, -1, -1,
						-1).isPresent());
			});
		}
	}

	@Test
	void findExpiredJobs() {
		try (var q = c.query(FIND_EXPIRED_JOBS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, 0).isPresent());
			});
		}
	}

	@Test
	void loadDirInfo() {
		try (var q = c.query(LOAD_DIR_INFO)) {
			c.transaction(() -> {
				q.call((row) -> null);
			});
		}
	}

	@Test
	void getChanges() {
		try (var q = c.query(GET_CHANGES)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_JOB).isPresent());
			});
		}
	}

	@Test
	void findRectangle() {
		try (var q = c.query(findRectangle)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, -1, -1, NO_MACHINE,
						0).isPresent());
			});
		}
	}

	@Test
	void findRectangleAt() {
		try (var q = c.query(findRectangleAt)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BOARD, -1, -1, NO_MACHINE,
								0).isPresent());
			});
		}
	}

	@Test
	void findLocation() {
		try (var q = c.query(findLocation)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE,
						NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void countConnected() {
		try (var q = c.query(countConnected)) {
			c.transaction(() -> {
				var row = q.call1(integer("connected_size"), NO_MACHINE, -1,
						-1, -1, -1).orElseThrow();
				assertEquals(0, row);
			});
		}
	}

	@Test
	void countPendingChanges() {
		try (var q = c.query(COUNT_PENDING_CHANGES)) {
			c.transaction(() -> {
				var row = q.call1(integer("c")).orElseThrow();
				assertEquals(0, row);
			});
		}
	}

	@Test
	void getPerimeterLinks() {
		try (var q = c.query(getPerimeterLinks)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_JOB).isPresent());
			});
		}
	}

	@Test
	void findBoardByGlobalChip() {
		try (var q = c.query(findBoardByGlobalChip)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE, -1,
						-1).isPresent());
			});
		}
	}

	@Test
	void findBoardByJobChip() {
		try (var q = c.query(findBoardByJobChip)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_JOB, NO_BOARD, -1,
						-1).isPresent());
			});
		}
	}

	@Test
	void findBoardByLogicalCoords() {
		try (var q = c.query(findBoardByLogicalCoords)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE, -1, -1,
						-1).isPresent());
			});
		}
	}

	@Test
	void findBoardByPhysicalCoords() {
		try (var q = c.query(findBoardByPhysicalCoords)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE, -1, -1,
						-1).isPresent());
			});
		}
	}

	@Test
	void findBoardByIPAddress() {
		try (var q = c.query(findBoardByIPAddress)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE,
						"127.0.0.1").isPresent());
			});
		}
	}

	@Test
	void getJobsWithChanges() {
		try (var q = c.query(getJobsWithChanges)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE, 0).isPresent());
			});
		}
	}

	@Test
	void getConnectedBoards() {
		try (var q = c.query(getConnectedBoards)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE, -1, -1, -1, -1, -1,
						-1).isPresent());
			});
		}
	}

	@Test
	void findBoardByNameAndXYZ() {
		try (var q = c.query(FIND_BOARD_BY_NAME_AND_XYZ)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_NAME, -1, -1, -1)
						.isPresent());
			});
		}
	}

	@Test
	void findBoardById() {
		try (var q = c.query(FIND_BOARD_BY_ID)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void findBoardByNameAndCFB() {
		try (var q = c.query(FIND_BOARD_BY_NAME_AND_CFB)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_NAME, -1, -1, -1)
						.isPresent());
			});
		}
	}

	@Test
	void findBoardByNameAndIPAddress() {
		try (var q = c.query(FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_NAME, "256.256.256.256")
						.isPresent());
			});
		}
	}

	@Test
	void getFunctioningField() {
		try (var q = c.query(GET_FUNCTIONING_FIELD)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getGroupQuota() {
		try (var q = c.query(GET_GROUP_QUOTA)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_GROUP).isPresent());
			});
		}
	}

	@Test
	void listAllGroups() {
		try (var q = c.query(LIST_ALL_GROUPS)) {
			c.transaction(() -> {
				// Not sure what default state is, but this should not error
				assertNotNull(q.call((row) -> 1));
			});
		}
	}

	@Test
	void listAllGroupsOfType() {
		try (var q = c.query(LIST_ALL_GROUPS_OF_TYPE)) {
			c.transaction(() -> {
				// Not sure what default state is, but this should not error
				assertNotNull(q.call((row) -> 1, GroupType.INTERNAL));
			});
		}
	}

	@Test
	void getGroupById() {
		try (var q = c.query(GET_GROUP_BY_ID)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_GROUP).isPresent());
			});
		}
	}

	@Test
	void getGroupByName() {
		try (var q = c.query(GET_GROUP_BY_NAME)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getUsersOfGroup() {
		try (var q = c.query(GET_USERS_OF_GROUP)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_GROUP).isPresent());
			});
		}
	}

	@Test
	void getMembership() {
		try (var q = c.query(GET_MEMBERSHIP)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MEMBER).isPresent());
			});
		}
	}

	@Test
	void getMembershipsOfUser() {
		try (var q = c.query(GET_MEMBERSHIPS_OF_USER)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_USER).isPresent());
			});
		}
	}

	@Test
	void getUserQuota() {
		try (var q = c.query(GET_USER_QUOTA)) {
			c.transaction(() -> {
				// Still get a quota, it is just 0
				assertTrue(q.call1((row) -> 1, NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getCurrentUsage() {
		try (var q = c.query(GET_CURRENT_USAGE)) {
			c.transaction(() -> {
				assertEquals(0, q.call1(integer("current_usage"), NO_GROUP)
						.orElseThrow());
			});
		}
	}

	@Test
	void getJobUsageAndQuota() {
		try (var q = c.query(GET_JOB_USAGE_AND_QUOTA)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_JOB).isPresent());
			});
		}
	}

	@Test
	void getConsolidationTargets() {
		try (var q = c.query(GET_CONSOLIDATION_TARGETS)) {
			c.transaction(() -> {
				// Empty DB has no consolidation targets
				assertFalse(q.call1((row) -> 1).isPresent());
			});
		}
	}

	@Test
	void isUserLocked() {
		try (var q = c.query(IS_USER_LOCKED)) {
			c.transaction(() -> {
				// Empty DB has no consolidation targets
				assertFalse(q.call1((row) -> 1, "").isPresent());
			});
		}
	}

	@Test
	void getUserAuthorities() {
		try (var q = c.query(GET_USER_AUTHORITIES)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_USER).isPresent());
			});
		}
	}

	@Test
	void listAllUsers() {
		try (var q = c.query(LIST_ALL_USERS)) {
			c.transaction(() -> {
				// Testing DB has no users by default
				assertFalse(q.call1((row) -> 1).isPresent());
			});
		}
	}

	@Test
	void listAllUsersOfType() {
		try (var q = c.query(LIST_ALL_USERS_OF_TYPE)) {
			c.transaction(() -> {
				// Testing DB has no users by default
				assertFalse(q.call1((row) -> 1, false).isPresent());
			});
		}
	}

	@Test
	void getUserId() {
		try (var q = c.query(GET_USER_ID)) {
			c.transaction(() -> {
				// Testing DB has no users by default
				assertFalse(q.call1((row) -> 1, NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getUserDetails() {
		try (var q = c.query(GET_USER_DETAILS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_USER).isPresent());
			});
		}
	}

	@Test
	void getUserDetailsByName() {
		try (var q = c.query(GET_USER_DETAILS_BY_NAME)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getUserDetailsBySubject() {
		try (var q = c.query(GET_USER_DETAILS_BY_SUBJECT)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getGroupByNameAndMember() {
		try (var q = c.query(GET_GROUP_BY_NAME_AND_MEMBER)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_NAME, NO_NAME).isPresent());
			});
		}
	}

	@Test
	void getGroupsAndQuotasOfUser() {
		try (var q = c.query(GET_GROUPS_AND_QUOTAS_OF_USER)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_NAME).isPresent());
			});
		}
	}

	@Test
	void countMachineThings() {
		try (var q = c.query(COUNT_MACHINE_THINGS)) {
			c.transaction(() -> {
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
				assertEquals(0, q.call1(integer("c"), NO_JOB).orElseThrow());
			});
		}
	}

	@Test
	void listLiveJobs() {
		try (var q = c.query(LIST_LIVE_JOBS)) {
			c.transaction(() -> {
				// No jobs right now
				assertFalse(q.call1((row) -> 1).isPresent());
			});
		}
	}

	@Test
	void getLocalUserDetails() {
		try (var q = c.query(GET_LOCAL_USER_DETAILS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_USER).isPresent());
			});
		}
	}

	@Test
	void getReportedBoards() {
		try (var q = c.query(getReportedBoards)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, 0).isPresent());
			});
		}
	}

	@Test
	void isBoardBlacklistCurrent() {
		try (var q = c.query(IS_BOARD_BLACKLIST_CURRENT)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBlacklistedChips() {
		try (var q = c.query(GET_BLACKLISTED_CHIPS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBlacklistedCores() {
		try (var q = c.query(GET_BLACKLISTED_CORES)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBlacklistedLinks() {
		try (var q = c.query(GET_BLACKLISTED_LINKS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BOARD).isPresent());
			});
		}
	}

	@Test
	void getBlacklistReads() {
		try (var q = c.query(GET_BLACKLIST_READS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getBlacklistWrites() {
		try (var q = c.query(GET_BLACKLIST_WRITES)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getSerialInfoReqs() {
		try (var q = c.query(GET_SERIAL_INFO_REQS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_MACHINE).isPresent());
			});
		}
	}

	@Test
	void getCompletedBlacklistOp() {
		try (var q = c.query(GET_COMPLETED_BLACKLIST_OP)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, NO_BLACKLIST_OP).isPresent());
			});
		}
	}
}
