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
package uk.ac.manchester.spinnaker.utils;

import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

/**
 * How to ping a host to test for (ICMP) network connectivity.
 *
 * @author Donal Fellows
 */
public abstract class Ping {
	private static final int PING_DELAY = 500;

	private static final int PING_COUNT = 10;

	private static final int BUFFER_SIZE = 256;

	private Ping() {
	}

	/**
	 * Core ping operation.
	 *
	 * @param address
	 *            Where to ping
	 * @return Return code, or -1 on total failure
	 */
	private static int ping1(String address) {
		ProcessBuilder cmd;
		if (getProperty("os.name").toLowerCase().contains("win")) {
			cmd = new ProcessBuilder("ping", "-n", "1", "-w", "1", address);
		} else {
			cmd = new ProcessBuilder("ping", "-c", "1", "-W", "1", address);
		}
		cmd.redirectErrorStream(true);
		try {
			Process process = cmd.start();
			new InputStreamDrain(process.getInputStream());
			return process.waitFor();
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Pings to detect if a host or IP address is reachable. May wait for up to
	 * about five seconds (or longer <i>in extremis</i>). Technically, it only
	 * detects if a host is reachable by ICMP ECHO requests.
	 *
	 * @param address
	 *            Where should be pinged.
	 * @return 0 on success, other values on failure (reflecting the result of
	 *         the OS subprocess).
	 */
	public static int ping(String address) {
		int result = -1;
		int i = 0;
		while (true) {
			result = ping1(address);
			if (result == 0 || ++i >= PING_COUNT) {
				break;
			}
			try {
				sleep(PING_DELAY);
			} catch (InterruptedException e) {
				break;
			}
		}
		return result;
	}

	/**
	 * Pings to detect if a host or IP address is reachable. May wait for up to
	 * about five seconds (or longer <i>in extremis</i>). Technically, it only
	 * detects if a host is reachable by ICMP ECHO requests.
	 *
	 * @param address
	 *            Where should be pinged.
	 * @return 0 on success, other values on failure (reflecting the result of
	 *         the OS subprocess).
	 */
	public static int ping(InetAddress address) {
		return ping(address.getHostAddress());
	}

	private static class InputStreamDrain implements Runnable {
		private InputStream is;

		InputStreamDrain(InputStream is) {
			this.is = is;
			Thread t = new Thread(this);
			t.setDaemon(true);
			t.start();
		}

		@Override
		public void run() {
			try {
				try {
					byte[] b = new byte[BUFFER_SIZE];
					int read;
					do {
						read = is.read(b);
					} while (read >= 0);
				} finally {
					is.close();
				}
			} catch (IOException e) {
				// Ignore this exception
			}
		}
	}
}
