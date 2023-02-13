/*
 * Copyright (c) 2022-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

import static java.util.Objects.isNull;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.errorprone.annotations.DoNotCall;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.UDPPacket;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/** An SCP connection that routes messages across the proxy. */
final class ProxiedSCPConnection extends SCPConnection {
	/** The port of the connection. */
	private static final int SCP_SCAMP_PORT = 17893;

	private final ProxyProtocolClient.ConnectedChannel channel;

	private final BlockingQueue<ByteBuffer> receiveQueue;

	private ProxyProtocolClient ws;

	/**
	 * @param chip
	 *            Which ethernet chip in the job are we talking to?
	 * @param ws
	 *            The proxy handle.
	 * @throws IOException
	 *             If we couldn't finish setting up our networking.
	 * @throws InterruptedException
	 *             If interrupted while things were setting up.
	 */
	ProxiedSCPConnection(ChipLocation chip, ProxyProtocolClient ws)
			throws IOException, InterruptedException {
		super(chip);
		this.ws = ws;
		receiveQueue = new LinkedBlockingQueue<>();
		channel = ws.openChannel(chip, SCP_SCAMP_PORT, receiveQueue);
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
	protected void doSendTo(ByteBuffer buffer, InetAddress addr, int port)
			throws IOException {
		throw new UnsupportedOperationException();
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

	/**
	 * Close this connection eventually. Actually processes it immediately
	 * because the other end of the proxy makes it eventual.
	 */
	@Override
	public void closeEventually() {
		closeAndLogNoExcept();
	}
}
