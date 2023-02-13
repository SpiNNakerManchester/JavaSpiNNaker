/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.ac.manchester.spinnaker.machine.Direction.EAST;
import static uk.ac.manchester.spinnaker.machine.MemoryLocation.NULL;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_VER;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;

class TestSCPMessageAssembly {
	private static final CoreLocation ZERO_CORE = new CoreLocation(0, 0, 0);

	@Test
	void testCreateNewSCPHeader() {
		var header = new SCPRequestHeader(CMD_VER);
		assertEquals(CMD_VER, header.command);
		assertEquals(0, header.getSequence());
	}

	@Test
	void testCreateNewVerSCPPacket() {
		var scp = new GetVersion(ZERO_CORE);
		assertEquals(0, scp.argument1);
		assertEquals(0, scp.argument2);
		assertEquals(0, scp.argument3);
		assertNull(scp.data);
	}

	@Test
	void testCreateNewLinkSCPPacket() {
		var scp = new ReadLink(ZERO_CORE, EAST, NULL, 252);
		assertEquals(0, scp.argument1);
		assertEquals(252, scp.argument2);
		assertEquals(0, scp.argument3);
		assertNull(scp.data);
	}

	@Test
	void testCreateNewMemorySCPPacket() {
		var scp = new ReadMemory(ZERO_CORE, NULL, 252);
		assertEquals(0, scp.argument1);
		assertEquals(252, scp.argument2);
		assertEquals(2, scp.argument3);
		assertNull(scp.data);
	}
}
