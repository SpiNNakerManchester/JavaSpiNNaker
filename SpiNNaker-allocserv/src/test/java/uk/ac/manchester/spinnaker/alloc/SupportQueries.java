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
package uk.ac.manchester.spinnaker.alloc;

import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.storage.GeneratesID;
import uk.ac.manchester.spinnaker.storage.Parameter;
import uk.ac.manchester.spinnaker.storage.ResultColumn;
import uk.ac.manchester.spinnaker.storage.SingleRowResult;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * SQL queries for testing purposes.
 *
 * @see SQLQueries
 */
@UsedInJavadocOnly(SQLQueries.class)
public interface SupportQueries {
	/** Insert a request-by-size. */
	@Parameter("job_id")
	@Parameter("num_boards")
	String TEST_INSERT_REQ_SIZE =
			"INSERT INTO job_request(job_id, num_boards) VALUES (?, ?)";

	/** Insert a request-by-dimensions. */
	@Parameter("job_id")
	@Parameter("width")
	@Parameter("height")
	@Parameter("max_dead_boards")
	String TEST_INSERT_REQ_DIMS =
			"INSERT INTO job_request(job_id, width, height, "
					+ "max_dead_boards) VALUES (?, ?, ?, ?)";

	/** Insert a request-for-a-board. */
	@Parameter("job_id")
	@Parameter("board_id")
	String TEST_INSERT_REQ_BOARD =
			"INSERT INTO job_request(job_id, board_id) VALUES (?, ?)";

	/** Count the jobs. */
	@ResultColumn("cnt")
	@SingleRowResult
	String TEST_COUNT_JOBS = "SELECT COUNT(*) AS cnt FROM jobs";

	/** Count the active allocation requests. */
	@Parameter("job_state")
	@ResultColumn("cnt")
	@SingleRowResult
	String TEST_COUNT_REQUESTS =
			"SELECT COUNT(*) AS cnt FROM job_request "
					+ "JOIN jobs USING (job_id) WHERE job_state = :job_state";

	/** Count the active power change requests. */
	@ResultColumn("cnt")
	@SingleRowResult
	String TEST_COUNT_POWER_CHANGES =
			"SELECT COUNT(*) AS cnt FROM pending_changes";

	/** Directly set the state of a job. */
	@Parameter("state")
	@Parameter("job")
	String TEST_SET_JOB_STATE =
			"UPDATE jobs SET job_state = :state WHERE job_id = :job";

	/** Directly set when a job died. */
	@Parameter("timestamp")
	@Parameter("job")
	String TEST_SET_JOB_DEATH_TIME =
			"UPDATE jobs SET death_timestamp = :timestamp WHERE job_id = :job";

	/** Get the quota for a user on a machine. */
	@Parameter("machine_id")
	@Parameter("user_id")
	@ResultColumn("quota")
	String TEST_GET_QUOTA =
			"SELECT quota FROM quotas WHERE machine_id = ? AND user_id = ?";

	/** Set the quota for a group. */
	@Parameter("quota")
	@Parameter("group")
	String TEST_SET_QUOTA =
			"UPDATE groups SET quota = :quota WHERE group_id = :group";

	/** Create a machine, specifying the ID. */
	@Parameter("machine_id")
	@Parameter("machine_name")
	@Parameter("width")
	@Parameter("height")
	@Parameter("depth")
	@GeneratesID
	String INSERT_MACHINE =
			"INSERT OR IGNORE INTO machines(machine_id, machine_name, "
					+ "width, height, [depth], board_model) "
					+ "VALUES (?, ?, ?, ?, ?, 5)";

	/** Create a BMP, specifying the ID. */
	@Parameter("bmp_id")
	@Parameter("machine_id")
	@Parameter("address")
	@Parameter("cabinet")
	@Parameter("frame")
	@GeneratesID
	String INSERT_BMP_WITH_ID =
			"INSERT OR IGNORE INTO bmp(bmp_id, machine_id, address, "
					+ "cabinet, frame) VALUES (?, ?, ?, ?, ?)";

	/** Create a user, specifying the ID. */
	@Parameter("user_id")
	@Parameter("user_name")
	@Parameter("trust_level")
	@Parameter("disabled")
	@GeneratesID
	String INSERT_USER = "INSERT OR IGNORE INTO user_info(user_id, user_name, "
			+ "trust_level, disabled, encrypted_password) "
			+ "VALUES (?, ?, ?, ?, '*')";

	/** Create a group, specifying the ID. */
	@Parameter("group_id")
	@Parameter("group_name")
	@Parameter("quota")
	@GeneratesID
	String INSERT_GROUP = "INSERT OR IGNORE INTO "
			+ "groups(group_id, group_name, quota, group_type) "
			+ "VALUES (?, ?, ?, 0)";

	/** Create a user/group association, specifying the ID. */
	@Parameter("membership_id")
	@Parameter("user_id")
	@Parameter("group_id")
	@GeneratesID
	String INSERT_MEMBER = "INSERT OR IGNORE INTO group_memberships("
			+ "membership_id, user_id, group_id) VALUES (?, ?, ?)";

	/** Create a board, specifying the ID. */
	@Parameter("board_id")
	@Parameter("address")
	@Parameter("bmp_id")
	@Parameter("board_num")
	@Parameter("machine_id")
	@Parameter("x")
	@Parameter("y")
	@Parameter("z")
	@Parameter("root_x")
	@Parameter("root_y")
	@Parameter("board_power")
	@GeneratesID
	String INSERT_BOARD_WITH_ID =
			"INSERT OR IGNORE INTO boards(board_id, address, "
					+ "bmp_id, board_num, machine_id, x, y, z, "
					+ "root_x, root_y, board_power) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/** Create a job, specifying timestamps. */
	@Parameter("machine_id")
	@Parameter("owner")
	@Parameter("group_id")
	@Parameter("root_id")
	@Parameter("job_state")
	@Parameter("create_timestamp")
	@Parameter("allocation_timestamp")
	@Parameter("death_timestamp")
	@Parameter("allocation_size")
	@Parameter("keepalive_interval")
	@Parameter("keepalive_timestamp")
	@GeneratesID
	String INSERT_JOB_WITH_TIMESTAMPS =
			"INSERT INTO jobs(machine_id, owner, group_id, root_id, "
					+ "job_state, create_timestamp, allocation_timestamp, "
					+ "death_timestamp, allocation_size, "
					+ "keepalive_interval, keepalive_timestamp) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
}
