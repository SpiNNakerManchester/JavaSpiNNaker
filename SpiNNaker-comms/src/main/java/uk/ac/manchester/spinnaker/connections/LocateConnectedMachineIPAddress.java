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
package uk.ac.manchester.spinnaker.connections;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.System.out;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.HashSet;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;

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
	 *            current time. Note that each board is only reported once.
	 * @throws Exception
	 *             If anything goes wrong
	 */
	public static void locateConnectedMachine(Handler handler)
			throws Exception {
		try (var connection = new IPAddressConnection()) {
			var seenBoards = new HashSet<>();
			while (true) {
				var ipAddress = connection.receiveMessage();
				var now = Calendar.getInstance();
				if (ipAddress != null && !seenBoards.contains(ipAddress)) {
					seenBoards.add(ipAddress);
					if (handler.handle(ipAddress, now)) {
						break;
					}
				}
			}
		}
	}

	/**
	 * The type of callbacks used to report where a board has been seen.
	 *
	 * @see LocateConnectedMachineIPAddress#locateConnectedMachine(Handler)
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	public interface Handler {
		/**
		 * Called to notify the handler about a SpiNNaker board.
		 *
		 * @param address
		 *            Where the board is
		 * @param timestamp
		 *            When it sent the notification
		 * @return True if we should stop receiving.
		 * @throws Exception
		 *             If anything goes wrong
		 */
		boolean handle(InetAddress address, Calendar timestamp)
				throws Exception;
	}

	@FormatMethod
	private static void print(@FormatString String formatString,
			Object... args) {
		out.println(format(formatString, args));
	}

	/**
	 * A little program that listens for, and prints, the pre-boot messages
	 * published by SpiNNaker boards.
	 *
	 * @param args
	 *            ignored
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public static void main(String... args) throws Exception {
		print("The following addresses might be SpiNNaker boards"
				+ " (press Ctrl-C to quit):");
		getRuntime().addShutdownHook(new Thread(() -> {
			print("Exiting");
		}));
		locateConnectedMachine((addr, time) -> {
			print("%s (%s) at %s", addr, addr.getCanonicalHostName(), time);
			return false;
		});
	}
}
