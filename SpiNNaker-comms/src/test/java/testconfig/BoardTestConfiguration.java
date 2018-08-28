package testconfig;

import static java.util.Arrays.asList;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.jupiter.api.Assumptions;

import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.utils.InetFactory;
import uk.ac.manchester.spinnaker.utils.RawConfigParser;

public class BoardTestConfiguration {
	public static final String LOCALHOST = "127.0.0.1";
	/**
	 * Microsoft invalid IP address.
	 *
	 * @see <a href=
	 *      "http://answers.microsoft.com/en-us/windows/forum/windows_vista-networking/invalid-ip-address-169254xx/ce096728-e2b7-4d54-80cc-52a4ed342870"
	 *      >Forum post</a>
	 */
	public static final String NOHOST = "169.254.254.254";
	public static final int PORT = 54321;
	private static RawConfigParser config = new RawConfigParser(
			BoardTestConfiguration.class.getResource("test.cfg"));

	public String localhost;
	public Integer localport;
	public String remotehost;
	public Integer board_version;
	public List<BMPConnectionData> bmp_names;
	public Boolean auto_detect_bmp;

	public BoardTestConfiguration() throws IOException {
		this.localhost = null;
		this.localport = null;
		this.remotehost = null;
		this.board_version = null;
		this.bmp_names = null;
		this.auto_detect_bmp = null;
	}

	public void set_up_local_virtual_board() {
		localhost = LOCALHOST;
		localport = PORT;
		remotehost = LOCALHOST;
		board_version = config.getint("Machine", "version");
	}

	public void set_up_remote_board() throws SocketException, UnknownHostException {
		remotehost = config.get("Machine", "machineName");
		Assumptions.assumeTrue(host_is_reachable(remotehost),
				() -> "test board (" + remotehost + ") appears to be down");
		board_version = config.getint("Machine", "version");
		String names = config.get("Machine", "bmp_names");
        Inet4Address bmpHost = InetFactory.getByName(names);
		if (names == "None") {
			bmp_names = null;
		} else {
			bmp_names =
					asList(new BMPConnectionData(0, 0, bmpHost, asList(0), null));
		}
		auto_detect_bmp = config.getboolean("Machine", "auto_detect_bmp");
		localport = PORT;
		try (DatagramSocket s = new DatagramSocket()) {
			s.connect(new InetSocketAddress(remotehost, PORT));
			localhost = s.getLocalAddress().getHostAddress();
		}
	}

	public void set_up_nonexistent_board() {
		localhost = null;
		localport = PORT;
		remotehost = NOHOST;
		board_version = config.getint("Machine", "version");
	}

	private static boolean host_is_reachable(String remotehost) {
		return ping(remotehost) == 0;
	}
}
