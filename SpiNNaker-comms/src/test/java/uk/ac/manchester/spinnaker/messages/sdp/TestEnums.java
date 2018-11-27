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
package uk.ac.manchester.spinnaker.messages.sdp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestEnums {

	@Test
	void testSdpHeaderFlag() {
		assertEquals(0x7, SDPHeader.Flag.REPLY_NOT_EXPECTED.value);
		assertEquals((byte) 0x87, SDPHeader.Flag.REPLY_EXPECTED.value);
	}

	@Test
	void testSdpPort() {
		assertEquals(0, SDPPort.DEFAULT_PORT.value);
		assertEquals(3, SDPPort.RUNNING_COMMAND_SDP_PORT.value);
	}
}
