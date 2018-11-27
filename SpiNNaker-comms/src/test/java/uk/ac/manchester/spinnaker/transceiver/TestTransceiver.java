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
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static testconfig.BoardTestConfiguration.NOHOST;
import static uk.ac.manchester.spinnaker.machine.MachineVersion.FIVE;
import static uk.ac.manchester.spinnaker.messages.Constants.SYSTEM_VARIABLE_BASE_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.software_watchdog_count;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import testconfig.BoardTestConfiguration;
import uk.ac.manchester.spinnaker.connections.BootConnection;
import uk.ac.manchester.spinnaker.connections.EIEIOConnection;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.MachineVersion;
import uk.ac.manchester.spinnaker.machine.VirtualMachine;
import uk.ac.manchester.spinnaker.transceiver.UDPTransceiver.ConnectionFactory;
import uk.ac.manchester.spinnaker.utils.InetFactory;

class TestTransceiver {
	static BoardTestConfiguration boardConfig;

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		boardConfig = new BoardTestConfiguration();
	}

	@Test
	void testCreateNewTransceiverToBoard() throws Exception {
		List<Connection> connections = new ArrayList<>();

		boardConfig.setUpRemoteBoard();
		connections.add(new SCPConnection(boardConfig.remotehost));

		try (Transceiver txrx = new Transceiver(FIVE, connections, null, null,
				null, null, null, null)) {
			assertEquals(1, txrx.getConnections().size());
		}
	}

	@Test
	void testCreateNewTransceiverOneConnection() throws Exception {
		List<Connection> connections = new ArrayList<>();

		boardConfig.setUpRemoteBoard();
		connections.add(new SCPConnection(boardConfig.remotehost));

		try (Transceiver txrx = new Transceiver(FIVE, connections, null, null,
				null, null, null, null)) {
			assertEquals(new HashSet<>(connections), txrx.getConnections());
		}
	}

	@Test
	void testCreateNewTransceiverFromListConnections() throws Exception {
		List<Connection> connections = new ArrayList<>();

		boardConfig.setUpRemoteBoard();
		connections.add(new SCPConnection(boardConfig.remotehost));

		boardConfig.setUpLocalVirtualBoard();
		connections.add(new SCPConnection(boardConfig.remotehost));

		try (Transceiver txrx = new Transceiver(FIVE, connections, null, null,
				null, null, null, null)) {
			for (Connection c : txrx.getConnections()) {
				assertTrue(connections.contains(c));
			}
			assertEquals(new HashSet<>(connections), txrx.getConnections());
		}
	}

	@Test
	void testRetrievingMachineDetails() throws Exception {
		List<Connection> connections = new ArrayList<>();

		boardConfig.setUpRemoteBoard();
		connections.add(new SCPConnection(boardConfig.remotehost));

		boardConfig.setUpLocalVirtualBoard();
		connections.add(
				new BootConnection(null, null, boardConfig.remotehost, null));

		try (Transceiver txrx = new Transceiver(FIVE, connections, null, null,
				null, null, null, null)) {
			if (boardConfig.boardVersion.isFourChip) {
				assertEquals(2, txrx.getMachineDimensions().width);
				assertEquals(2, txrx.getMachineDimensions().height);
			} else if (boardConfig.boardVersion.isFourtyeightChip) {
				assertEquals(8, txrx.getMachineDimensions().width);
				assertEquals(8, txrx.getMachineDimensions().height);
			} else {
				MachineDimensions size = txrx.getMachineDimensions();
				fail(format("Unknown board with size %dx%d", size.width,
						size.height));
			}
			assertTrue(txrx.isConnected());
			assertNotNull(txrx.getScampVersion());
			assertNotNull(txrx.getCPUInformation());
		}
	}

	@Test
	void testBootBoard() throws Exception {
		boardConfig.setUpRemoteBoard();
		try (Transceiver txrx = new Transceiver(boardConfig.remotehost,
				boardConfig.boardVersion)) {
			// self.assertFalse(trans.is_connected())
			txrx.bootBoard();
		}
	}

	/** Tests the creation of listening sockets. */
	@Test
	@Disabled("CB commented out")
	void testListenerCreation() throws Exception {
		// Create board connections
		List<Connection> connections = new ArrayList<>();
		Inet4Address noHost = InetFactory.getByName(NOHOST);
		connections.add(new SCPConnection(null, (Integer) null, noHost, null));
		EIEIOConnection orig = new EIEIOConnection(null, null, null, null);
		connections.add(orig);

		// Create transceiver
		try (Transceiver txrx = new Transceiver(FIVE, connections, null, null,
				null, null, null, null)) {
			int port = orig.getLocalPort();
			EIEIOConnectionFactory cf = new EIEIOConnectionFactory();
			// Register a UDP listeners
			Connection c1 = txrx.registerUDPListener(null, cf);
			assertTrue(c1 == orig, "first connection must be original");
			Connection c2 = txrx.registerUDPListener(null, cf);
			assertTrue(c2 == orig, "second connection must be original");
			Connection c3 = txrx.registerUDPListener(null, cf, port);
			assertTrue(c3 == orig, "third connection must be original");
			Connection c4 = txrx.registerUDPListener(null, cf, port + 1);
			assertFalse(c4 == orig, "fourth connection must not be original");
		}
	}

	@Test
	// @Disabled("CB commented out")
	void testSetWatchdog() throws Exception {
		// The expected write values for the watch dog
		List<byte[]> expectedWrites = asList(new byte[] {
				((Number) software_watchdog_count.getDefault()).byteValue()
		}, new byte[] {
				0
		}, new byte[] {
				5
		});

		List<Connection> connections = new ArrayList<>();
		Inet4Address noHost = InetFactory.getByName(NOHOST);
		connections.add(new SCPConnection(noHost));
		try (MockWriteTransceiver txrx =
				new MockWriteTransceiver(FIVE, connections)) {
			// All chips
			txrx.enableWatchDogTimer(true);
			txrx.enableWatchDogTimer(false);
			txrx.setWatchDogTimeout(5);

			/*
			 * Check the values that were "written" for set_watch_dog, which
			 * should be one per chip
			 */
			int writeItem = 0;
			for (byte[] expectedData : expectedWrites) {
				for (ChipLocation chip : txrx.getMachineDetails()
						.chipCoordinates()) {
					MockWriteTransceiver.Write write =
							txrx.writtenMemory.get(writeItem++);
					assertEquals(chip.getScampCore(), write.core);
					assertEquals(
							SYSTEM_VARIABLE_BASE_ADDRESS
									+ software_watchdog_count.offset,
							write.address);
					assertArrayEquals(expectedData, write.data);
				}
			}
		}
	}

	private static final int REPETITIONS = 10;

	@Test
	void testReliableMachine() throws Exception {
		boardConfig.setUpRemoteBoard();
		Machine first = null;

		for (int i = 0; i < REPETITIONS; i++) {
			try (Transceiver txrx =
					new Transceiver(boardConfig.remotehost, FIVE)) {
				txrx.ensureBoardIsReady();
				txrx.getMachineDimensions();
				txrx.getScampVersion();
				if (first == null) {
					first = txrx.getMachineDetails();
				} else {
					assertNull(first.difference(txrx.getMachineDetails()));
				}
			}
		}
	}
}

class MockWriteTransceiver extends Transceiver {
	static class Write {
		final CoreLocation core;
		final byte[] data;
		final int address;
		final int offset;
		final int numBytes;

		Write(HasCoreLocation core, int baseAddress, ByteBuffer data) {
			this.core = core.asCoreLocation();
			this.address = baseAddress;
			this.data = data.array().clone();
			this.offset = data.position();
			this.numBytes = data.remaining();
		}
	}

	List<Write> writtenMemory = new ArrayList<>();

	public MockWriteTransceiver(MachineVersion version,
			Collection<Connection> connections)
			throws IOException, SpinnmanException,
			uk.ac.manchester.spinnaker.transceiver.processes.ProcessException {
		super(version, connections, null, null, null, null, null, null);
	}

	@Override
	public Machine getMachineDetails() {
		return new VirtualMachine(new MachineDimensions(2, 2));
	}

	@Override
	void updateMachine() {
		this.machine = getMachineDetails();
	}

	@Override
	public void writeMemory(HasCoreLocation core, int baseAddress,
			ByteBuffer data) {
		writtenMemory.add(new Write(core, baseAddress, data));
	}
}

class EIEIOConnectionFactory implements ConnectionFactory<EIEIOConnection> {
	@Override
	public Class<EIEIOConnection> getClassKey() {
		return EIEIOConnection.class;
	}

	@Override
	public EIEIOConnection getInstance(InetAddress localAddress)
			throws IOException {
		return new EIEIOConnection(localAddress, null, null, null);
	}

	@Override
	public EIEIOConnection getInstance(InetAddress localAddress, int localPort)
			throws IOException {
		return new EIEIOConnection(localAddress, localPort, null, null);
	}
}
