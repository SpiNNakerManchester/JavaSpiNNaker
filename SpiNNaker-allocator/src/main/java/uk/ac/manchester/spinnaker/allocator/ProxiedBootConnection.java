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
import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import uk.ac.manchester.spinnaker.connections.BootConnection;

/** A boot connection that routes messages across the proxy. */
final class ProxiedBootConnection extends BootConnection {
	/** The port of the connection. */
	private static final int BOOT_PORT = 54321;

	private final ProxyProtocolClient.ConnectedChannel channel;

	private final BlockingQueue<ByteBuffer> received;

	private ProxyProtocolClient ws;

	/**
	 * @param ws
	 *            The proxy handle.
	 * @throws IOException
	 *             If we couldn't finish setting up our networking.
	 * @throws InterruptedException
	 *             If interrupted while things were setting up.
	 */
	ProxiedBootConnection(ProxyProtocolClient ws)
			throws IOException, InterruptedException {
		this.ws = requireNonNull(ws);
		received = new LinkedBlockingQueue<>();
		channel = ws.openChannel(ZERO_ZERO, BOOT_PORT, received::add);
	}

	@Override
	public void close() throws IOException {
		channel.close();
		ws = null;
	}

	@Override
	public boolean isClosed() {
		return isNull(ws) || !ws.isOpen();
	}

	@Override
	protected void doSend(ByteBuffer buffer) throws IOException {
		channel.send(buffer);
	}

	@Override
	protected ByteBuffer doReceive(int timeout)
			throws IOException {
		if (isClosed() && received.isEmpty()) {
			throw new EOFException("connection closed");
		}
		return ClientUtils.receiveHelper(received, timeout);
	}
}
