package uk.ac.manchester.spinnaker.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

/**
 * How to ping a host to test for (ICMP) network connectivity.
 *
 * @author Donal Fellows
 */
public abstract class Ping {
	private Ping() {
	}

	/**
	 * Pings to detect if a host or IP address is reachable. May wait for up to
	 * about a second. Technically, it only detects if a host is reachable by
	 * ICMP ECHO requests.
	 *
	 * @param address
	 *            Where should be pinged.
	 * @return 0 on success, other values on failure (reflecting the result of
	 *         the OS subprocess).
	 */
	public static int ping(String address) {
		ProcessBuilder cmd;
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
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
	 * about a second. Technically, it only detects if a host is reachable by
	 * ICMP ECHO requests.
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
		private static final int BUFFER_SIZE = 256;
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
