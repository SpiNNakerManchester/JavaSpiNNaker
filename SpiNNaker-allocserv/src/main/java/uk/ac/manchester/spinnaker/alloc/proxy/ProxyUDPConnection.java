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
package uk.ac.manchester.spinnaker.alloc.proxy;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Optional;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import uk.ac.manchester.spinnaker.connections.UDPConnection;

/**
 * The low-level handler for proxy connections.
 *
 * @author Donal Fellows
 */
public class ProxyUDPConnection extends UDPConnection<Optional<ByteBuffer>> {
	private static final int TIMEOUT = 1000;

	// Plenty of room; SpiNNaker messages are quite a bit smaller than this
	private static final int WORKING_BUFFER_SIZE = 1024;

	private final WebSocketSession session;

	private final int id;

	ProxyUDPConnection(WebSocketSession session, InetAddress remoteHost,
			int remotePort, int id) throws IOException {
		super(null, null, remoteHost, remotePort);
		this.session = session;
		this.id = id;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return The wrapped message bytes, or {@link Optional#empty()} if the
	 *         socket timed out (not an error!)
	 */
	@Override
	public Optional<ByteBuffer> receiveMessage(Integer timeout)
			throws IOException {
		try {
			// Raw buffer, including header bytes
			return Optional.of(receive(timeout));
		} catch (SocketTimeoutException e) {
			return Optional.empty();
		}
	}

	/**
	 * Core SpiNNaker message receive and dispatch-to-websocket loop.
	 */
	protected void receiveLoop() {
		ByteBuffer workingBuffer =
				allocate(WORKING_BUFFER_SIZE).order(LITTLE_ENDIAN);
		// Fixed header from this particular connection
		workingBuffer.putInt(ProxyOp.MESSAGE.ordinal());
		workingBuffer.putInt(id);

		try {
			while (!isClosed()) {
				Optional<ByteBuffer> msg = receiveMessage(TIMEOUT);
				if (!session.isOpen()) {
					break;
				}
				if (!msg.isPresent()) {
					// Timeout; go round the loop again.
					continue;
				}
				ByteBuffer outgoing = workingBuffer.duplicate();
				outgoing.put(msg.get());
				outgoing.flip();
				session.sendMessage(new BinaryMessage(outgoing));
			}
		} catch (IOException e) {
			try {
				// TODO remove from connections?
				close();
			} catch (IOException e1) {
				// TODO log this!
			}
		}
	}
}
