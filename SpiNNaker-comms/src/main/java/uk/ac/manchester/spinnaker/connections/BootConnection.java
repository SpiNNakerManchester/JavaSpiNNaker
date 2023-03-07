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
package uk.ac.manchester.spinnaker.connections;

import static java.lang.Thread.sleep;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static uk.ac.manchester.spinnaker.connections.UDPConnection.TrafficClass.IPTOS_RELIABILITY;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_BOOT_CONNECTION_DEFAULT_PORT;

import java.io.IOException;
import java.net.InetAddress;

import uk.ac.manchester.spinnaker.messages.boot.BootMessage;

/** A connection to the SpiNNaker board that uses UDP for booting. */
public class BootConnection extends UDPConnection<BootMessage> {
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
	 * Create a boot connection where the mechanism for sending and receiving
	 * messages is being overridden by a subclass.
	 * @param remoteHost
	 *            The remote host to send packets to. If {@code null}, the
	 *            socket will be available for listening only, and will throw
	 *            and exception if used for sending
	 * @param remotePort
	 *            The remote port to send packets to. If {@code null}, a default
	 *            value is used.
	 *
	 * @throws IOException
	 *             If anything goes wrong with socket manipulation.
	 * @hidden
	 */
	public BootConnection(InetAddress remoteHost, Integer remotePort)
			throws IOException {
		super(remoteHost, remotePort == null
				? UDP_BOOT_CONNECTION_DEFAULT_PORT : remotePort);
	}

	@Override
	public BootMessage receiveMessage(int timeout)
			throws IOException, InterruptedException {
		return new BootMessage(receive(timeout));
	}

	/**
	 * Sends a SpiNNaker boot message using this connection.
	 *
	 * @param bootMessage
	 *            The message to be sent
	 * @throws IOException
	 *             If there is an error sending the message
	 */
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
