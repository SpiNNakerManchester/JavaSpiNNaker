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
package testconfig;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static uk.ac.manchester.spinnaker.machine.MachineVersion.FIVE;
import static uk.ac.manchester.spinnaker.utils.InetFactory.getByName;
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

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.machine.MachineVersion;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.spalloc.CreateJob;
import uk.ac.manchester.spinnaker.spalloc.SpallocJob;
import uk.ac.manchester.spinnaker.spalloc.exceptions.JobDestroyedException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocStateChangeTimeoutException;
import uk.ac.manchester.spinnaker.utils.RawConfigParser;

@SuppressWarnings({
	"checkstyle:JavadocVariable", "checkstyle:VisibilityModifier"
})
public class BoardTestConfiguration {
	public static final String LOCALHOST = "127.0.0.1";

	/**
	 * Microsoft invalid IP address.
	 *
	 * @see <a href=
	 *      "https://answers.microsoft.com/en-us/windows/forum/windows_vista-networking/invalid-ip-address-169254xx/ce096728-e2b7-4d54-80cc-52a4ed342870"
	 *      >Forum post</a>
	 */
	public static final String NOHOST = "169.254.254.254";

	public static final int PORT = 54321;

	private static RawConfigParser config = new RawConfigParser(
			BoardTestConfiguration.class.getResource("test.cfg"));

	private static final String MCSEC = "Machine";

	private static final String SPSEC = "Spalloc";

	private static final int KEEPALIVE_SECS = 60;

	public static final String OWNER = "java test harness";

	public String localhost;

	public Integer localport;

	public Inet4Address remotehost;

	public MachineVersion boardVersion;

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

	private void initRemoteHost(String name, boolean checkReachable) {
		try {
			remotehost = getByName(name);
		} catch (UnknownHostException e) {
			assumeTrue(false,
					() -> format("test board (%s) appears to be not in DNS",
							name));
		}
		if (checkReachable) {
			assumeTrue(hostIsReachable(remotehost.getHostAddress()),
					() -> format("test board (%s) appears to be down",
							remotehost));
		}
	}

	public void setUpLocalVirtualBoard() {
		localhost = LOCALHOST;
		localport = PORT;
		initRemoteHost(LOCALHOST, false);
		boardVersion = MachineVersion.byId(config.getInt(MCSEC, "version"));
	}

	public void setUpRemoteBoard()
			throws SocketException, UnknownHostException {
		initRemoteHost(config.get(MCSEC, "machineName"), true);
		boardVersion = MachineVersion.byId(config.getInt(MCSEC, "version"));
		var names = config.get(MCSEC, "bmp_names");
		if (names == null || "None".equals(names)) {
			bmpNames = null;
		} else {
			var bmpHost = getByName(names);
			bmpNames = List.of(
					new BMPConnectionData(0, 0, bmpHost, List.of(0), null));
		}
		autoDetectBMP = config.getBoolean(MCSEC, "auto_detect_bmp");
		localport = PORT;
		try (var s = new DatagramSocket()) {
			s.connect(new InetSocketAddress(remotehost, PORT));
			localhost = s.getLocalAddress().getHostAddress();
		}
	}

	private CreateJob jobDesc(int size, double keepAlive, String tag) {
		if (tag == null) {
			tag = "default";
		}
		return new CreateJob(size).owner(OWNER).keepAlive(keepAlive).tags(tag);
	}

	@MustBeClosed
	public SpallocJob setUpSpallocedBoard()
			throws IOException, SpallocServerException, JobDestroyedException,
			SpallocStateChangeTimeoutException, InterruptedException {
		var spalloc = config.get(SPSEC, "hostname");
		assumeTrue(spalloc != null, "no spalloc server defined");
		assumeTrue(hostIsReachable(spalloc),
				() -> format("spalloc server (%s) appears to be down",
						spalloc));
		var port = config.getInt(SPSEC, "port");
		var timeout = config.getInt(SPSEC, "timeout");
		var tag = config.get(SPSEC, "tag");
		@SuppressWarnings({"resource", "MustBeClosed"})
		var job = new SpallocJob(spalloc, port, timeout,
				jobDesc(1, KEEPALIVE_SECS, tag));
		job.waitUntilReady(null);
		try {
			initRemoteHost(job.getHostname(), true);
		} catch (TestAbortedException e) {
			job.destroy("cannot use board from here");
			job.close();
			throw e;
		}
		boardVersion = FIVE; // ASSUME FOR SPALLOC!
		bmpNames = null; // NO ACCESS TO BMP
		autoDetectBMP = false;
		return job;
	}

	public void setUpNonexistentBoard() throws UnknownHostException {
		localhost = null;
		localport = PORT;
		remotehost = getByName(NOHOST);
		boardVersion = MachineVersion.byId(config.getInt(MCSEC, "version"));
		assumeFalse(hostIsReachable(remotehost),
				() -> format("unreachable host (%s) appears to be up",
						remotehost));
	}

	private static final Map<Object, Boolean> REACHABLE = new HashMap<>();

	private static boolean hostIsReachable(String remotehost) {
		return REACHABLE.computeIfAbsent(remotehost,
				__ -> ping(remotehost) == 0);
	}

	private static boolean hostIsReachable(InetAddress remotehost) {
		return REACHABLE.computeIfAbsent(remotehost,
				__ -> ping(remotehost) == 0);
	}
}
