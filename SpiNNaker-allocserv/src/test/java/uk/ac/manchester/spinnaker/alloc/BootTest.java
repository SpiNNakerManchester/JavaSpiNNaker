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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.admin.AdminAPI;
import uk.ac.manchester.spinnaker.alloc.admin.AdminController;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.web.RootController;
import uk.ac.manchester.spinnaker.alloc.web.SpallocServiceAPI;

@SpringBootTest
@SpringJUnitWebConfig(BootTest.Config.class)
@ActiveProfiles("unittest") // Disable booting CXF
@TestPropertySource(properties = {
	"spalloc.database-path=" + BootTest.DB
})
class BootTest {
	private static final Logger log = getLogger(BootTest.class);

	/** The DB file. */
	static final String DB = "boot_test.sqlite3";

	@Configuration
	@ComponentScan
	static class Config {
	}

	@Autowired
	private SpallocServiceAPI service;

	@Autowired
	private SpallocAPI core;

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private RootController root;

	@Autowired
	private AdminAPI admin;

	@Autowired
	private AdminController adminUI;

	@BeforeAll
	static void clearDB() throws IOException {
		Path dbp = Paths.get(DB);
		if (exists(dbp)) {
			log.info("deleting old database: {}", dbp);
			delete(dbp);
		}
	}

	@Test
	void testContextBoot() {
		// If all these bits are there, we declare the application to be working
		assertNotNull(service);
		assertNotNull(core);
		assertNotNull(db);
		assertNotNull(root);
		assertNotNull(admin);
		assertNotNull(adminUI);
	}
}
