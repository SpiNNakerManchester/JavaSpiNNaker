/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

import static java.util.Objects.isNull;

import java.io.EOFException;
import java.io.IOException;
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
	private final Map<InetAddress, ChipLocation> hostToChip;

	private final BlockingQueue<ByteBuffer> receiveQueue;

	private ProxyProtocolClient ws;

	private ProxyProtocolClient.UnconnectedChannel channel;

	ProxiedEIEIOListenerConnection(Map<InetAddress, ChipLocation> hostToChip,
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
