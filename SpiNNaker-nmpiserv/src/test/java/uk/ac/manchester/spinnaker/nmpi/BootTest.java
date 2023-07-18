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
package uk.ac.manchester.spinnaker.nmpi;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.nmpi.jobmanager.JobExecuterFactory;
import uk.ac.manchester.spinnaker.nmpi.jobmanager.JobManager;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.MachineManager;
import uk.ac.manchester.spinnaker.nmpi.nmpi.NMPIQueueManager;
import uk.ac.manchester.spinnaker.nmpi.rest.OutputManager;
import uk.ac.manchester.spinnaker.nmpi.web.WebApplicationConfig;

@SpringBootTest
@SpringJUnitWebConfig(WebApplicationConfig.class)
@TestPropertySource("classpath:nmpi.properties")
@ActiveProfiles("unittest") // Disable booting CXF
class BootTest extends TestSupport {

	@Autowired
	private JobManager jobManager;

	@Autowired
	private MachineManager machineManager;

	@Autowired
	private OutputManager outputManager;

	@Autowired
	private JobExecuterFactory jobExecuterFactory;

	@Autowired
	private NMPIQueueManager nmpiQueueManager;

	@Test
	void testContextBoot() throws InterruptedException {
		// If all these bits are there, we declare the application to be working
		assertNotNull(jobManager);
		assertNotNull(machineManager);
		assertNotNull(outputManager);
		assertNotNull(jobExecuterFactory);
		assertNotNull(nmpiQueueManager);
	}
}
