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

import java.io.IOException;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.HashSet;
import java.util.function.BiPredicate;

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
	 *            A predicate that decides whether to stop searching (we stop if
	 *            the predicate returns true). The predicate is given two
	 *            arguments: the IP address found and the current time. Note
	 *            that each board is only reported once.
	 * @throws IOException
	 *             If anything goes wrong
	 */
	public static void locateConnectedMachine(
			BiPredicate<InetAddress, Calendar> handler) throws IOException {
		try (var connection = new IPAddressConnection()) {
			var seenBoards = new HashSet<>();
			while (true) {
				var ipAddress = connection.receiveMessage();
				var now = Calendar.getInstance();
				if (ipAddress != null && !seenBoards.contains(ipAddress)) {
					seenBoards.add(ipAddress);
					if (handler.test(ipAddress, now)) {
						break;
					}
				}
			}
		}
	}

	/**
	 * A little program that listens for, and prints, the pre-boot messages
	 * published by SpiNNaker boards.
	 *
	 * @param args
	 *            ignored
	 * @throws IOException
	 *             if anything goes wrong.
	 */
	public static void main(String... args) throws IOException {
		System.out.format("The following addresses might be SpiNNaker boards"
				+ " (press Ctrl-C to quit):%n");
		getRuntime().addShutdownHook(
				new Thread(LocateConnectedMachineIPAddress::goodbye));
		locateConnectedMachine(LocateConnectedMachineIPAddress::printHost);
	}

	private static boolean printHost(InetAddress addr, Calendar time) {
		System.out.format("%s (%s) at %s%n", addr, addr.getCanonicalHostName(),
				time);
		return false;
	}

	private static void goodbye() {
		System.out.format("Exiting%n");
	}
}
