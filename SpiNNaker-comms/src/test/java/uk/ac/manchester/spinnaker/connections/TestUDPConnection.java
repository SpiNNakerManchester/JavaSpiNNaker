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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static uk.ac.manchester.spinnaker.machine.Direction.EAST;
import static uk.ac.manchester.spinnaker.machine.MemoryLocation.NULL;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.SCPRequest.BOOT_CHIP;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_OK;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.BUFFERED_SDRAM_START;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import testconfig.BoardTestConfiguration;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.messages.scp.GetVersion;
import uk.ac.manchester.spinnaker.messages.scp.ReadLink;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;

public class TestUDPConnection {
	private static BoardTestConfiguration boardConfig;

	private static final CoreLocation ZERO_CORE = new CoreLocation(0, 0, 0);

	private static final ChipLocation ZERO_CHIP = new ChipLocation(0, 0);

	private static final Integer TIMEOUT = 10000;

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		boardConfig = new BoardTestConfiguration();
	}

	@Test
	public void testSCPVersionWithBoard() throws Exception {
		boardConfig.setUpRemoteBoard();
		var scpReq = new GetVersion(ZERO_CORE);
		scpReq.scpRequestHeader.issueSequenceNumber(Set.of());
		SCPResultMessage result;
		try (var connection =
				new SCPConnection(BOOT_CHIP, null, null,
						boardConfig.remotehost)) {
			connection.send(scpReq);
			result = connection.receiveSCPResponse(TIMEOUT);
		} catch (SocketTimeoutException e) {
			assertEquals(0, e.bytesTransferred);
			assumeTrue(false, "timed out (general connectivity issue?)");
			throw e; // unreachable
		}
		var scpResponse = result.parsePayload(scpReq);
		System.out.println(scpResponse.versionInfo);
		assertEquals(scpResponse.result, RC_OK);
	}

	private static final int LINK_SIZE = 250;

	@Test
	public void testSCPReadLinkWithBoard() throws Exception {
		boardConfig.setUpRemoteBoard();
		var scpReq =
				new ReadLink(ZERO_CHIP, EAST, BUFFERED_SDRAM_START, LINK_SIZE);
		scpReq.scpRequestHeader.issueSequenceNumber(Set.of());
		SCPResultMessage result;
		try (var connection =
				new SCPConnection(BOOT_CHIP, null, null,
						boardConfig.remotehost)) {
			connection.send(scpReq);
			result = connection.receiveSCPResponse(TIMEOUT);
		} catch (SocketTimeoutException e) {
			assertEquals(0, e.bytesTransferred);
			assumeTrue(false, "timed out (general connectivity issue?)");
			throw e; // unreachable
		}
		assertEquals(result.getResult(), RC_OK);
	}

	@Test
	public void testSCPReadMemoryWithBoard() throws Exception {
		boardConfig.setUpRemoteBoard();
		var scpReq = new ReadMemory(ZERO_CHIP, BUFFERED_SDRAM_START,
				UDP_MESSAGE_MAX_SIZE);
		scpReq.scpRequestHeader.issueSequenceNumber(Set.of());
		SCPResultMessage result;
		try (var connection =
				new SCPConnection(BOOT_CHIP, null, null,
						boardConfig.remotehost)) {
			connection.send(scpReq);
			result = connection.receiveSCPResponse(TIMEOUT);
		} catch (SocketTimeoutException e) {
			assertEquals(0, e.bytesTransferred);
			assumeTrue(false, "timed out (general connectivity issue?)");
			throw e; // unreachable
		}
		assertEquals(result.getResult(), RC_OK);
	}

	@Test
	public void testSendSCPRequestToNonexistentHost()
			throws UnknownHostException {
		boardConfig.setUpNonexistentBoard();
		assertThrows(IOException.class, () -> {
			try (var connection =
					new SCPConnection(BOOT_CHIP, null, null,
							boardConfig.remotehost)) {
				var scp = new ReadMemory(ZERO_CHIP, NULL, UDP_MESSAGE_MAX_SIZE);
				scp.scpRequestHeader.issueSequenceNumber(Set.of());
				connection.send(scp);
				connection.receiveSCPResponse(2);
			}
		});
	}
}
