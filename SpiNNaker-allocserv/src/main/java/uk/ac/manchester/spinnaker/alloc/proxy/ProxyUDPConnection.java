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

import static java.lang.Thread.currentThread;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;

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

	private final Runnable emergencyRemove;

	private final ByteBuffer workingBuffer;

	private final String name;

	/** The number of <em>successfully</em> sent packets. */
	private long sendCount;

	/** The number of <em>successfully</em> received packets. */
	private long receiveCount;

	ProxyUDPConnection(WebSocketSession session, InetAddress remoteHost,
			int remotePort, int id, Runnable emergencyRemove)
			throws IOException {
		super(null, null, remoteHost, remotePort);
		this.session = session;
		this.emergencyRemove = emergencyRemove;
		workingBuffer = allocate(WORKING_BUFFER_SIZE).order(LITTLE_ENDIAN);
		// Fixed header for this particular connection
		workingBuffer.putInt(ProxyOp.MESSAGE.ordinal());
		workingBuffer.putInt(id);
		// Make the name now so it remains useful after close()
		name = session.getUri() + "#" + id + " " + remoteHost + "/"
				+ remotePort;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return The wrapped message bytes, or {@link Optional#empty()} if the
	 *         socket timed out (not an error!)
	 */
	@Override
	public Optional<ByteBuffer> receiveMessage(int timeout) throws IOException {
		try {
			// Raw buffer, including header bytes
			ByteBuffer msg = receive(timeout);
			receiveCount++;
			return Optional.of(msg);
		} catch (SocketTimeoutException e) {
			return Optional.empty();
		}
	}

	/**
	 * Send a message on this connection.
	 *
	 * @param msg
	 *            The message to send.
	 * @throws IOException
	 *             If sending fails.
	 */
	public void sendMessage(ByteBuffer msg) throws IOException {
		send(msg);
		sendCount++;
	}

	protected void writeCountsToLog() {
		log.info("{} message counts: sent {} received {}", name, sendCount,
				receiveCount);
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Core SpiNNaker message receive and dispatch-to-websocket loop.
	 */
	protected void receiverTask() {
		Thread me = currentThread();
		String oldThreadName = me.getName();
		me.setName("ws/udp " + name);
		log.debug("launched listener {}", name);
		try {
			mainLoop();
		} catch (IOException e) {
			try {
				close();
				emergencyRemove.run();
			} catch (IOException e1) {
				e.addSuppressed(e1);
			}
			log.warn("problem in SpiNNaker-to-client part of {}", name, e);
		} finally {
			log.debug("shutting down listener {}", name);
			me.setName(oldThreadName);
		}
	}

	private void mainLoop() throws IOException {
		while (!isClosed()) {
			while (!isReadyToReceive(TIMEOUT)) {
				if (!session.isOpen() || isClosed()) {
					return;
				}
			}
			Optional<ByteBuffer> msg = receiveMessage(TIMEOUT);
			if (!msg.isPresent()) {
				// Timeout; go round the loop again.
				continue;
			}
			handleReceivedMessage(msg.get());
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
		log.debug("{} received message {}", name, msg);
		ByteBuffer outgoing = workingBuffer.duplicate();
		outgoing.put(msg);
		outgoing.flip();
		session.sendMessage(new BinaryMessage(outgoing));
	}

	/**
	 * Core SpiNNaker message receive and dispatch-to-websocket loop.
	 * @param recvFrom
	 *            What hosts we are allowed to receive messages from.
	 *            Messages from elsewhere will be discarded.
	 */
	protected void eieioReceiverTask(Set<InetAddress> recvFrom) {
		Thread me = currentThread();
		String oldThreadName = me.getName();
		me.setName("ws/udp " + name);
		log.debug("launched listener {}", name);
		try {
			mainLoop(recvFrom);
		} catch (IOException e) {
			try {
				close();
				emergencyRemove.run();
			} catch (IOException e1) {
				e.addSuppressed(e1);
			}
			log.warn("problem in SpiNNaker-to-client part of {}", name, e);
		} finally {
			log.debug("shutting down listener {}", name);
			me.setName(oldThreadName);
		}
	}

	private void mainLoop(Set<InetAddress> recvFrom) throws IOException {
		while (!isClosed()) {
			while (!isReadyToReceive(TIMEOUT)) {
				if (!session.isOpen() || isClosed()) {
					return;
				}
			}
			DatagramPacket packet;
			try {
				packet = receiveWithAddress(TIMEOUT);
			} catch (SocketTimeoutException e) {
				// Timeout; go round the loop again.
				continue;
			}
			if (!recvFrom.contains(packet.getAddress())) {
				continue;
			}
			ByteBuffer msg = wrap(packet.getData(), 0,
					packet.getLength()).order(LITTLE_ENDIAN);
			handleReceivedMessage(msg);
		}
	}
}
