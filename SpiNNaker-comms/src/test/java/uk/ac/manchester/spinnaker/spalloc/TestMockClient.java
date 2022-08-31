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
package uk.ac.manchester.spinnaker.spalloc;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.JobsChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.Machine;
import uk.ac.manchester.spinnaker.spalloc.messages.Notification;
import uk.ac.manchester.spinnaker.spalloc.messages.State;

/**
 * Tests the Spalloc Client ideally using an actual connection but with a backup
 * of canned replies if the connection fails/times out.
 *
 * @author Christian
 */
@SuppressWarnings("unused")
public class TestMockClient {
	private static final int SECOND = 1000;

	private static int timeout = SECOND;

	private static MockConnectedClient client =
			new MockConnectedClient(timeout);

	private static final Logger log = getLogger(TestMockClient.class);

	@Test
	void testListJobs() throws IOException, SpallocServerException, Exception {
		try (var c = client.withConnection()) {
			var jobs = client.listJobs(timeout);
			if (client.isActual()) {
				// Don't know the jobids currently on the machine if any
				jobs.forEach(d -> assertThat("Jobid > 0", d.getJobID(),
						greaterThan(0)));
				jobs.forEach(j -> System.out.println(j));
			} else {
				int[] expectedIDs = {
					47224, 47444
				};
				assertArrayEquals(expectedIDs,
						jobs.stream().mapToInt(j -> j.getJobID()).toArray());
			}
		}
	}

	void testListMachines()
			throws IOException, SpallocServerException, Exception {
		try (var c = client.withConnection()) {
			var machines = client.listMachines(timeout);
			if (client.isActual()) {
				// Don't know the jobids currently on the machine if any
				machines.forEach(m -> assertNotNull(m.getName()));
			} else {
				String[] expectedNames = {
					"Spin24b-223", "Spin24b-225", "Spin24b-226"
				};
				var foundNames = machines.stream()
						.map(Machine::getName).collect(Collectors.toList());
				assertArrayEquals(expectedNames, foundNames.toArray());
			}
			if (!machines.isEmpty()) {
				var machineName = machines.get(0).getName();
				var coords = new BoardCoordinates(0, 0, 1);
				var physical =
						client.getBoardPosition(machineName, coords, timeout);
				var coords2 =
						client.getBoardPosition(machineName, physical, timeout);
				assertEquals(coords, coords2);
				boolean previous = client.isActual();
				var whereis1 = client.whereIs(machineName, coords, timeout);
				var whereis2 = client.whereIs(machineName, physical, timeout);
				var chip = whereis1.getChip();
				var whereis3 = client.whereIs(machineName, chip, timeout);
				// check only work if all real or all mock
				if (previous == client.isActual()) {
					assertEquals(whereis1, whereis2);
					assertEquals(whereis1, whereis3);
				}
			}
		}
	}

	private void checkNotification(int jobId, String machineName) {
		// TODO
	}

	@Test
	void testJob() throws IOException, SpallocServerException, Exception {
		Notification notification = null;
		try (var c = client.withConnection()) {
			var args = new ArrayList<Integer>();
			@SuppressWarnings("deprecation")
			int jobId = client.createJob(args,
					Map.of("owner", "Unittest. OK to kill after 1 minute."),
					timeout);
			if (client.isActual()) {
				assertThat("Jobid > 0", jobId, greaterThan(0));
			} else {
				assertEquals(MockConnectedClient.MOCK_ID, jobId);
			}
			client.notifyJob(jobId, true, timeout);
			var state = client.getJobState(jobId, timeout);
			int retries = 0;
			while (client.isActual() && state.getState() == State.QUEUED) {
				retries += 1;
				if (retries > 2) {
					log.warn("Test Aborted as Spalloc busy");
					client.destroyJob(jobId, "Too long to wait in unittests");
					assumeTrue(false, () -> "Spalloc busy skipping test");
				}
				sleep(timeout);
				state = client.getJobState(jobId, timeout);
			}
			assertEquals(State.POWER, state.getState());
			assertTrue(state.getPower());
			var machineInfo = client.getJobMachineInfo(jobId, timeout);
			var machineName = machineInfo.getMachineName();
			if (client.isActual()) {
				assert !machineName.isBlank() : "must have a machine name";
			} else {
				assertEquals("Spin24b-223", machineName);
			}
			var connections = machineInfo.getConnections();
			var hostName = connections.get(0).getHostname();
			if (client.isActual()) {
				InetAddress.getAllByName(hostName);
			} else {
				assertEquals("10.11.223.33", hostName);
			}
			if (client.isActual()) {
				client.jobKeepAlive(jobId, timeout);
				client.powerOffJobBoards(jobId, timeout);
				notification = client.waitForNotification(timeout);
				state = client.getJobState(jobId, timeout);
			}
			if (client.isActual()) {
				notification = client.waitForNotification(-1);
				JobsChangedNotification.class.cast(notification);
			}
			assertEquals(State.POWER, state.getState());
			if (client.isActual()) {
				assertFalse(state.getPower());
			}
			client.powerOnJobBoards(jobId, timeout);
			state = client.getJobState(jobId, timeout);
			assertEquals(State.POWER, state.getState());
			assertTrue(state.getPower());
			var chip = new ChipLocation(1, 1);
			var whereis = client.whereIs(jobId, chip, timeout);
			assertEquals(chip, whereis.getJobChip());
			assertEquals(jobId, whereis.getJobId());
			if (client.isActual()) {
				assertNotNull(whereis.getBoardChip());
			} else {
				assertEquals(MockConnectedClient.MOCK_ID, whereis.getJobId());
				assertEquals(chip, whereis.getBoardChip());
			}
			client.destroyJob(jobId, "Test finished", timeout);
			state = client.getJobState(jobId, timeout);
			if (client.isActual()) {
				assertEquals(State.DESTROYED, state.getState());
			}
		}
	}

	@Test
	void testVersion() throws IOException, SpallocServerException, Exception {
		try (var c = client.withConnection()) {
			var version = client.version(timeout);
			if (client.isActual()) {
				// TODO: Something here!
				assertThat("version is meaningful", version.majorVersion,
						greaterThan(0));
			} else {
				assertNotNull(version);
			}
		}
	}
}
