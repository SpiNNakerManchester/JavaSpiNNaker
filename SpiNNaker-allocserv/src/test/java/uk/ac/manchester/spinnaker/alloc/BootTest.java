/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.admin.AdminAPI;
import uk.ac.manchester.spinnaker.alloc.admin.AdminController;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.web.SystemController;
import uk.ac.manchester.spinnaker.alloc.web.SpallocServiceAPI;

@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest") // Disable booting CXF
class BootTest extends TestSupport {

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
