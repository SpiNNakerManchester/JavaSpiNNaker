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

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK;
import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY;

import java.util.Set;

import org.junit.jupiter.api.function.Executable;
import org.springframework.dao.DataAccessException;
import org.sqlite.SQLiteException;

import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Query;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.MemberRecord;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Miscellaneous shared bits and pieces for testing the database code.
 *
 * @author Donal Fellows
 */
abstract class DBTestingUtils {
	/** Not equal to any {@code machine_id}. */
	static final int NO_MACHINE = -1;

	/** Not equal to any {@code job_id}. */
	static final int NO_JOB = -1;

	/** Not equal to any {@code board_id}. */
	static final int NO_BOARD = -1;

	/** Not equal to any {@code bmp_id}. */
	static final int NO_BMP = -1;

	/** Not equal to any {@code change_id}. */
	static final int NO_CHANGE = -1;

	/** Not equal to any {@code user_id}. */
	static final int NO_USER = -1;

	/** Not equal to any {@code group_id}. */
	static final int NO_GROUP = -1;

	/** Not equal to any {@code membership_id}. */
	static final int NO_MEMBER = -1;

	/** Not equal to any {@code op_id}. */
	static final int NO_BLACKLIST_OP = -1;

	/** Not the name of anything. */
	static final String NO_NAME = "gorp";

	/**
	 * The columns needed to make a {@link SpallocAPI.Machine} implementation.
	 */
	@UsedInJavadocOnly(SpallocAPI.class)
	static final Set<String> BASIC_MACHINE_INFO = Set.of("in_service",
			"machine_id", "machine_name", "width", "height");

	/** The columns the {@link BoardLocation} constructor expects to find. */
	static final Set<String> BOARD_LOCATION_REQUIRED_COLUMNS = Set.of(
			"machine_name", "x", "y", "z", "cabinet", "frame", "board_num",
			"chip_x", "chip_y", "board_chip_x", "board_chip_y", "job_id",
			"job_root_chip_x", "job_root_chip_y");

	/** Columns expected when building {@link BoardState} from a {@link Row}. */
	static final Set<String> MSC_BOARD_COORDS = Set.of("board_id", "x", "y",
			"z", "cabinet", "frame", "board_num", "address", "machine_name",
			"bmp_serial_id", "physical_serial_id");

	/**
	 * Columns expected when building {@link BoardCoords} from a {@link Row}.
	 */
	static final Set<String> BOARD_COORDS_REQUIRED_COLUMNS = Set.of("board_id",
			"x", "y", "z", "cabinet", "frame", "board_num", "address");

	/**
	 * Columns expected when building a {@link UserRecord} from a {@link Row}.
	 */
	static final Set<String> USER_COLUMNS =
			Set.of("disabled", "has_password", "last_fail_timestamp",
					"last_successful_login_timestamp", "locked", "trust_level",
					"user_id", "user_name", "openid_subject", "is_internal");

	/**
	 * Columns expected when building a {@link GroupRecord} from a {@link Row}.
	 */
	static final Set<String> GROUP_COLUMNS =
			Set.of("group_id", "group_name", "group_type", "quota");

	/**
	 * Columns expected when building a {@link MemberRecord} from a {@link Row}.
	 */
	static final Set<String> MEMBER_COLUMNS = Set.of("group_id", "group_name",
			"user_id", "user_name", "membership_id");

	// @formatter:off
	@UsedInJavadocOnly({
		BoardState.class, BoardLocation.class, BoardCoords.class,
		UserRecord.class, GroupRecord.class, MemberRecord.class
	})
	// @formatter:on
	private DBTestingUtils() {
	}

	/**
	 * {@linkplain org.junit.jupiter.api.Assertions Assert} that the result
	 * columns of the query are adequate for making a {@link BoardLocation}
	 * instance.
	 *
	 * @param q
	 *            The query that feeds the creation.
	 */
	static void assertCanMakeBoardLocation(Query q) {
		assertTrue(
				q.getRowColumnNames()
						.containsAll(BOARD_LOCATION_REQUIRED_COLUMNS),
				() -> "board location creation using " + q
						+ " will fail; required columns missing");
	}

	private static SQLiteException causedBySQLite(DataAccessException e) {
		var t = e.getMostSpecificCause();
		assertEquals(SQLiteException.class, t.getClass());
		return (SQLiteException) t;
	}

	private static String generateMessage(String expected,
			SQLiteException got) {
		// Extract the real error message out of SQLite itself
		return format("expected %s failure but got %s", expected,
				got.getMessage().replaceFirst(".*\\((.+)\\)$", "$1"));
	}

	/**
	 * {@linkplain org.junit.jupiter.api.Assertions Assert} that execution of
	 * the supplied executable throws an exception due to a foreign key
	 * constraint failure.
	 *
	 * @param op
	 *            The executable operation being tested.
	 */
	static void assertThrowsFK(Executable op) {
		var e = assertThrows(DataAccessException.class, op);
		var exn = causedBySQLite(e);
		assertEquals(SQLITE_CONSTRAINT_FOREIGNKEY, exn.getResultCode(),
				() -> generateMessage("FK", exn));
	}

	/**
	 * {@linkplain org.junit.jupiter.api.Assertions Assert} that execution of
	 * the supplied executable throws an exception due to a CHECK constraint
	 * failure.
	 *
	 * @param op
	 *            The executable operation being tested.
	 */
	static void assertThrowsCheck(Executable op) {
		var e = assertThrows(DataAccessException.class, op);
		var exn = causedBySQLite(e);
		assertEquals(SQLITE_CONSTRAINT_CHECK, exn.getResultCode(),
				() -> generateMessage("CHECK", exn));
	}

	/**
	 * {@linkplain org.junit.jupiter.api.Assumptions Assume} that the connection
	 * is writable.
	 *
	 * @param conn
	 *            The connection to check.
	 */
	static void assumeWritable(Connection conn) {
		assumeFalse(conn.isReadOnly(), "connection is read-only");
	}
}
