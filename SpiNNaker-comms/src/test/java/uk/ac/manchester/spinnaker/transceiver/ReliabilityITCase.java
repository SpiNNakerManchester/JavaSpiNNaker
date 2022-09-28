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

import static java.net.InetAddress.getByName;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.machine.MachineVersion.FIVE;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

import net.jcip.annotations.NotThreadSafe;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;

@Tag("integration")
@NotThreadSafe
class ReliabilityITCase {
	private static Machine jsonMachine;

	private static final Logger log = getLogger(ReliabilityITCase.class);

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		var url = ReliabilityITCase.class.getResource("/spinn4.json");
		var mapper = MapperFactory.createMapper();
		var fromJson = mapper.readValue(url, MachineBean.class);
		jsonMachine = new Machine(fromJson);
	}

	private static final int REPETITIONS = 8;

	@Test
	@Timeout(120) // Two minutes is enough
	void testReliableMachine() throws Exception {
		var host = getByName("spinn-4.cs.man.ac.uk");
		assumeTrue(ping(host) == 0);

		for (int i = 0; i < REPETITIONS; i++) {
			try (var txrx = new Transceiver(host, FIVE)) {
				assertNotNull(txrx.ensureBoardIsReady());
				assertNotNull(txrx.getMachineDimensions());
				assertNotNull(txrx.getScampVersion());
				var machine = txrx.getMachineDetails();
				assertNull(jsonMachine.difference(machine));
			} catch (ProcessException e) {
				if (e.getCause() instanceof SocketTimeoutException) {
					log.info("ignoring timeout from " + e.getCause());
				} else {
					throw e;
				}
			}
		}
	}
}
