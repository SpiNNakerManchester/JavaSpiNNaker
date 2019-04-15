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
package uk.ac.manchester.spinnaker.connections;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_OK;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import testconfig.BoardTestConfiguration;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import uk.ac.manchester.spinnaker.messages.scp.GetVersion;
import uk.ac.manchester.spinnaker.messages.scp.GetVersion.Response;
import uk.ac.manchester.spinnaker.messages.scp.ReadLink;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;

public class TestUDPConnection {
	static BoardTestConfiguration boardConfig;
	static final CoreLocation ZERO_CORE = new CoreLocation(0, 0, 0);
	static final ChipLocation ZERO_CHIP = new ChipLocation(0, 0);

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		boardConfig = new BoardTestConfiguration();
	}

	@Test
	public void testSCPVersionWithBoard() throws Exception {
		boardConfig.setUpRemoteBoard();
		GetVersion scpReq = new GetVersion(ZERO_CORE);
		scpReq.scpRequestHeader.issueSequenceNumber(emptySet());
		SCPResultMessage result;
		try (SCPConnection connection =
				new SCPConnection(boardConfig.remotehost)) {
			connection.sendSCPRequest(scpReq);
			result = connection.receiveSCPResponse(null);
		}
		Response scp_response = result.parsePayload(scpReq);
		System.out.println(scp_response.versionInfo);
		assertEquals(scp_response.result, RC_OK);
	}

	@Test
	public void testSCPReadLinkWoard() throws Exception {
		boardConfig.setUpRemoteBoard();
		ReadLink scpReq = new ReadLink(ZERO_CHIP, 0, 0x70000000, 250);
		scpReq.scpRequestHeader.issueSequenceNumber(emptySet());
		SCPResultMessage result;
		try (SCPConnection connection =
				new SCPConnection(boardConfig.remotehost)) {
			connection.sendSCPRequest(scpReq);
			result = connection.receiveSCPResponse(null);
		}
		assertEquals(result.getResult(), RC_OK);
	}

	@Test
	public void testSCPReadMemoryWithBoard() throws Exception {
		boardConfig.setUpRemoteBoard();
		ReadMemory scpReq = new ReadMemory(
                ZERO_CHIP, 0x70000000, UDP_MESSAGE_MAX_SIZE);
		scpReq.scpRequestHeader.issueSequenceNumber(emptySet());
		SCPResultMessage result;
		try (SCPConnection connection =
				new SCPConnection(boardConfig.remotehost)) {
			connection.sendSCPRequest(scpReq);
			result = connection.receiveSCPResponse(null);
		}
		assertEquals(result.getResult(), RC_OK);
	}

	@Test
	@Disabled("host reachability; issue #215")
	public void testSendSCPRequestToNonexistentHost()
			throws UnknownHostException {
		boardConfig.setUpNonexistentBoard();
		assertThrows(SocketTimeoutException.class, () -> {
			try (SCPConnection connection =
					new SCPConnection(boardConfig.remotehost)) {
				ReadMemory scp =
						new ReadMemory(ZERO_CHIP, 0, UDP_MESSAGE_MAX_SIZE);
				scp.scpRequestHeader.issueSequenceNumber(emptySet());
				connection.sendSCPRequest(scp);
				connection.receiveSCPResponse(2);
			}
		});
	}
}
