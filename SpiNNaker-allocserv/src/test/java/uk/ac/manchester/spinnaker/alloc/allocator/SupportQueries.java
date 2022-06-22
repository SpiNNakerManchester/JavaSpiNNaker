/*
 * Copyright (c) 2021-2022 The University of Manchester
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

import uk.ac.manchester.spinnaker.storage.Parameter;
import uk.ac.manchester.spinnaker.storage.ResultColumn;
import uk.ac.manchester.spinnaker.storage.SingleRowResult;

/**
 * SQL queries for testing purposes.
 */
interface SupportQueries {
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
}
