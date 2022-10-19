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
package uk.ac.manchester.spinnaker.allocator;

import static java.util.Objects.isNull;

import java.io.EOFException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.errorprone.annotations.DoNotCall;

import uk.ac.manchester.spinnaker.connections.EIEIOConnection;
import uk.ac.manchester.spinnaker.connections.UDPPacket;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

final class ProxiedEIEIOListenerConnection extends EIEIOConnection {
	private final Map<Inet4Address, ChipLocation> hostToChip;

	private final BlockingQueue<ByteBuffer> receiveQueue;

	private ProxyProtocolClient ws;

	private ProxyProtocolClient.UnconnectedChannel channel;

	ProxiedEIEIOListenerConnection(Map<Inet4Address, ChipLocation> hostToChip,
			ProxyProtocolClient proxy)
			throws InterruptedException {
		this.hostToChip = hostToChip;
		this.ws = proxy;
		receiveQueue = new LinkedBlockingQueue<>();
		channel = ws.openUnconnectedChannel(receiveQueue);
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
	@DoNotCall
	protected void doSend(ByteBuffer buffer) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void doSendTo(ByteBuffer buffer, InetAddress addr, int port)
			throws IOException {
		channel.send(hostToChip.get(addr), port, buffer);
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
