/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
