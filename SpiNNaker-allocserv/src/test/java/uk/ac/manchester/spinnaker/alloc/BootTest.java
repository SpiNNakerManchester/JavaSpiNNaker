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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.admin.AdminAPI;
import uk.ac.manchester.spinnaker.alloc.admin.AdminController;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.web.SystemController;
import uk.ac.manchester.spinnaker.alloc.web.SpallocServiceAPI;

@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest") // Disable booting CXF
@TestPropertySource(properties = {
	"spalloc.database-path=" + BootTest.DB,
	"spalloc.historical-data.path=" + BootTest.HIST_DB
})
class BootTest extends TestSupport {
	/** The DB file. */
	static final String DB = "target/boot_test.sqlite3";

	/** The DB history file. */
	static final String HIST_DB = "target/boot_test_hist.sqlite3";

	@Autowired
	private SpallocServiceAPI service;

	@Autowired
	private SpallocAPI core;

	@Autowired
	private SystemController root;

	@Autowired
	private AdminAPI admin;

	@Autowired
	private AdminController adminUI;

	@BeforeAll
	static void clearDB() throws IOException {
		killDB(DB);
	}

	@Test
	void testContextBoot() throws InterruptedException {
		// If all these bits are there, we declare the application to be working
		assertNotNull(service);
		assertNotNull(core);
		assertNotNull(db);
		assertNotNull(root);
		assertNotNull(admin);
		assertNotNull(adminUI);
		// Give background tasks a chance to run
		Thread.sleep(5000);
	}
}
