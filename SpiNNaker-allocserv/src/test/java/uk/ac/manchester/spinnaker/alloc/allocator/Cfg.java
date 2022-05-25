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

import static uk.ac.manchester.spinnaker.alloc.security.TrustLevel.BASIC;

import java.time.Duration;
import java.time.Instant;

import org.springframework.lang.NonNull;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.storage.GeneratesID;
import uk.ac.manchester.spinnaker.storage.Parameter;

/** Various configuration support bits for testing. */
@SuppressWarnings({
	"checkstyle:MagicNumber", "checkstyle:ParameterNumber"
})
abstract class Cfg {
	/** Machine ID. */
	static final int MACHINE = 1000;

	/** BMP ID. */
	static final int BMP = 2000;

	/** BMP IP address. */
	static final String BMP_ADDR = "1.1.1.1";

	/** Board ID. */
	static final int BOARD = 3000;

	/** Board IP address. */
	static final String BOARD_ADDR = "2.2.2.2";

	/** User ID. */
	static final int USER = 4000;

	/** Group ID. */
	static final int GROUP = 5000;

	/** User-group membership ID. */
	static final int MEMBERSHIP = 6000;

	/** Initial quota. */
	static final int INITIAL_QUOTA = 1024;

	@Parameter("machine_id")
	@Parameter("machine_name")
	@Parameter("width")
	@Parameter("height")
	@Parameter("depth")
	@GeneratesID
	private static final String INSERT_MACHINE =
			"INSERT OR IGNORE INTO machines(machine_id, machine_name, "
					+ "width, height, [depth], board_model) "
					+ "VALUES (?, ?, ?, ?, ?, 5)";

	@Parameter("bmp_id")
	@Parameter("machine_id")
	@Parameter("address")
	@Parameter("cabinet")
	@Parameter("frame")
	@GeneratesID
	private static final String INSERT_BMP =
			"INSERT OR IGNORE INTO bmp(bmp_id, machine_id, address, "
					+ "cabinet, frame) VALUES (?, ?, ?, ?, ?)";

	private static void makeMachine(Connection c, int width, int height,
			int depth) {
		try (var u = c.update(INSERT_MACHINE)) {
			u.call(MACHINE, "foo", width, height, depth);
		}
		try (var u = c.update(INSERT_BMP)) {
			u.call(BMP, MACHINE, BMP_ADDR, 1, 1);
		}
	}

	@Parameter("user_id")
	@Parameter("user_name")
	@Parameter("trust_level")
	@Parameter("disabled")
	@GeneratesID
	private static final String INSERT_USER =
			"INSERT OR IGNORE INTO user_info(user_id, user_name, "
					+ "trust_level, disabled, encrypted_password) "
					+ "VALUES (?, ?, ?, ?, '*')";

	@Parameter("group_id")
	@Parameter("group_name")
	@Parameter("quota")
	@GeneratesID
	private static final String INSERT_GROUP = "INSERT OR IGNORE INTO "
			+ "groups(group_id, group_name, quota, group_type) "
			+ "VALUES (?, ?, ?, 0)";

	@Parameter("membership_id")
	@Parameter("user_id")
	@Parameter("group_id")
	@GeneratesID
	private static final String INSERT_MEMBER =
			"INSERT OR IGNORE INTO group_memberships("
					+ "membership_id, user_id, group_id) VALUES (?, ?, ?)";

	private static void makeUser(Connection c) {
		try (var u = c.update(INSERT_USER)) {
			u.call(USER, "bar", BASIC, true);
		}
		try (var u = c.update(INSERT_GROUP)) {
			u.call(GROUP, "grill", INITIAL_QUOTA);
		}
		try (var u = c.update(INSERT_MEMBER)) {
			u.call(MEMBERSHIP, USER, GROUP);
		}
	}

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
	private static final String INSERT_BOARD =
			"INSERT OR IGNORE INTO boards(board_id, address, "
					+ "bmp_id, board_num, machine_id, x, y, z, "
					+ "root_x, root_y, board_power) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	@Parameter("board_1")
	@Parameter("dir_1")
	@Parameter("board_2")
	@Parameter("dir_2")
	@GeneratesID
	private static final String INSERT_LINK =
			"INSERT OR IGNORE INTO links(board_1, dir_1, board_2, dir_2) "
					+ "VALUES (?, ?, ?, ?)";

	/**
	 * Set up a machine with one board, and a user.
	 *
	 * @param c
	 *            Database connection
	 */
	static void setupDB1(Connection c) {
		// A simple machine
		makeMachine(c, 1, 1, 1);
		// Add one board to the machine
		try (var u = c.update(INSERT_BOARD)) {
			u.call(BOARD, BOARD_ADDR, BMP, 0, MACHINE, 0, 0, 0, 0, 0, false);
		}
		// A disabled permission-less user with a quota
		makeUser(c);
	}

	/**
	 * Set up a machine with three boards, and a user.
	 *
	 * @param c
	 *            Database connection
	 */
	static void setupDB3(Connection c) {
		// A simple machine
		makeMachine(c, 1, 1, 3);
		// Add three connected boards to the machine
		int b0 = BOARD, b1 = BOARD + 1, b2 = BOARD + 2;
		try (var u = c.update(INSERT_BOARD)) {
			u.call(b0, BOARD_ADDR, BMP, 0, MACHINE, 0, 0, 0, 0, 0, false);
			u.call(b1, "2.2.2.3", BMP, 1, MACHINE, 0, 0, 1, 8, 4, false);
			u.call(b2, "2.2.2.4", BMP, 2, MACHINE, 0, 0, 2, 4, 8, false);
		}
		try (var u = c.update(INSERT_LINK)) {
			u.call(b0, 0, b1, 3);
			u.call(b0, 1, b2, 4);
			u.call(b1, 2, b2, 5);
		}
		// A disabled permission-less user with a quota
		makeUser(c);
	}

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
	private static final String INSERT_JOB =
			"INSERT INTO jobs(machine_id, owner, group_id, root_id, "
					+ "job_state, create_timestamp, allocation_timestamp, "
					+ "death_timestamp, allocation_size, "
					+ "keepalive_interval, keepalive_timestamp) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * Insert a job of a given size and length.
	 *
	 * @param c
	 *            DB connection
	 * @param root
	 *            Root board, or {@code null}
	 * @param state
	 *            Job state
	 * @param size
	 *            Number of boards, or {@code null}
	 * @param createTime
	 *            Time of creation, or {@code null}
	 * @param allocateTime
	 *            Time of allocation, or {@code null}
	 * @param deathTime
	 *            Time of death, or {@code null}
	 * @param keepalive
	 *            Keepalive interval, or {@code null}
	 * @param keepaliveTime
	 *            Time of last keepalive, or {@code null}
	 * @return Job ID
	 */
	static int makeJob(Connection c, Integer root, @NonNull JobState state,
			Integer size, Instant createTime, Instant allocateTime,
			Instant deathTime, Duration keepalive, Instant keepaliveTime) {
		try (var u = c.update(INSERT_JOB)) {
			return u.key(MACHINE, USER, GROUP, root, state, createTime,
					allocateTime, deathTime, size, keepalive, keepaliveTime)
					.orElseThrow(
							() -> new RuntimeException("failed to insert job"));
		}
	}

}
