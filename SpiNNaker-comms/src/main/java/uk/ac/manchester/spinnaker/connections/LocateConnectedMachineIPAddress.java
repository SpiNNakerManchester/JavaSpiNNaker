package uk.ac.manchester.spinnaker.connections;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.System.out;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Locate any SpiNNaker machines IP addresses from the auto-transmitted packets
 * from non-booted SpiNNaker machines.
 */
public abstract class LocateConnectedMachineIPAddress {
	private LocateConnectedMachineIPAddress() {
	}

	/**
	 * Locates any SpiNNaker machines IP addresses from the auto-transmitted
	 * packets from non-booted SpiNNaker machines.
	 *
	 * @param handler
	 *            A callback that decides whether to stop searching. The
	 *            callback is given two arguments: the IP address found and the
	 *            current time.
	 * @throws Exception
	 *             If anything goes wrong
	 */
	public static void locateConnectedMachine(Handler handler)
			throws Exception {
		try (IPAddressConnection connection = new IPAddressConnection()) {
			Set<InetAddress> seen_boards = new HashSet<>();
			while (true) {
				InetAddress ip_address = connection.receiveIPAddress();
				Calendar now = Calendar.getInstance();
				if (ip_address != null && !seen_boards.contains(ip_address)) {
					seen_boards.add(ip_address);
					if (handler.handle(ip_address, now)) {
						break;
					}
				}
			}
		}
	}

	@FunctionalInterface
	public interface Handler {
		boolean handle(InetAddress address, Calendar timestamp)
				throws Exception;
	}

	private static void print(String formatString, Object... args) {
		out.println(format(formatString, args));
	}

	public static void main(String... args) throws Exception {
		print("The following addresses might be SpiNNaker boards"
				+ " (press Ctrl-C to quit):");
		getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				print("Exiting");
			}
		});
		locateConnectedMachine((addr, time) -> {
			print("%s (%s) at %s", addr, addr.getCanonicalHostName(), time);
			return false;
		});
	}
}
