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
package uk.ac.manchester.spinnaker.connections.model;

import java.net.InetAddress;

/**
 * Indicates a resource class that holds a network socket and that can answer
 * basic questions about it.
 *
 * @author Donal Fellows
 */
public interface SocketHolder {
	/**
	 * @return the local (host) IP address of the socket. Expected to be an IPv4
	 *         address when talking to SpiNNaker.
	 */
	InetAddress getLocalIPAddress();

	/**
	 * @return the local (host) port of the socket.
	 */
	int getLocalPort();

	/**
	 * @return the remote (board) IP address of the socket. Expected to be an
	 *         IPv4 address when talking to SpiNNaker.
	 */
	InetAddress getRemoteIPAddress();

	/**
	 * @return the remote (board) port of the socket.
	 */
	int getRemotePort();

	/**
	 * Convert a timeout into a primitive type.
	 *
	 * @param timeout
	 *            The timeout in milliseconds, or {@code null} to wait
	 *            "forever".
	 * @return The primitive timeout.
	 */
	default int convertTimeout(Integer timeout) {
		if (timeout == null) {
			/*
			 * "Infinity" is nearly 25 days, which is a very long time to wait
			 * for any message from SpiNNaker.
			 */
			return Integer.MAX_VALUE;
		}
		return timeout.intValue();
	}
}
