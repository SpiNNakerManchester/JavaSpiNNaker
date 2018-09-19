package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static testconfig.BoardTestConfiguration.NOHOST;
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
import uk.ac.manchester.spinnaker.machine.VirtualMachine;
import uk.ac.manchester.spinnaker.transceiver.UDPTransceiver.ConnectionFactory;
import uk.ac.manchester.spinnaker.utils.InetFactory;

class TestTransceiver {
	static BoardTestConfiguration board_config;
	static final int ver = 5; // Guess?

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		board_config = new BoardTestConfiguration();
	}

	@Test
	void testCreateNewTransceiverToBoard() throws Exception {
		List<Connection> connections = new ArrayList<>();

		board_config.set_up_remote_board();
        Inet4Address remoteHost = InetFactory.getByName(board_config.remotehost);
		connections.add(new SCPConnection(remoteHost));

		try (Transceiver trans = new Transceiver(ver, connections, null, null,
				null, null, null, null)) {
			assertEquals(1, trans.getConnections().size());
		}
	}

	@Test
	void testCreateNewTransceiverOneConnection() throws Exception {
		List<Connection> connections = new ArrayList<>();

		board_config.set_up_remote_board();
        Inet4Address remoteHost = InetFactory.getByName(board_config.remotehost);
		connections.add(new SCPConnection(remoteHost));

		try (Transceiver trans = new Transceiver(ver, connections, null, null,
				null, null, null, null)) {
			assertEquals(new HashSet<>(connections), trans.getConnections());
		}
	}

	@Test
	void testCreateNewTransceiverFromListConnections() throws Exception {
		List<Connection> connections = new ArrayList<>();

		board_config.set_up_remote_board();
        Inet4Address remoteHost = InetFactory.getByName(board_config.remotehost);
		connections.add(new SCPConnection(remoteHost));

		board_config.set_up_local_virtual_board();
		connections.add(new SCPConnection(remoteHost));

		try (Transceiver trans = new Transceiver(ver, connections, null, null,
				null, null, null, null)) {
			for (Connection c : trans.getConnections()) {
				assertTrue(connections.contains(c));
			}
			assertEquals(new HashSet<>(connections), trans.getConnections());
		}
	}

	@Test
	void testRetrievingMachineDetails() throws Exception {
		List<Connection> connections = new ArrayList<>();

		board_config.set_up_remote_board();
        Inet4Address remoteHost = InetFactory.getByName(board_config.remotehost);
		connections.add(new SCPConnection(remoteHost));

		board_config.set_up_local_virtual_board();
		connections.add(
				new BootConnection(null, null, remoteHost, null));

		try (Transceiver trans = new Transceiver(ver, connections, null, null,
				null, null, null, null)) {
			if (board_config.board_version == 3
					|| board_config.board_version == 2) {
				assertEquals(2, trans.getMachineDimensions().width);
				assertEquals(2, trans.getMachineDimensions().height);
			} else if (board_config.board_version == 5
					|| board_config.board_version == 4) {
				assertEquals(8, trans.getMachineDimensions().width);
				assertEquals(8, trans.getMachineDimensions().height);
			} else {
				MachineDimensions size = trans.getMachineDimensions();
				fail(format("Unknown board with size %dx%d", size.width,
						size.height));
			}
			assertTrue(trans.isConnected());
			assertNotNull(trans.getScampVersion());
			assertNotNull(trans.getCPUInformation());
		}
	}

	@Test
	void testBootBoard() throws Exception {
		board_config.set_up_remote_board();
        InetAddress remoteHost = InetFactory.getByName(board_config.remotehost);

		try (Transceiver trans = Transceiver.createTransceiver(
				remoteHost, board_config.board_version)) {
			// self.assertFalse(trans.is_connected())
			trans.bootBoard();
		}
	}

	/** Tests the creation of listening sockets. 
	@Test
	void testListenerCreation() throws Exception {
		// Create board connections
		List<Connection> connections = new ArrayList<>();
        Inet4Address noHost = InetFactory.getByName(NOHOST);
		connections.add(new SCPConnection(null, (Integer) null, noHost, null));
		EIEIOConnection orig = new EIEIOConnection(null, null, null, null);
		connections.add(orig);

		// Create transceiver
		try (Transceiver trnx = new Transceiver(5, connections, null, null,
				null, null, null, null)) {
			int port = orig.getLocalPort();
			EIEIOConnectionFactory cf = new EIEIOConnectionFactory();
			// Register a UDP listeners
			Connection c1 = trnx.registerUDPListener(null, cf);
			assertTrue(c1 == orig, "first connection must be original");
			Connection c2 = trnx.registerUDPListener(null, cf);
			assertTrue(c2 == orig, "second connection must be original");
			Connection c3 = trnx.registerUDPListener(null, cf, port);
			assertTrue(c3 == orig, "third connection must be original");
			Connection c4 = trnx.registerUDPListener(null, cf, port + 1);
			assertFalse(c4 == orig, "fourth connection must not be original");
		}
	}

	@Test
	void testSetWatchdog() throws Exception {
		// The expected write values for the watch dog
		List<byte[]> expected_writes = asList(new byte[] {
				((Number) software_watchdog_count.getDefault()).byteValue()
		}, new byte[] {
				0
		}, new byte[] {
				5
		});

		List<Connection> connections = new ArrayList<>();
        Inet4Address noHost = InetFactory.getByName(NOHOST);
		connections.add(new SCPConnection(noHost));
		try (MockWriteTransceiver tx =
				new MockWriteTransceiver(5, connections)) {
			// All chips
			tx.enableWatchDogTimer(true);
			tx.enableWatchDogTimer(false);
			tx.setWatchDogTimeout(5);

			/*
			 * Check the values that were "written" for set_watch_dog, which
			 * should be one per chip
			 * /
			int write_item = 0;
			for (byte[] expected_data : expected_writes) {
				for (ChipLocation chip : tx.getMachineDetails()
						.chipCoordinates()) {
					MockWriteTransceiver.Write write =
							tx.written_memory.get(write_item++);
					assertEquals(chip.getScampCore(), write.core);
					assertEquals(
							SYSTEM_VARIABLE_BASE_ADDRESS
									+ software_watchdog_count.offset,
							write.address);
					assertArrayEquals(expected_data, write.data);
				}
			}
		}
	}*/

	static class MockWriteTransceiver extends Transceiver {
		static class Write {
			final CoreLocation core;
			final byte[] data;
			final int address;
			final int offset;
			final int n_bytes;

			Write(HasCoreLocation core, int baseAddress, ByteBuffer data) {
				this.core = core.asCoreLocation();
				this.address = baseAddress;
				this.data = data.array().clone();
				this.offset = data.position();
				this.n_bytes = data.remaining();
			}
		}

		List<Write> written_memory = new ArrayList<>();

		public MockWriteTransceiver(int version,
				Collection<Connection> connections)
				throws IOException, SpinnmanException,
				uk.ac.manchester.spinnaker.processes.Process.Exception {
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
			written_memory.add(new Write(core, baseAddress, data));
		}
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
