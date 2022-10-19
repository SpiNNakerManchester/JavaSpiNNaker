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

import static java.lang.Thread.sleep;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static uk.ac.manchester.spinnaker.connections.UDPConnection.TrafficClass.IPTOS_RELIABILITY;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_BOOT_CONNECTION_DEFAULT_PORT;

import java.io.IOException;
import java.net.InetAddress;

import uk.ac.manchester.spinnaker.connections.model.BootReceiver;
import uk.ac.manchester.spinnaker.connections.model.BootSender;
import uk.ac.manchester.spinnaker.messages.boot.BootMessage;

/** A connection to the SpiNNaker board that uses UDP to for booting. */
public class BootConnection extends UDPConnection<BootMessage>
		implements BootSender, BootReceiver {
	// Determined by Ethernet MTU, not by SDP buffer size
	private static final int BOOT_MESSAGE_SIZE = 1500;

	private static final int ANTI_FLOOD_DELAY = 100;

	/**
	 * Creates a boot connection.
	 *
	 * @param localHost
	 *            The local host to bind to. If {@code null} defaults to bind to
	 *            all interfaces, unless remoteHost is specified, in which case
	 *            binding is done to the IP address that will be used to send
	 *            packets.
	 * @param localPort
	 *            The local port to bind to, between 1025 and 32767. If
	 *            {@code null}, defaults to a random unused local port
	 * @param remoteHost
	 *            The remote host to send packets to. If {@code null}, the
	 *            socket will be available for listening only, and will throw
	 *            and exception if used for sending
	 * @param remotePort
	 *            The remote port to send packets to. If {@code null}, a default
	 *            value is used.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public BootConnection(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort) throws IOException {
		super(localHost, localPort, remoteHost,
				remotePort == null ? UDP_BOOT_CONNECTION_DEFAULT_PORT
						: remotePort, IPTOS_RELIABILITY);
	}

	/**
	 * Creates a boot connection that binds to all local interfaces on an
	 * arbitrary port from the range 1025 to 32767.
	 *
	 * @param remoteHost
	 *            The remote host to send packets to. If {@code null}, the
	 *            socket will be available for listening only, and will throw
	 *            and exception if used for sending
	 * @param remotePort
	 *            The remote port to send packets to. If {@code null}, a
	 *            default value is used.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public BootConnection(InetAddress remoteHost, Integer remotePort)
			throws IOException {
		super(null, null, remoteHost,
				remotePort == null ? UDP_BOOT_CONNECTION_DEFAULT_PORT
						: remotePort, IPTOS_RELIABILITY);
	}

	/**
	 * Create a boot connection where the mechanism for sending and receiving
	 * messages is being overridden by a subclass.
	 *
	 * @throws IOException
	 *             If anything goes wrong with socket manipulation.
	 */
	protected BootConnection() throws IOException {
		super(true);
	}

	@Override
	public BootMessage receiveMessage(int timeout)
			throws IOException, InterruptedException {
		return new BootMessage(receive(timeout));
	}

	@Override
	public void sendBootMessage(BootMessage bootMessage) throws IOException {
		var b = allocate(BOOT_MESSAGE_SIZE).order(BIG_ENDIAN);
		bootMessage.addToBuffer(b);
		b.flip();
		send(b);
		// Sleep between messages to avoid flooding the machine
		try {
			sleep(ANTI_FLOOD_DELAY);
		} catch (InterruptedException e) {
			throw new IOException("interrupted during anti-flood delay", e);
		}
	}
}
