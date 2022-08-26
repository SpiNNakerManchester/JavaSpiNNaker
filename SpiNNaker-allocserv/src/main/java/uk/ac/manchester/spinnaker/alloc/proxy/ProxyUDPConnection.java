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
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.connections.UDPConnection.TrafficClass.IPTOS_THROUGHPUT;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.connections.UDPPacket;

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
			int remotePort, int id, Runnable emergencyRemove,
			InetAddress localHost)
			throws IOException {
		super(localHost, null, remoteHost, remotePort, IPTOS_THROUGHPUT);
		this.session = session;
		this.emergencyRemove = emergencyRemove;
		workingBuffer = allocate(WORKING_BUFFER_SIZE).order(LITTLE_ENDIAN);
		// Fixed header for this particular connection
		workingBuffer.putInt(ProxyOp.MESSAGE.ordinal());
		workingBuffer.putInt(id);
		// Make the name now so it remains useful after close()
		if (remoteHost == null) {
			name = session.getUri() + "#" + id + " ANY/ANY";
		} else {
			name = session.getUri() + "#" + id + " " + remoteHost + "/"
					+ remotePort;
		}
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

	/**
	 * Send a message on this connection.
	 *
	 * @param msg
	 *            The message to send.
	 * @param addr
	 *            Where to send to.
	 * @param port
	 *            What port to send to.
	 * @throws IOException
	 *             If sending fails.
	 */
	public void sendMessage(ByteBuffer msg, InetAddress addr, int port)
			throws IOException {
		sendTo(msg, addr, port);
		sendCount++;
	}

	/**
	 * Writes the count of messages sent and received on this connection to the
	 * log.
	 */
	protected void writeCountsToLog() {
		if (sendCount > 0 || receiveCount > 0) {
			log.info("{} message counts: sent {} received {}", name, sendCount,
					receiveCount);
		}
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Core SpiNNaker message receive and dispatch-to-websocket loop.
	 */
	protected void connectedReceiverTask() {
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
			Optional<ByteBuffer> msg = receiveMessage(TIMEOUT);
			if (!msg.isPresent()) {
				// Timeout; go round the loop again.
				if (!session.isOpen() || isClosed()) {
					return;
				}
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
	 * Core SpiNNaker message receive and dispatch-to-websocket loop for the
	 * type of connections required for EIEIO, especially for the live packet
	 * gatherer, which does a complex muxing and programs sockets/tags in a
	 * different way.
	 *
	 * @param recvFrom
	 *            What hosts we are allowed to receive messages from.
	 *            Messages from elsewhere will be discarded.
	 */
	protected void eieioReceiverTask(Set<InetAddress> recvFrom) {
		Thread me = currentThread();
		String oldThreadName = me.getName();
		me.setName("ws/udp(eieio) " + name);
		log.debug("launched eieio listener {}", name);
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
			log.debug("shutting down eieio listener {}", name);
			me.setName(oldThreadName);
		}
	}

	/**
	 * Loop at core of {@link #eieioReceiverTask(Set)}.
	 *
	 * @param msg
	 *            What hosts we are allowed to receive messages from. Messages
	 *            from elsewhere will be discarded.
	 * @throws IOException
	 *             If there is some sort of network problem.
	 */
	private void mainLoop(Set<InetAddress> recvFrom) throws IOException {
		while (!isClosed()) {
			UDPPacket packet;
			try {
				packet = receiveWithAddress(TIMEOUT);
			} catch (SocketTimeoutException e) {
				// Timeout; go round the loop again.
				if (!session.isOpen() || isClosed()) {
					return;
				}
				continue;
			}
			// SECURITY: drop any packet not from an allocated board
			if (!recvFrom.contains(packet.getAddress().getAddress())) {
				log.debug("dropped packet from {}", packet.getAddress());
				continue;
			}
			handleReceivedMessage(packet.getByteBuffer());
		}
	}
}
