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

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static testconfig.BoardTestConfiguration.NOHOST;
import static uk.ac.manchester.spinnaker.machine.MachineVersion.FIVE;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.software_watchdog_count;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.SYS_VARS;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import net.jcip.annotations.NotThreadSafe;
import testconfig.BoardTestConfiguration;
import uk.ac.manchester.spinnaker.connections.BootConnection;
import uk.ac.manchester.spinnaker.connections.EIEIOConnection;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.MachineVersion;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.VirtualMachine;
import uk.ac.manchester.spinnaker.utils.InetFactory;

@NotThreadSafe
class TestTransceiver {
	private static BoardTestConfiguration boardConfig;

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		boardConfig = new BoardTestConfiguration();
	}

	@Test
	void testCreateNewTransceiverToBoard() throws Exception {
		var connections = new ArrayList<Connection>();

		boardConfig.setUpRemoteBoard();
		connections.add(new SCPConnection(boardConfig.remotehost));

		try (var txrx = new Transceiver(FIVE, connections, null,
				null, null, null, null)) {
			assertEquals(1, txrx.getConnections().size());
		}
	}

	@Test
	void testCreateNewTransceiverOneConnection() throws Exception {
		var connections = new ArrayList<Connection>();

		boardConfig.setUpRemoteBoard();
		connections.add(new SCPConnection(boardConfig.remotehost));

		try (var txrx = new Transceiver(FIVE, connections, null,
				null, null, null, null)) {
			assertEquals(Set.copyOf(connections), txrx.getConnections());
		}
	}

	@Test
	void testCreateNewTransceiverFromListConnections() throws Exception {
		var connections = new ArrayList<Connection>();

		boardConfig.setUpRemoteBoard();
		connections.add(new SCPConnection(boardConfig.remotehost));

		boardConfig.setUpLocalVirtualBoard();
		connections.add(new SCPConnection(boardConfig.remotehost));

		try (var txrx = new Transceiver(FIVE, connections, null,
				null, null, null, null)) {
			for (var c : txrx.getConnections()) {
				assertTrue(connections.contains(c));
			}
			assertEquals(Set.copyOf(connections), txrx.getConnections());
		}
	}

	@Test
	void testRetrievingMachineDetails() throws Exception {
		var connections = new ArrayList<Connection>();

		boardConfig.setUpRemoteBoard();
		connections.add(new SCPConnection(boardConfig.remotehost));

		boardConfig.setUpLocalVirtualBoard();
		connections.add(
				new BootConnection(null, null, boardConfig.remotehost, null));

		try (var txrx = new Transceiver(FIVE, connections, null,
				null, null, null, null)) {
			if (boardConfig.boardVersion.isFourChip) {
				assertEquals(2, txrx.getMachineDimensions().width);
				assertEquals(2, txrx.getMachineDimensions().height);
			} else if (boardConfig.boardVersion.isFourtyeightChip) {
				assertEquals(8, txrx.getMachineDimensions().width);
				assertEquals(8, txrx.getMachineDimensions().height);
			} else {
				var size = txrx.getMachineDimensions();
				fail(format("Unknown board with size %dx%d", size.width,
						size.height));
			}
			assertTrue(txrx.isConnected());
			assertNotNull(txrx.getScampVersion());
			assertNotNull(txrx.getCPUInformation());
		} catch (ProcessException e) {
			if (e.getMessage().contains("Operation CMD_READ timed out")) {
				assumeFalse(true, e.getMessage());
			}
			throw e;
		}
	}

	@Test
	void testBootBoard() throws Exception {
		boardConfig.setUpRemoteBoard();
		try (var txrx = new Transceiver(boardConfig.remotehost,
				boardConfig.boardVersion)) {
			// self.assertFalse(trans.is_connected())
			txrx.bootBoard();
		}
	}

	// Tests the creation of listening sockets.
	@Test
	@Disabled("host reachability; issue #215")
	void testListenerCreation() throws Exception {
		// Create board connections
		var noHost = InetFactory.getByName(NOHOST);
		assumeFalse(ping(noHost) == 0,
				() -> "unreachable host (" + noHost + ") appears to be up");
		var connections = new ArrayList<Connection>();
		connections.add(new SCPConnection(null, (Integer) null, noHost, null));
		var orig = new EIEIOConnection(null, null, null, null);
		connections.add(orig);

		// Create transceiver
		try (var txrx = new Transceiver(FIVE, connections, null,
				null, null, null, null)) {
			int port = orig.getLocalPort();
			// Register a UDP listeners
			var c1 = txrx.registerEIEIOListener(null);
			assertTrue(c1 == orig, "first connection must be original");
			var c2 = txrx.registerEIEIOListener(null);
			assertTrue(c2 == orig, "second connection must be original");
			var c3 = txrx.registerEIEIOListener(null, port);
			assertTrue(c3 == orig, "third connection must be original");
			var c4 = txrx.registerEIEIOListener(null, port + 1);
			assertFalse(c4 == orig, "fourth connection must not be original");
		}
	}

	@Test
	@Disabled("host reachability; issue #215")
	void testSetWatchdog() throws Exception {
		// The expected write values for the watch dog
		var expectedWrites = List.of(new byte[] {
			((Number) software_watchdog_count.getDefault()).byteValue()
		}, new byte[] {
			0
		}, new byte[] {
			5
		});

		var connections = new ArrayList<Connection>();
		var noHost = InetFactory.getByName(NOHOST);
		connections.add(new SCPConnection(noHost));
		try (var txrx = new MockWriteTransceiver(FIVE, connections)) {
			// All chips
			txrx.enableWatchDogTimer(true);
			txrx.enableWatchDogTimer(false);
			txrx.setWatchDogTimeout(5);

			/*
			 * Check the values that were "written" for set_watch_dog, which
			 * should be one per chip
			 */
			int writeItem = 0;
			for (var expectedData : expectedWrites) {
				for (var chip : txrx.getMachineDetails().chipCoordinates()) {
					var write = txrx.writtenMemory.get(writeItem++);
					assertEquals(chip.getScampCore(), write.core);
					assertEquals(SYS_VARS.add(software_watchdog_count.offset),
							write.address);
					assertArrayEquals(expectedData, write.data);
				}
			}
		}
	}

	private static final int REPETITIONS = 10;

	@Test
	@Timeout(120) // Two minutes is enough
	void testReliableMachine() throws Exception {
		boardConfig.setUpRemoteBoard();
		Machine first = null;

		for (int i = 0; i < REPETITIONS; i++) {
			try (var txrx = new Transceiver(boardConfig.remotehost, FIVE)) {
				assertNotNull(txrx.ensureBoardIsReady());
				assertNotNull(txrx.getMachineDimensions());
				assertNotNull(txrx.getScampVersion());
				if (first == null) {
					first = txrx.getMachineDetails();
				} else {
					assertNull(first.difference(txrx.getMachineDetails()));
				}
			} catch (ProcessException e) {
				if (e.getMessage().contains("Operation CMD_VER timed out")) {
					assumeFalse(true, e.getMessage());
				}
				throw e;
			}
		}
	}
}

@SuppressWarnings("checkstyle:JavadocVariable")
class MockWriteTransceiver extends Transceiver {
	static class Write {
		final CoreLocation core;

		final byte[] data;

		final MemoryLocation address;

		final int offset;

		final int numBytes;

		Write(HasCoreLocation core, MemoryLocation baseAddress,
				ByteBuffer data) {
			this.core = core.asCoreLocation();
			this.address = baseAddress;
			this.data = new byte[data.remaining()];
			this.offset = data.position();
			this.numBytes = data.remaining();
			data.get(this.data);
		}
	}

	List<Write> writtenMemory = new ArrayList<>();

	MockWriteTransceiver(MachineVersion version,
			Collection<Connection> connections)
			throws IOException, SpinnmanException,
			uk.ac.manchester.spinnaker.transceiver.ProcessException {
		super(version, connections, null, null, null, null, null);
	}

	@Override
	public Machine getMachineDetails() {
		return new VirtualMachine(new MachineDimensions(2, 2));
	}

	@Override
	void updateMachine() {
		var details = getMachineDetails();
		try {
			var machineField = Transceiver.class.getField("machine");
			machineField.setAccessible(true);
			machineField.set(this, details);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			ByteBuffer data) {
		writtenMemory.add(new Write(core, baseAddress, data));
	}
}
