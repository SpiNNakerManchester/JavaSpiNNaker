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
package uk.ac.manchester.spinnaker.alloc.db;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK;
import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.function.Executable;
import org.springframework.dao.DataAccessException;
import org.sqlite.SQLiteException;

import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.MemberRecord;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;

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

	/** Not the name of anything. */
	static final String NO_NAME = "gorp";

	/**
	 * The columns needed to make a {@link SpallocAPI.Machine} implementation.
	 */
	static final Set<String> BASIC_MACHINE_INFO =
			set("in_service", "machine_id", "machine_name", "width", "height");

	/** The columns the {@link BoardLocation} constructor expects to find. */
	static final Set<String> BOARD_LOCATION_REQUIRED_COLUMNS =
			set("machine_name", "x", "y", "z", "cabinet", "frame", "board_num",
					"chip_x", "chip_y", "board_chip_x", "board_chip_y",
					"job_id", "job_root_chip_x", "job_root_chip_y");

	/** Columns expected when building {@link BoardState} from a {@link Row}. */
	static final Set<String> MSC_BOARD_COORDS = set("board_id", "x", "y", "z",
			"cabinet", "frame", "board_num", "address", "machine_name");

	/**
	 * Columns expected when building {@link BoardCoords} from a {@link Row}.
	 */
	static final Set<String> BOARD_COORDS_REQUIRED_COLUMNS =
			set("x", "y", "z", "cabinet", "frame", "board_num", "address");

	/**
	 * Columns expected when building a {@link UserRecord} from a {@link Row}.
	 */
	static final Set<String> USER_COLUMNS =
			set("disabled", "has_password", "last_fail_timestamp",
					"last_successful_login_timestamp", "locked", "trust_level",
					"user_id", "user_name", "openid_subject", "is_internal");

	/**
	 * Columns expected when building a {@link GroupRecord} from a {@link Row}.
	 */
	static final Set<String> GROUP_COLUMNS =
			set("group_id", "group_name", "group_type", "quota");

	/**
	 * Columns expected when building a {@link MemberRecord} from a {@link Row}.
	 */
	static final Set<String> MEMBER_COLUMNS = set("group_id", "group_name",
			"user_id", "user_name", "membership_id");

	/** Classes used in Javadoc. Technically not needed, but... */
	static final Class<?>[] JAVADOC_ONLY_CLASSES = {
		BoardState.class, BoardLocation.class, BoardCoords.class,
		UserRecord.class, GroupRecord.class, MemberRecord.class
	};

	private DBTestingUtils() {
	}

	/**
	 * Easy set builder.
	 *
	 * @param strings
	 *            The values in the set.
	 * @return An unmodifiable set.
	 */
	static Set<String> set(String... strings) {
		return unmodifiableSet(new HashSet<>(asList(strings)));
	}

	/**
	 * {@linkplain org.junit.jupiter.api.Assertions Assert} that two sets are
	 * equal by converting them into sorted lists and comparing those. This
	 * produces the most comprehensible results.
	 *
	 * @param <T>
	 *            The type of elements in the sets.
	 * @param expected
	 *            The set of expected elements.
	 * @param actual
	 *            The actual results of the operation.
	 */
	static <T extends Comparable<T>> void assertSetEquals(Set<T> expected,
			Set<T> actual) {
		List<T> e = new ArrayList<>(expected);
		sort(e);
		List<T> a = new ArrayList<>(actual);
		sort(a);
		assertEquals(e, a);
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
		Throwable t = e.getMostSpecificCause();
		assertEquals(SQLiteException.class, t.getClass());
		return (SQLiteException) t;
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
		DataAccessException e = assertThrows(DataAccessException.class, op);
		SQLiteException exn = causedBySQLite(e);
		assertEquals(SQLITE_CONSTRAINT_FOREIGNKEY, exn.getResultCode());
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
		DataAccessException e = assertThrows(DataAccessException.class, op);
		SQLiteException exn = causedBySQLite(e);
		assertEquals(SQLITE_CONSTRAINT_CHECK, exn.getResultCode());
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
