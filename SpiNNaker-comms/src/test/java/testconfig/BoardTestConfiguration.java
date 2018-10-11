package testconfig;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opentest4j.TestAbortedException;

import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.spalloc.SpallocJob;
import uk.ac.manchester.spinnaker.spalloc.exceptions.JobDestroyedException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocStateChangeTimeoutException;
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
	private static final String MCSEC = "Machine";
	private static final String SPSEC = "Spalloc";

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
		boardVersion = config.getint(MCSEC, "version");
	}

	public void setUpRemoteBoard()
			throws SocketException, UnknownHostException {
		remotehost = InetFactory.getByName(config.get(MCSEC, "machineName"));
		assumeTrue(hostIsReachable(remotehost.getHostAddress()),
				() -> "test board (" + remotehost + ") appears to be down");
		boardVersion = config.getint(MCSEC, "version");
		String names = config.get(MCSEC, "bmp_names");
		if (names == null || "None".equals(names)) {
			bmpNames = null;
		} else {
			Inet4Address bmpHost = InetFactory.getByName(names);
			bmpNames = asList(
					new BMPConnectionData(0, 0, bmpHost, asList(0), null));
		}
		autoDetectBMP = config.getboolean(MCSEC, "auto_detect_bmp");
		localport = PORT;
		try (DatagramSocket s = new DatagramSocket()) {
			s.connect(new InetSocketAddress(remotehost, PORT));
			localhost = s.getLocalAddress().getHostAddress();
		}
	}

	public SpallocJob setUpSpallocedBoard()
			throws IOException, SpallocServerException, JobDestroyedException,
			SpallocStateChangeTimeoutException, InterruptedException {
		String spalloc = config.get(SPSEC, "hostname");
		assumeTrue(spalloc != null, "no spalloc server defined");
		assumeTrue(hostIsReachable(spalloc),
				() -> "spalloc server (" + spalloc + ") appears to be down");
		Integer port = config.getint(SPSEC, "port");
		Integer timeout = config.getint(SPSEC, "timeout");
		String tag = config.get(SPSEC, "tag");
		Map<String, Object> kwargs = new HashMap<>();
		kwargs.put("owner", "java test harness");
		kwargs.put("keepalive", 60);
		if (tag != null) {
			tag = "default";
		}
		kwargs.put("tags", new String[] {
				tag
		});
		SpallocJob job =
				new SpallocJob(spalloc, port, timeout, asList(1), kwargs);
		job.waitUntilReady(null);
		try {
			remotehost = InetFactory.getByName(job.getHostname());
			assumeTrue(hostIsReachable(remotehost),
					() -> "spalloc server (" + spalloc
							+ ") gave unreachable board (" + remotehost + ")");
		} catch (UnknownHostException | TestAbortedException e) {
			job.destroy("cannot use board from here");
			job.close();
			throw e;
		}
		boardVersion = 5; // ASSUME FOR SPALLOC!
		bmpNames = null; // NO ACCESS TO BMP
		autoDetectBMP = false;
		return job;
	}

	public void setUpNonexistentBoard() throws UnknownHostException {
		localhost = null;
		localport = PORT;
		remotehost = InetFactory.getByName(NOHOST);
		boardVersion = config.getint(MCSEC, "version");
	}

	private static boolean hostIsReachable(String remotehost) {
		return ping(remotehost) == 0;
	}

	private static boolean hostIsReachable(InetAddress remotehost) {
		return ping(remotehost) == 0;
	}
}
