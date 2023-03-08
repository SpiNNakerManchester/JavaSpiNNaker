/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
