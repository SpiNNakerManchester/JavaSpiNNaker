/*
 * Copyright (c) 2018-2022 The University of Manchester
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

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.connections.UDPConnection.TrafficClass.IPTOS_RELIABILITY;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.notification.AbstractNotificationMessage;
import uk.ac.manchester.spinnaker.messages.notification.NotificationMessage;

/**
 * A UDP connection for sending and receiving notification protocol messages.
 * These messages go between programs on host; they <em>do not</em> go to and
 * from SpiNNaker and are not constrained by SpiNNaker's message size rules.
 */
public class NotificationConnection
		extends UDPConnection<NotificationMessage> {
	private static final int NOTIFICATION_MESSAGE_BUFFER_SIZE = 65535;

	/**
	 * Create a notification protocol connection only available for listening,
	 * using default local port.
	 *
	 * @param localHost
	 *            The local IP address to bind to.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public NotificationConnection(InetAddress localHost) throws IOException {
		super(localHost, null, null, null, null);
		setReceivePacketSize(NOTIFICATION_MESSAGE_BUFFER_SIZE);
	}

	/**
	 * Create a notification protocol connection only available for listening.
	 *
	 * @param localHost
	 *            The local IP address to bind to.
	 * @param localPort
	 *            The local port to bind to, 0 or between 1025 and 65535.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public NotificationConnection(InetAddress localHost, Integer localPort)
			throws IOException {
		super(localHost, localPort, null, null, null);
		setReceivePacketSize(NOTIFICATION_MESSAGE_BUFFER_SIZE);
	}

	/**
	 * Create a notification protocol connection that is bound to a particular
	 * remote location (where the toolchain should be running).
	 *
	 * @param localHost
	 *            The local host to bind to. If not specified, it defaults to
	 *            binding to all interfaces, unless {@code remoteHost} is
	 *            specified, in which case binding is done to the IP address
	 *            that will be used to send packets.
	 * @param localPort
	 *            The local port to bind to, 0 or between 1025 and 65535.
	 * @param remoteHost
	 *            The remote host to send packets to. If not specified, the
	 *            socket will be available for listening only, and will throw
	 *            and exception if used for sending.
	 * @param remotePort
	 *            The remote port to send packets to. If remoteHost is
	 *            {@code null}, this is ignored. If remoteHost is specified,
	 *            this must also be specified as non-zero for the connection to
	 *            allow sending.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public NotificationConnection(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort) throws IOException {
		super(localHost, localPort, remoteHost, remotePort, IPTOS_RELIABILITY);
		setReceivePacketSize(NOTIFICATION_MESSAGE_BUFFER_SIZE);
	}

	/**
	 * @return Get a new little-endian buffer sized suitably for notification
	 *         messages.
	 */
	private static ByteBuffer newMessageBuffer() {
		return allocate(NOTIFICATION_MESSAGE_BUFFER_SIZE).order(LITTLE_ENDIAN);
	}

	/**
	 * Sends a notification message down this connection.
	 *
	 * @param notificationMessage
	 *            The notification message to be sent
	 * @throws IOException
	 *             If there is an error sending the message
	 */
	public void sendNotification(NotificationMessage notificationMessage)
			throws IOException {
		var b = newMessageBuffer();
		notificationMessage.addToBuffer(b);
		b.flip();
		send(b);
	}

	@Override
	public NotificationMessage receiveMessage(int timeout)
			throws IOException, InterruptedException {
		var b = receive(timeout);
		return AbstractNotificationMessage.build(b);
	}
}
