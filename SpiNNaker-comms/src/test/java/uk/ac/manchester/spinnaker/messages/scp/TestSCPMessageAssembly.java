package uk.ac.manchester.spinnaker.messages.scp;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_VER;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;

class TestSCPMessageAssembly {
	static final CoreLocation ZERO_CORE = new CoreLocation(0, 0, 0);

	@Test
	void testCreateNewSCPHeader() {
		SCPRequestHeader header = new SCPRequestHeader(CMD_VER);
		assertEquals(CMD_VER, header.command);
		assertEquals(0, header.getSequence());
	}

	@Test
	void testCreateNewVerSCPPacket() {
        GetVersion scp = new GetVersion(ZERO_CORE);
        assertEquals(0, scp.argument1);
        assertEquals(0, scp.argument2);
        assertEquals(0, scp.argument3);
        assertNull(scp.data);
	}

	@Test
	void testCreateNewLinkSCPPacket() {
        ReadLink scp = new ReadLink(ZERO_CORE, 0, 0, 252);
        assertEquals(0, scp.argument1);
        assertEquals(252, scp.argument2);
        assertEquals(0, scp.argument3);
        assertNull(scp.data);
	}

	@Test
	void testCreateNewMemorySCPPacket() {
        ReadMemory scp = new ReadMemory(ZERO_CORE, 0, 252);
        assertEquals(0, scp.argument1);
        assertEquals(252, scp.argument2);
        assertEquals(2, scp.argument3);
        assertNull(scp.data);
	}
}
