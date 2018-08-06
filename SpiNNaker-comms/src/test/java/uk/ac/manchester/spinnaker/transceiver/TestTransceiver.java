package uk.ac.manchester.spinnaker.transceiver;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import testconfig.BoardTestConfiguration;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.LinkDescriptor;
import uk.ac.manchester.spinnaker.machine.Machine;

class TestTransceiver {
	static BoardTestConfiguration board_config;

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		board_config = new BoardTestConfiguration();
	}

	@Test
	void test() {
		fail("Not yet implemented");
	}

}

class Write {
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

class MockWriteTransceiver extends Transceiver {
	List<Object> written_memory;

	public MockWriteTransceiver(int version, Collection<Connection> connections,
			Collection<ChipLocation> ignoreChips,
			Collection<CoreLocation> ignoreCores,
			Collection<LinkDescriptor> ignoreLinks, Integer maxCoreID,
			Collection<ConnectionDescriptor> scampConnections,
			Integer maxSDRAMSize) throws IOException, SpinnmanException,
			uk.ac.manchester.spinnaker.processes.Process.Exception {
		super(version, connections, ignoreChips, ignoreCores, ignoreLinks, maxCoreID,
				scampConnections, maxSDRAMSize);
		this.written_memory = new ArrayList<>();
		// TODO Auto-generated constructor stub
	}

    @Override
	public Machine getMachineDetails() {
        return new VirtualMachine(2, 2);
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
