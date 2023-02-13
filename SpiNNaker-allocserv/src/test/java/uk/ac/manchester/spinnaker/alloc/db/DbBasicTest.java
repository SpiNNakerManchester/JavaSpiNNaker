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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assumeWritable;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.test.context.ActiveProfiles;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.ConnectionImpl;

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
class DbBasicTest extends MemDBTestBase {
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
				for (var row : q.call()) {
					// For v2, v3, v4 and v5 board configs (
					assertEquals(104, row.getInt("c"));
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
			Exception e;
			try (var q0 = c.query(COUNT);
					var q1 = c.query(COUNT + "WHERE model = :model")) {

				// Too many args
				e = assertThrows(InvalidDataAccessResourceUsageException.class,
						() -> q0.call1(1));
				assertEquals("prepared statement takes no arguments",
						e.getMessage());

				// Not enough args
				e = assertThrows(InvalidDataAccessResourceUsageException.class,
						() -> q1.call1());
				assertEquals("prepared statement takes 1 arguments, not 0",
						e.getMessage());

				// No column to fetch
				e = assertThrows(InvalidResultSetAccessException.class,
						() -> q0.call1().orElseThrow().getInstant("d"));
				assertContains("no such column: 'd'", e.getMessage());
			}

			// No column in query definition
			e = assertThrows(BadSqlGrammarException.class,
					() -> c.query(COUNT + "WHERE job_id = ?"));
			assertContains("no such column: job_id", e.getMessage());
		});

		// Accessing row after it has been disposed of
		var row = c.transaction(() -> {
			try (var q = c.query(COUNT)) {
				var r = q.call1().orElseThrow();
				assertContains("Row(c:", r.toString());
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
			Exception e;
			try (var q0 = c.update(COUNT);
					var q1 = c.update(COUNT + "WHERE model = :model")) {

				// Too many args
				e = assertThrows(InvalidDataAccessResourceUsageException.class,
						() -> q0.call(1));
				assertEquals("prepared statement takes no arguments",
						e.getMessage());

				// Not enough args
				e = assertThrows(InvalidDataAccessResourceUsageException.class,
						() -> q1.call());
				assertEquals("prepared statement takes 1 arguments, not 0",
						e.getMessage());
			}

			// No column in query definition
			e = assertThrows(BadSqlGrammarException.class,
					() -> c.update(COUNT + "WHERE job_id = ?"));
			assertContains("no such column: job_id", e.getMessage());
		});
	}

	@Test
	@SuppressWarnings("deprecation")
	void testDbChanges() {
		assumeWritable(c);
		c.transaction(() -> {
			int rows;
			((ConnectionImpl) c).exec("CREATE TEMPORARY TABLE foo(x)");
			try (var u = c.update("INSERT INTO foo(x) VALUES(?)");
					var q = c.query("SELECT x FROM foo WHERE ? = ?");
					var q2 = c.query("SELECT x FROM foo")) {
				rows = 0;
				for (var row : q.call(1, 1)) {
					assertNotNull(row.getObject("x"));
					rows++;
				}
				assertEquals(0, rows);

				int keyCount = 0;
				for (var key : u.keys(123)) {
					// Tricky: assumes how SQLite generates keys
					assertEquals(Integer.valueOf(1), key);
					keyCount++;
				}
				assertEquals(1, keyCount);

				rows = 0;
				for (var row : q.call(1, 1)) {
					assertEquals(123, row.getInt("x"));
					rows++;
				}
				assertEquals(1, rows);

				// Check that it works with the row-extraction mapping too
				rows = 0;
				for (var row : q.call(1, 1).map(integer("x"))) {
					assertEquals(123, row);
					rows++;
				}
				assertEquals(1, rows);

				// Test what happens when we give too many arguments
				var e1 = assertThrows(DataAccessException.class,
						() -> q.call(1, 2, 3));
				assertEquals("prepared statement takes 2 arguments, not 3",
						e1.getMostSpecificCause().getMessage());

				var e2 = assertThrows(DataAccessException.class,
						() -> q2.call(1));
				assertEquals("prepared statement takes no arguments",
						e2.getMostSpecificCause().getMessage());

				// Test what happens when we give too few arguments
				var e3 = assertThrows(DataAccessException.class,
						() -> q.call(1));
				assertEquals("prepared statement takes 2 arguments, not 1",
						e3.getMostSpecificCause().getMessage());
			}
		});
	}
}
