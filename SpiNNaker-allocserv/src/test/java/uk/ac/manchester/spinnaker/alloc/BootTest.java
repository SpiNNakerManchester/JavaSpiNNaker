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

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.web.SpallocServiceAPI;

@SpringBootTest
@SpringJUnitWebConfig(BootTest.Config.class)
@ActiveProfiles("unittest") // Disable booting CXF
@TestPropertySource(properties = "databasePath=boot_test.sqlite3")
@TestInstance(PER_CLASS)
class BootTest {
	private static final Logger log = getLogger(BootTest.class);

	@Configuration
	@ComponentScan
	static class Config {
	};

	@Autowired
	private SpallocServiceAPI service;

	@Autowired
	private SpallocAPI core;

	@Autowired
	private DatabaseEngine db;

	@BeforeAll
	void clearDB() throws IOException {
		Path dbp = db.getDatabasePath();
		if (exists(dbp)) {
			log.info("deleting old database: {}", dbp);
			delete(dbp);
		}
	}

	@Test
	@DisplayName("Spring context startup")
	void testContextBoot() {
		assertNotNull(service);
		assertNotNull(core);
		assertNotNull(db);
	}
}
