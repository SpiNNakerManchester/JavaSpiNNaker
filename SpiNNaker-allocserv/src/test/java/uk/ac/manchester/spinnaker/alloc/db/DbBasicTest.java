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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assumeWritable;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.Row.instant;
import static uk.ac.manchester.spinnaker.alloc.db.Row.object;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.context.ActiveProfiles;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;

/**
 * Test that the database engine interface works and that the queries are
 * synchronised with the schema. Deliberately does not do meaningful testing of
 * the data in the database.
 *
 * @author Donal Fellows
 */
@SpringBootTest
@TestInstance(PER_CLASS)
@ActiveProfiles("unittest")
class DbBasicTest extends SimpleDBTestBase {
	private static void assertContains(String expected, String value) {
		assertTrue(value.contains(expected),
				() -> "did not find " + expected + " in " + value);
	}

	/** A very simple query. Note the space at the end is deliberate. */
	private static final String COUNT =
			"SELECT COUNT(*) AS c FROM board_model_coords ";

	@Test
	void testDbConn() {
		try (var q = c.query(COUNT)) {
			c.transaction(() -> {
				int rows = 0;
				for (var row : q.call(integer("c"))) {
					// For v2, v3, v4 and v5 board configs (
					assertEquals(104, row);
					rows++;
				}
				assertEquals(1, rows, "should be only one row in query result");
			});
		}

		// Not supported by SQLite
		assertThrows(Exception.class,
				() -> ((java.sql.Connection) c).createSQLXML());
	}

	@Test
	void testBadQueries() {
		c.transaction(() -> {
			try (var q0 = c.query(COUNT);
					var q1 = c.query(COUNT + "WHERE model = :model")) {

				// Too many args
				assertThrows(TransientDataAccessResourceException.class,
						() -> q0.call1((row) -> null, 1));

				// Not enough args
				assertThrows(BadSqlGrammarException.class,
						() -> q1.call1((row) -> null));

				// No column to fetch
				assertThrows(UncategorizedSQLException.class,
						() -> q0.call1(instant("d")).orElseThrow());
			}

			// No column in query definition
		    assertThrows(BadSqlGrammarException.class,
					() -> c.query(COUNT + "WHERE job_id = ?").call(r -> 1));
		});

		// Accessing row after it has been disposed of
		var row = c.transaction(() -> {
			try (var q = c.query(COUNT)) {
				var r = q.call1((theRow) -> theRow).orElseThrow();
				assertEquals("Row(...)", r.toString());
				return r;
			}
		});
		assertNotNull(row.toString()); // Must not throw or return null!
		var e = assertThrows(DataAccessException.class, () -> row.getInt("c"));
		assertContains("closed", e.getMessage());
	}

	@Test
	void testBadUpdates() {
		// Not very good SQL for updates, but we won't actually run them
		c.transaction(() -> {
			try (var q0 = c.update(COUNT);
					var q1 = c.update(COUNT + "WHERE model = :model")) {

				// Too many args
				assertThrows(TransientDataAccessResourceException.class,
						() -> q0.call(1));

				// Not enough args
				assertThrows(DataIntegrityViolationException.class,
						() -> q1.call());
			}

			// No column in query definition
			assertThrows(DataIntegrityViolationException.class,
					() -> c.update(COUNT + "WHERE job_id = ?").call());
		});
	}

	@Test
	@SuppressWarnings("deprecation")
	void testDbChanges() {
		assumeWritable(c);
		c.transaction(() -> {
			int rows;
			((Connection) c).update(
					"CREATE TEMPORARY TABLE foo "
					+ "(k INT PRIMARY KEY AUTO_INCREMENT, x INT)").call();
			try (var u = c.update("INSERT INTO foo(x) VALUES(?)");
					var q = c.query("SELECT x FROM foo WHERE ? = ?");
					var q2 = c.query("SELECT x FROM foo")) {
				rows = 0;
				for (var row : q.call(object("x"), 1, 1)) {
					assertNotNull(row);
					rows++;
				}
				assertEquals(0, rows);

				var key = u.key(123).orElseThrow();
				assertEquals(Integer.valueOf(1), key);

				rows = 0;
				for (var row : q.call(integer("x"), 1, 1)) {
					assertEquals(123, row);
					rows++;
				}
				assertEquals(1, rows);

				// Check that it works with the row-extraction mapping too
				rows = 0;
				for (var row : q.call(integer("x"), 1, 1)) {
					assertEquals(123, row);
					rows++;
				}
				assertEquals(1, rows);

				// Test what happens when we give too many arguments
				assertThrows(DataAccessException.class,
						() -> q.call((row) -> null, 1, 2, 3));

				assertThrows(DataAccessException.class,
						() -> q2.call((row) -> null, 1));

				// Test what happens when we give too few arguments
				assertThrows(DataAccessException.class,
						() -> q.call((row) -> null, 1));
			}
		});
	}
}
