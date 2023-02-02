/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.errorprone.annotations.DoNotCall;

import uk.ac.manchester.spinnaker.connections.BootConnection;
import uk.ac.manchester.spinnaker.connections.UDPPacket;

/** A boot connection that routes messages across the proxy. */
final class ProxiedBootConnection extends BootConnection {
	/** The port of the connection. */
	private static final int BOOT_PORT = 54321;

	private final ProxyProtocolClient.ConnectedChannel channel;

	private final BlockingQueue<ByteBuffer> receiveQueue;

	private ProxyProtocolClient ws;

	/**
	 * @param ws
	 *            The proxy handle.
	 * @param remoteHost
	 *            The remote host name or IP address to send packets to. If
	 *            {@code null}, the socket will be available for listening only,
	 *            and will throw an exception if used for sending.
	 * @param remotePort
	 *            The remote port to send packets to. If {@code remoteHost} is
	 *            {@code null}, this is ignored. If {@code remoteHost} is
	 *            specified, this must also be specified as non-zero for the
	 *            connection to allow sending.
	 * @throws IOException
	 *             If we couldn't finish setting up our networking.
	 * @throws InterruptedException
	 *             If interrupted while things were setting up.
	 */
	ProxiedBootConnection(ProxyProtocolClient ws, InetAddress remoteAddr,
			Integer remotePort)
			throws IOException, InterruptedException {
		super(remoteAddr, remotePort);
		this.ws = requireNonNull(ws);
		receiveQueue = new LinkedBlockingQueue<>();
		channel = ws.openChannel(ZERO_ZERO, BOOT_PORT, receiveQueue);
	}

	@Override
	@SuppressWarnings("MissingSuperCall")
	public void close() throws IOException {
		channel.close();
		ws = null;
	}

	@Override
	public boolean isClosed() {
		return isNull(ws) || !ws.isOpen();
	}

	@Override
	public boolean isConnected() {
		return !isClosed();
	}

	@Override
	protected void doSend(ByteBuffer buffer) throws IOException {
		channel.send(buffer);
	}

	@Override
	@DoNotCall
	protected void doSendTo(ByteBuffer data, InetAddress address, int port) {
		throw new UnsupportedOperationException(
				"sendTo() not supported by this connection type");
	}

	@Override
	protected ByteBuffer doReceive(int timeout)
			throws IOException, InterruptedException {
		if (isClosed() && receiveQueue.isEmpty()) {
			throw new EOFException("connection closed");
		}
		return ClientUtils.receiveHelper(receiveQueue, timeout);
	}

	@Override
	@DoNotCall
	protected UDPPacket doReceiveWithAddress(int timeout) {
		throw new UnsupportedOperationException(
				"receiveWithAddress() not supported by this connection type");
	}
}
