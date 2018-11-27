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

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;
import uk.ac.manchester.spinnaker.spalloc.SpallocJob;

import static uk.ac.manchester.spinnaker.machine.MachineVersion.FIVE;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

class SpallocMachineTest {
	static Machine jsonMachine;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		URL url = SpallocMachineTest.class.getResource("/spinn4.json");
		ObjectMapper mapper = MapperFactory.createMapper();
		MachineBean fromJson = mapper.readValue(url, MachineBean.class);
		jsonMachine = new Machine(fromJson);
	}

	@Test
	@Disabled("https://github.com/SpiNNakerManchester/JavaSpiNNaker/issues/53")
	void testSpallocMachine() throws Exception {
		assumeTrue(ping("spinnaker.cs.man.ac.uk") == 0);

		List<Integer> args = new ArrayList<>();
		Map<String, Object> kwargs = new HashMap<>();
		kwargs.put("owner", "Unittest. OK to kill after 1 minute.");

		try (SpallocJob job = new SpallocJob("spinnaker.cs.man.ac.uk", 22245,
				10000, args, kwargs)) {
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
