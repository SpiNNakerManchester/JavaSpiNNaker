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
package uk.ac.manchester.spinnaker.alloc.security;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.USER;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.setupDB1;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.security.LocalAuthProviderImpl.TestAPI;

@SpringBootTest
@SpringJUnitWebConfig(LocalAuthTest.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + LocalAuthTest.DB,
	"spalloc.historical-data.path=" + LocalAuthTest.HIST_DB
})
class LocalAuthTest extends SQLQueries {
	private static final Logger log = getLogger(LocalAuthTest.class);

	/** The name of the database file. */
	static final String DB = "target/sec_test.sqlite3";

	/** The name of the database file. */
	static final String HIST_DB = "target/sec_test-hist.sqlite3";

	@Configuration
	@ComponentScan(basePackageClasses = SpallocProperties.class)
	static class Config {
	}

	@Autowired
	private DatabaseEngine db;

	private TestAPI authEngine;

	@BeforeAll
	static void clearDB() throws IOException {
		Path dbp = Paths.get(DB);
		if (exists(dbp)) {
			log.info("deleting old database: {}", dbp);
			delete(dbp);
		}
	}

	@BeforeEach
	@SuppressWarnings("deprecation")
	void checkSetup(@Autowired LocalAuthenticationProvider<?> authEngine) {
		assumeTrue(db != null, "spring-configured DB engine absent");
		try (Connection c = db.getConnection()) {
			c.transaction(() -> setupDB1(c));
		}
		this.authEngine = (TestAPI) authEngine.getTestAPI();
	}

	// The actual tests

	@Test
	public void unlockUser() throws Exception {
		try (Connection c = db.getConnection()) {
			c.transaction(() -> {
				// 90k seconds is more than one day
				try (Update setLocked =
						c.update("UPDATE user_info SET locked = :locked, "
								+ "last_fail_timestamp = :time - 90000 "
								+ "WHERE user_id = :user_id")) {
					setLocked.call(true, now(), USER);
				}
			});
		}

		authEngine.unlock();

		try (Connection c = db.getConnection()) {
			assertEquals(false, c.transaction(() -> {
				try (Query q = c.query("SELECT locked FROM user_info "
						+ "WHERE user_id = :user_id")) {
					return q.call1(USER).map(Row.bool("locked")).get();
				}
			}));
		}
	}
}
