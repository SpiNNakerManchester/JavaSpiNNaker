/*
 * Copyright (c) 2018 The University of Manchester
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
