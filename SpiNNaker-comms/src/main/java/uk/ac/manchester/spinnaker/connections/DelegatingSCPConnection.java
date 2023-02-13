/*
 * Copyright (c) 2018-2023 The University of Manchester
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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

/**
 * An SCP connection that actually delegates message sending and receiving to
 * another connection. Note that closing a delegating connection does nothing;
 * the socket is only closed when the real underlying connection is closed.
 *
 * @author Donal Fellows
 */
@SuppressWarnings("ForOverride")
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
	protected InetSocketAddress getLocalAddress() throws IOException {
		return delegate.getLocalAddress();
	}

	@Override
	protected InetSocketAddress getRemoteAddress() throws IOException {
		return delegate.getRemoteAddress();
	}

	@Override
	DatagramSocket initialiseSocket(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort,
			TrafficClass trafficClass) throws IOException {
		// We never initialise a socket of our own.
		return null;
	}

	@Override
	protected ByteBuffer doReceive(int timeout)
			throws SocketTimeoutException, IOException, InterruptedException {
		return delegate.doReceive(timeout);
	}

	@Override
	protected UDPPacket doReceiveWithAddress(int timeout)
			throws SocketTimeoutException, IOException {
		return delegate.doReceiveWithAddress(timeout);
	}

	@Override
	protected void doSend(ByteBuffer data) throws IOException {
		delegate.doSend(data);
	}

	@Override
	protected void doSendTo(ByteBuffer data, InetAddress address, int port)
			throws IOException {
		delegate.doSendTo(data, address, port);
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
	@SuppressWarnings("MissingSuperCall")
	public void close() throws IOException {
		// Do nothing
	}

	@Override
	public String toString() {
		return "Delegate(" + delegate + ")";
	}
}
