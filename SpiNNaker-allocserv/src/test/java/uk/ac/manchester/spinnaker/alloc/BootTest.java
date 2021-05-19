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
package uk.ac.manchester.spinnaker.alloc;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocInterface;
import uk.ac.manchester.spinnaker.alloc.web.SpallocAPI;

@SpringJUnitWebConfig(BootTest.Config.class)
@ActiveProfiles("unittest") // Disable booting CXF
@TestPropertySource(properties = "databasePath=boot_test.sqlite3")
@TestInstance(PER_CLASS)
class BootTest {
	@Configuration
	@ComponentScan
	static class Config {
	};

	@Autowired
	private SpallocAPI service;

	@Autowired
	private SpallocInterface core;

	@Autowired
	private DatabaseEngine db;

	@Value("@{databasePath}")
	private File dbPath;

	@BeforeAll
	void clearDB() {
		dbPath.delete();
	}

	@Test
	@DisplayName("Spring context startup")
	void testContextBoot() {
		assertNotNull(service);
		assertNotNull(core);
		assertNotNull(db);
	}

	@Nested
	@DisplayName("Database tests")
	class DbTest {
		@Test
		void testDbConn() throws SQLException {
			try (Connection c = db.getConnection()) {
				assertFalse(c.isReadOnly());
				int rows = 0;
				try (Query q =
						query(c, "SELECT COUNT(*) FROM board_model_coords")) {
					for (ResultSet row : q.call()) {
						// For v2, v3, v4 and v5 board configs
						assertTrue(row.getInt(1) == 104);
						rows++;
					}
				}
				assertEquals(1, rows, "should be only one row in query result");
			}
		}

		// TODO add tests of the queries
	}
}
