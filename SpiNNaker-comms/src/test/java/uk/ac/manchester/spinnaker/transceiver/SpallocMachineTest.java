/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.transceiver;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static testconfig.BoardTestConfiguration.OWNER;
import static uk.ac.manchester.spinnaker.machine.MachineVersion.FIVE;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

import java.net.InetAddress;
import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.jcip.annotations.NotThreadSafe;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;
import uk.ac.manchester.spinnaker.spalloc.CreateJobBuilder;
import uk.ac.manchester.spinnaker.spalloc.SpallocJob;

@NotThreadSafe
class SpallocMachineTest {
	private static Machine jsonMachine;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		URL url = SpallocMachineTest.class.getResource("/spinn4.json");
		ObjectMapper mapper = MapperFactory.createMapper();
		MachineBean fromJson = mapper.readValue(url, MachineBean.class);
		jsonMachine = new Machine(fromJson);
	}

	private static final String SPALLOC = "spinnaker.cs.man.ac.uk";
	private static final int PORT = 22245;
	private static final int TEN_S = 10000;

	@Test
	@Disabled("https://github.com/SpiNNakerManchester/JavaSpiNNaker/issues/53")
	void testSpallocMachine() throws Exception {
		assumeTrue(ping(SPALLOC) == 0);

		CreateJobBuilder cj = new CreateJobBuilder().owner(OWNER);

		try (SpallocJob job = new SpallocJob(SPALLOC, PORT, TEN_S, cj)) {
			job.waitUntilReady(null);

			System.out.println(job.getState());
			System.out.println(job.getHostname());
			InetAddress host = InetAddress.getByName(job.getHostname());

			// InetAddress host = InetAddress.getByName("spinn-2.cs.man.ac.uk");
			try (Transceiver txrx = new Transceiver(host, FIVE)) {
				txrx.ensureBoardIsReady();
				txrx.getMachineDimensions();
				txrx.getScampVersion();
				Machine machine = txrx.getMachineDetails();
				assertNull(jsonMachine.difference(machine));
			}
		}
	}
}
