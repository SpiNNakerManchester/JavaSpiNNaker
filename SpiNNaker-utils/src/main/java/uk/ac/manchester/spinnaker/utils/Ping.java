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
package uk.ac.manchester.spinnaker.utils;

import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import com.google.errorprone.annotations.CheckReturnValue;

/**
 * How to ping a host to test for (ICMP) network connectivity.
 *
 * @author Donal Fellows
 */
public abstract class Ping {
	private static final int PING_DELAY = 500;

	private static final int PING_COUNT = 10;

	private Ping() {
	}

	private static ProcessBuilder pingCmd(String address, int count, int wait) {
		if (getProperty("os.name").toLowerCase().contains("win")) {
			return new ProcessBuilder("ping", "-n", Integer.toString(count),
					"-w", Integer.toString(wait), address);
		} else {
			return new ProcessBuilder("ping", "-c", Integer.toString(count),
					"-W", Integer.toString(wait), address);
		}
	}

	/**
	 * Core ping operation.
	 *
	 * @param address
	 *            Where to ping
	 * @return Return code, or -1 on total failure
	 */
	private static int ping1(String address) {
		var cmd = pingCmd(address, 1, 1);
		cmd.redirectErrorStream(true);
		try {
			var process = cmd.start();
			var input = process.getInputStream();
			new Daemon(() -> drain(input)).start();
			return process.waitFor();
		} catch (Exception e) {
			return -1;
		}
	}

	private static void drain(InputStream is) {
		try (is) {
			is.skip(Long.MAX_VALUE);
		} catch (IOException e) {
			// Ignore this exception
		}
	}

	/**
	 * Pings to detect if a host or IP address is reachable. May wait for up to
	 * about five seconds (or longer <i>in extremis</i>). Technically, it only
	 * detects if a host is reachable by ICMP ECHO requests; there are
	 * environments (such as Microsoft Azure) where ICMP ECHO is blocked but it
	 * is possible to route ordinary UDP packets.
	 *
	 * @param address
	 *            Where should be pinged.
	 * @return 0 on success, other values on failure (reflecting the result of
	 *         the OS subprocess).
	 */
	@CheckReturnValue
	public static int ping(String address) {
		int i = 0;
		while (true) {
			int result = ping1(address);
			if (result == 0 || ++i >= PING_COUNT) {
				return result;
			}
			try {
				sleep(PING_DELAY);
			} catch (InterruptedException e) {
				return result;
			}
		}
	}

	/**
	 * Pings to detect if a host or IP address is reachable. May wait for up to
	 * about five seconds (or longer <i>in extremis</i>). Technically, it only
	 * detects if a host is reachable by ICMP ECHO requests; there are
	 * environments (such as Microsoft Azure) where ICMP ECHO is blocked but it
	 * is possible to route ordinary UDP packets.
	 *
	 * @param address
	 *            Where should be pinged.
	 * @return 0 on success, other values on failure (reflecting the result of
	 *         the OS subprocess).
	 */
	@CheckReturnValue
	public static int ping(InetAddress address) {
		return ping(address.getHostAddress());
	}
}
