/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.transceiver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static testconfig.BoardTestConfiguration.OWNER;
import static uk.ac.manchester.spinnaker.machine.MachineVersion.FIVE;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

import java.net.InetAddress;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import net.jcip.annotations.NotThreadSafe;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;
import uk.ac.manchester.spinnaker.spalloc.CreateJob;
import uk.ac.manchester.spinnaker.spalloc.SpallocJob;

@NotThreadSafe
class SpallocMachineTest {
	private static Machine jsonMachine;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		var url = SpallocMachineTest.class.getResource("/spinn4.json");
		var mapper = MapperFactory.createMapper();
		var fromJson = mapper.readValue(url, MachineBean.class);
		jsonMachine = new Machine(fromJson);
	}

	private static final String SPALLOC = "spinnaker.cs.man.ac.uk";

	private static final int PORT = 22245;

	private static final int TEN_S = 10000;

	@Test
	@Timeout(120) // Two minutes is enough
	@Disabled("https://github.com/SpiNNakerManchester/JavaSpiNNaker/issues/53")
	void testSpallocMachine() throws Exception {
		assumeTrue(ping(SPALLOC) == 0);

		try (var job = new SpallocJob(SPALLOC, PORT, TEN_S,
				new CreateJob().owner(OWNER))) {
			job.waitUntilReady(null);

			System.out.println(job.getState());
			System.out.println(job.getHostname());
			var host = InetAddress.getByName(job.getHostname());

			// InetAddress host = InetAddress.getByName("spinn-2.cs.man.ac.uk");
			try (var txrx = new Transceiver(host, FIVE)) {
				assertNotNull(txrx.ensureBoardIsReady());
				assertNotNull(txrx.getMachineDimensions());
				assertNotNull(txrx.getScampVersion());
				var machine = txrx.getMachineDetails();
				assertNull(jsonMachine.difference(machine));
			}
		}
	}
}
