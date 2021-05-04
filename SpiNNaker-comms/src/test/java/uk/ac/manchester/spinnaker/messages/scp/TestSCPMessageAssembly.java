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
package uk.ac.manchester.spinnaker.messages.scp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.ac.manchester.spinnaker.machine.Direction.EAST;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_VER;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;

class TestSCPMessageAssembly {
	private static final CoreLocation ZERO_CORE = new CoreLocation(0, 0, 0);

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
        ReadLink scp = new ReadLink(ZERO_CORE, EAST, 0, 252);
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
