package uk.ac.manchester.spinnaker.connections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_OK;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import testconfig.BoardTestConfiguration;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.messages.scp.GetVersion;
import uk.ac.manchester.spinnaker.messages.scp.GetVersion.Response;
import uk.ac.manchester.spinnaker.messages.scp.ReadLink;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;

public class TestUDPConnection {
	static BoardTestConfiguration board_config;
	static final CoreLocation ZERO_CORE = new CoreLocation(0, 0, 0);
	static final ChipLocation ZERO_CHIP = new ChipLocation(0, 0);

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		board_config = new BoardTestConfiguration();
	}

	@Test
	public void testSCPVersionWithBoard() throws Exception {
		board_config.set_up_remote_board();
		GetVersion scp_req = new GetVersion(ZERO_CORE);
		SCPResultMessage result;
		try (SCPConnection connection = new SCPConnection(
				board_config.remotehost)) {
			connection.sendSCPRequest(scp_req);
			result = connection.receiveSCPResponse(null);
		}
		Response scp_response = scp_req.getSCPResponse(result.responseData);
		System.out.println(scp_response.versionInfo);
		assertEquals(scp_response.scpResponseHeader.result, RC_OK);
	}

	@Test
	public void testSCPReadLinkWoard() throws Exception {
		board_config.set_up_remote_board();
		ReadLink scp_link = new ReadLink(ZERO_CHIP, 0, 0x70000000, 250);
		SCPResultMessage result;
		try (SCPConnection connection = new SCPConnection(
				board_config.remotehost)) {
			connection.sendSCPRequest(scp_link);
			result = connection.receiveSCPResponse(null);
		}
		assertEquals(result.result, RC_OK);
	}

	@Test
	public void testSCPReadMemoryWithBoard() throws Exception {
		board_config.set_up_remote_board();
		ReadMemory scp_link = new ReadMemory(ZERO_CHIP, 0x70000000, 256);
		SCPResultMessage result;
		try (SCPConnection connection = new SCPConnection(
				board_config.remotehost)) {
			connection.sendSCPRequest(scp_link);
			result = connection.receiveSCPResponse(null);
		}
		assertEquals(result.result, RC_OK);
	}

	@Test
	public void testSendSCPRequestToNonexistentHost() {
		board_config.set_up_nonexistent_board();
		assertThrows(SocketTimeoutException.class, () -> {
			try (SCPConnection connection = new SCPConnection(
					board_config.remotehost)) {
				ReadMemory scp = new ReadMemory(ZERO_CHIP, 0, 256);
				connection.sendSCPRequest(scp);
				connection.receiveSCPResponse(2);
			}
		});
	}
}
