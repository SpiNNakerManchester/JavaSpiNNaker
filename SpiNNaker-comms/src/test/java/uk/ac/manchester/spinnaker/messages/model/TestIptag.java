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
	static BoardTestConfiguration board_config;
	static final ChipLocation ZERO_CHIP = new ChipLocation(0, 0);

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		board_config = new BoardTestConfiguration();
	}

	@Test
	void testNewIptag() throws UnknownHostException, SocketException {
        board_config.set_up_remote_board();
        InetAddress ip = InetAddress.getByName("8.8.8.8");
        int port = 1337;
        int tag = 255;
        InetAddress board_address = InetAddress.getByName(board_config.remotehost);
        IPTag iptag = new IPTag(board_address, ZERO_CHIP, tag, ip, port);
        assertEquals(ip, iptag.getIPAddress());
        assertEquals(port, (int) iptag.getPort());
        assertEquals(tag, iptag.getTag());
	}

}
