package testconfig;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

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
	public Inet4Address remotehost;
	public Integer boardVersion;
	public List<BMPConnectionData> bmpNames;
	public Boolean autoDetectBMP;

	public BoardTestConfiguration() {
		this.localhost = null;
		this.localport = null;
		this.remotehost = null;
		this.boardVersion = null;
		this.bmpNames = null;
		this.autoDetectBMP = null;
	}

	public void setUpLocalVirtualBoard() throws UnknownHostException {
		localhost = LOCALHOST;
		localport = PORT;
		remotehost = InetFactory.getByName(LOCALHOST);
		boardVersion = config.getint("Machine", "version");
	}

	public void setUpRemoteBoard()
			throws SocketException, UnknownHostException {
		remotehost = InetFactory.getByName(config.get("Machine", "machineName"));
		assumeTrue(hostIsReachable(remotehost.getHostAddress()),
				() -> "test board (" + remotehost + ") appears to be down");
		boardVersion = config.getint("Machine", "version");
		String names = config.get("Machine", "bmpNames");
		if (names == "None") {
			bmpNames = null;
		} else {
			Inet4Address bmpHost = InetFactory.getByName(names);
			bmpNames = asList(
					new BMPConnectionData(0, 0, bmpHost, asList(0), null));
		}
		autoDetectBMP = config.getboolean("Machine", "autoDetectBMP");
		localport = PORT;
		try (DatagramSocket s = new DatagramSocket()) {
			s.connect(new InetSocketAddress(remotehost, PORT));
			localhost = s.getLocalAddress().getHostAddress();
		}
	}

	public void setUpNonexistentBoard() throws UnknownHostException {
		localhost = null;
		localport = PORT;
		remotehost = InetFactory.getByName(NOHOST);
		boardVersion = config.getint("Machine", "version");
	}

	private static boolean hostIsReachable(String remotehost) {
		return ping(remotehost) == 0;
	}
}
