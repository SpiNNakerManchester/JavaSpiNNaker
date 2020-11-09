/*
 * Copyright (c) 2020 The University of Manchester
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * An SCP connection that actually delegates message sending and receiving to
 * another connection. Note that closing a delegating connection does nothing;
 * the socket is only closed when the real underlying connection is closed.
 *
 * @author Donal Fellows
 */
public class DelegatingSCPConnection extends SCPConnection {
	private final SDPConnection delegate;

	/**
	 * Create a connection that delegates actual communications to another
	 * connection.
	 *
	 * @param connection
	 *            The connection to delegate to.
	 * @throws IOException
	 *             If anything goes wrong. (Unexpected)
	 */
	public DelegatingSCPConnection(SDPConnection connection)
			throws IOException {
		super(connection.getChip(), connection.getLocalIPAddress(),
				connection.getLocalPort(), connection.getRemoteIPAddress(),
				connection.getRemotePort());
		this.delegate = connection;
	}

	@Override
	InetSocketAddress getLocalAddress() throws IOException {
		return delegate.getLocalAddress();
	}

	@Override
	InetSocketAddress getRemoteAddress() throws IOException {
		return delegate.getRemoteAddress();
	}

	@Override
	DatagramChannel initialiseSocket(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort) throws IOException {
		// We never initialise a socket of our own.
		return null;
	}

	@Override
	ByteBuffer doReceive(Integer timeout)
			throws SocketTimeoutException, IOException {
		return delegate.doReceive(timeout);
	}

	@Override
	DatagramPacket doReceiveWithAddress(int timeout)
			throws SocketTimeoutException, IOException {
		return delegate.doReceiveWithAddress(timeout);
	}

	@Override
	void doSend(ByteBuffer data) throws IOException {
		delegate.doSend(data);
	}

	@Override
	void doSendTo(ByteBuffer data, InetAddress address, int port)
			throws IOException {
		delegate.doSendTo(data, address, port);
	}

	@Override
	boolean readyToReceive(int timeout) throws IOException {
		return delegate.readyToReceive(timeout);
	}

	@Override
	public boolean isConnected() {
		return delegate.isConnected();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	/**
	 * When we're delegating, closing is a no-op; the underlying channel has to
	 * be closed directly.
	 */
	@Override
	public void close() throws IOException {
		// Do nothing
	}

	@Override
	public String toString() {
		return "Delegate(" + delegate + ")";
	}
}
