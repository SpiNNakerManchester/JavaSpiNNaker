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
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assumeWritable;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.test.context.ActiveProfiles;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;

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
class DbBasicTest {
	private DatabaseEngine memdb;

	private Connection c;

	@BeforeAll
	void getMemoryDatabase(@Autowired DatabaseEngine mainDBEngine) {
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

	private static void assertContains(String expected, String value) {
		assertTrue(value.contains(expected),
				() -> "did not find " + expected + " in " + value);
	}

	/** A very simple query. Note the space at the end. */
	private static final String COUNT =
			"SELECT COUNT(*) AS c FROM board_model_coords ";

	@Test
	void testDbConn() {
		try (Query q = c.query(COUNT)) {
			c.transaction(() -> {
				int rows = 0;
				for (Row row : q.call()) {
					// For v2, v3, v4 and v5 board configs (
					assertEquals(104, row.getInt("c"));
					rows++;
				}
				assertEquals(1, rows, "should be only one row in query result");
			});
		}

		// Not supported by SQLite
		assertThrows(Exception.class, () -> c.createSQLXML());
	}

	@Test
	void testBadQueries() {
		c.transaction(() -> {
			Exception e;
			try (Query q0 = c.query(COUNT);
					Query q1 = c.query(COUNT + "WHERE model = :model")) {

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
						() -> q0.call1().get().getInstant("d"));
				assertContains("no such column: 'd'", e.getMessage());
			}

			// No column in query definition
			e = assertThrows(BadSqlGrammarException.class,
					() -> c.query(COUNT + "WHERE job_id = ?"));
			assertContains("no such column: job_id", e.getMessage());
		});

		// Accessing row after it has been disposed of
		Row row = c.transaction(() -> {
			try (Query q = c.query(COUNT)) {
				Row r = q.call1().get();
				assertContains("Row(c:", r.toString());
				return r;
			}
		});
		assertNotNull(row.toString()); // Must not throw or return null!
		Exception e = assertThrows(Exception.class, () -> row.getInt("c"));
		assertContains("closed", e.getMessage());
	}

	@Test
	void testBadUpdates() {
		// Not very good SQL for updates, but we won't actually run them
		c.transaction(() -> {
			Exception e;
			try (Update q0 = c.update(COUNT);
					Update q1 = c.update(COUNT + "WHERE model = :model")) {

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
	void testDbChanges() {
		assumeWritable(c);
		c.transaction(() -> {
			int rows;
			c.exec("CREATE TEMPORARY TABLE foo(x)");
			try (Update u = c.update("INSERT INTO foo(x) VALUES(?)");
					Query q = c.query("SELECT x FROM foo WHERE ? = ?");
					Query q2 = c.query("SELECT x FROM foo")) {
				rows = 0;
				for (Row row : q.call(1, 1)) {
					assertNotNull(row.getObject("x"));
					rows++;
				}
				assertEquals(0, rows);

				int keyCount = 0;
				for (Integer key : u.keys(123)) {
					// Tricky: assumes how SQLite generates keys
					assertEquals(Integer.valueOf(1), key);
					keyCount++;
				}
				assertEquals(1, keyCount);

				rows = 0;
				for (Row row : q.call(1, 1)) {
					assertEquals(123, row.getInt("x"));
					rows++;
				}
				assertEquals(1, rows);

				// Check that it works with the row-extraction mapping too
				rows = 0;
				for (Integer row : q.call(1, 1).map(integer("x"))) {
					assertEquals(123, row);
					rows++;
				}
				assertEquals(1, rows);

				// Test what happens when we give too many arguments
				DataAccessException e1 = assertThrows(DataAccessException.class,
						() -> q.call(1, 2, 3));
				assertEquals("prepared statement takes 2 arguments, not 3",
						e1.getMostSpecificCause().getMessage());

				DataAccessException e2 = assertThrows(DataAccessException.class,
						() -> q2.call(1));
				assertEquals("prepared statement takes no arguments",
						e2.getMostSpecificCause().getMessage());

				// Test what happens when we give too few arguments
				DataAccessException e3 = assertThrows(DataAccessException.class,
						() -> q.call(1));
				assertEquals("prepared statement takes 2 arguments, not 1",
						e3.getMostSpecificCause().getMessage());
			}
		});
	}
}