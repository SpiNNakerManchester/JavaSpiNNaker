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
package uk.ac.manchester.spinnaker.connections.model;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.UDPPacket;

/**
 * An abstract connection to the SpiNNaker board over some medium. This resource
 * class holds a network socket (or a proxy for one) and can answer basic
 * questions about it.
 */
public interface Connection extends Closeable {
	/**
	 * Determines if the medium is connected at this point in time. Connected
	 * media are not {@linkplain #isClosed() closed}. Disconnected media might
	 * not be open.
	 *
	 * @return true if the medium is connected, false otherwise
	 * @throws IOException
	 *             If there is an error when determining the connectivity of the
	 *             medium.
	 */
	boolean isConnected() throws IOException;

	/**
	 * Determines if the medium is closed at this point in time. Closed media
	 * are not {@linkplain #isConnected() connected}. Open media might not be
	 * connected.
	 *
	 * @return true if the medium is closed, false otherwise
	 */
	boolean isClosed();

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

	/**
	 * Receive data from the connection.
	 *
	 * @param timeout
	 *            The timeout in milliseconds, or {@code null} to wait forever
	 * @return The data received, in a little-endian buffer
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If an error occurs receiving the data
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	ByteBuffer receive(Integer timeout)
			throws SocketTimeoutException, IOException, InterruptedException;

	/**
	 * Receive data from the connection along with the address where the data
	 * was received from.
	 *
	 * @param timeout
	 *            The timeout in milliseconds
	 * @return The datagram packet received; caller is responsible for only
	 *         accessing the valid part of the buffer.
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If an error occurs receiving the data
	 */
	UDPPacket receiveWithAddress(int timeout)
			throws SocketTimeoutException, IOException;

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If there is an error sending the data
	 * @throws IllegalStateException
	 *             If the data buffer doesn't hold a message; zero-length
	 *             messages are not supported!
	 */
	void send(ByteBuffer data) throws IOException;

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @param address
	 *            Where to send (must be non-{@code null})
	 * @param port
	 *            What port to send to (must be non-zero)
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If there is an error sending the data
	 * @throws IllegalStateException
	 *             If the data packet doesn't hold a real message; zero-length
	 *             messages are not supported!
	 */
	void sendTo(ByteBuffer data, InetAddress address, int port)
			throws IOException;
}
