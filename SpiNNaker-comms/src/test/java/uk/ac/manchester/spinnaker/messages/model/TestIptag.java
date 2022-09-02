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
package uk.ac.manchester.spinnaker.messages.model;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import testconfig.BoardTestConfiguration;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;

class TestIptag {
	private static BoardTestConfiguration boardConfig;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		boardConfig = new BoardTestConfiguration();
	}

	@Test
	void testNewIptag() throws UnknownHostException, SocketException {
		boardConfig.setUpRemoteBoard();
		var ip = InetAddress.getByName("8.8.8.8");
		int port = 1337;
		int tag = 255;
		var iptag = new IPTag(boardConfig.remotehost, ZERO_ZERO, tag, ip, port);
		assertEquals(ip, iptag.getIPAddress());
		assertEquals(port, (int) iptag.getPort());
		assertEquals(tag, iptag.getTag());
	}

}
