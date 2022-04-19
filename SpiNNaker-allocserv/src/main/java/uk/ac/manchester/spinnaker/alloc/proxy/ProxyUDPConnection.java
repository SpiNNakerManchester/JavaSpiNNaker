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
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import uk.ac.manchester.spinnaker.connections.UDPConnection;

/**
 * The low-level handler for proxy connections.
 *
 * @author Donal Fellows
 */
public class ProxyUDPConnection extends UDPConnection<Optional<ByteBuffer>> {
	private static final Logger log = getLogger(ProxyUDPConnection.class);

	private static final int TIMEOUT = 1000;

	// Plenty of room; SpiNNaker messages are quite a bit smaller than this
	// Even boot messages are smaller; this is bigger than an Ethernet packet
	private static final int WORKING_BUFFER_SIZE = 2048;

	private final WebSocketSession session;

	/**
	 * The ID. Visible so we can remove the connection from the set of
	 * connections elsewhere.
	 */
	private final int id;

	private final Runnable emergencyRemove;

	private final ByteBuffer workingBuffer;

	private final String name;

	ProxyUDPConnection(WebSocketSession session, InetAddress remoteHost,
			int remotePort, int id, Runnable emergencyRemove)
			throws IOException {
		super(null, null, remoteHost, remotePort);
		this.session = session;
		this.id = id;
		this.emergencyRemove = emergencyRemove;
		workingBuffer = allocate(WORKING_BUFFER_SIZE).order(LITTLE_ENDIAN);
		// Fixed header for this particular connection
		workingBuffer.putInt(ProxyOp.MESSAGE.ordinal());
		workingBuffer.putInt(id);
		// Get the name now so it remains useful after close()
		name = toString();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return The wrapped message bytes, or {@link Optional#empty()} if the
	 *         socket timed out (not an error!)
	 */
	@Override
	public Optional<ByteBuffer> receiveMessage(int timeout)
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
		log.info("launched listener {} for channel {}", name, id);
		try {
			mainLoop:
			while (!isClosed()) {
				while (!isReadyToReceive(TIMEOUT)) {
					if (!session.isOpen() || isClosed()) {
						break mainLoop;
					}
				}
				Optional<ByteBuffer> msg = receiveMessage(TIMEOUT);
				if (!msg.isPresent()) {
					// Timeout; go round the loop again.
					continue;
				}
				handleReceivedMessage(msg.get());
			}
		} catch (IOException e) {
			try {
				close();
				emergencyRemove.run();
			} catch (IOException e1) {
				e.addSuppressed(e1);
			}
			log.warn("problem in SpiNNaker-to-client part of {}/{}", name, id,
					e);
		} finally {
			log.info("shutting down listener {} for channel {}", name, id);
		}
	}

	/**
	 * Process a received message, forwarding it to the client.
	 *
	 * @param msg
	 *            The received message, positioned at the point of the payload.
	 * @throws IOException
	 *             If the message can't be sent.
	 */
	private void handleReceivedMessage(ByteBuffer msg) throws IOException {
		log.debug("{}/{} received message {}", name, id, msg);
		ByteBuffer outgoing = workingBuffer.duplicate();
		outgoing.put(msg);
		outgoing.flip();
		session.sendMessage(new BinaryMessage(outgoing));
	}
}
