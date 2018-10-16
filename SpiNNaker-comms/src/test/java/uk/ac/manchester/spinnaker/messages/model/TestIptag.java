package uk.ac.manchester.spinnaker.messages.model;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import testconfig.BoardTestConfiguration;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;

class TestIptag {
	static BoardTestConfiguration boardConfig;
	static final ChipLocation ZERO_CHIP = new ChipLocation(0, 0);

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		boardConfig = new BoardTestConfiguration();
	}

	@Test
	void testNewIptag() throws UnknownHostException, SocketException {
		boardConfig.setUpRemoteBoard();
		InetAddress ip = InetAddress.getByName("8.8.8.8");
		int port = 1337;
		int tag = 255;
		IPTag iptag =
				new IPTag(boardConfig.remotehost, ZERO_CHIP, tag, ip, port);
		assertEquals(ip, iptag.getIPAddress());
		assertEquals(port, (int) iptag.getPort());
		assertEquals(tag, iptag.getTag());
	}

}
