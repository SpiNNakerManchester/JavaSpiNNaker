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
	public Optional<ByteBuffer> receiveMessage(int timeout)
			throws IOException, InterruptedException {
		try {
			// Raw buffer, including header bytes
			var msg = receive(timeout);
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
		var me = currentThread();
		var oldThreadName = me.getName();
		me.setName("ws/udp " + name);
		log.debug("launched listener {}", name);
		try {
			mainLoop();
		} catch (IOException e) {
			if (!isClosed()) {
				try {
					close();
					emergencyRemove.run();
				} catch (IOException e1) {
					e.addSuppressed(e1);
				}
				log.warn("problem in SpiNNaker-to-client part of {}", name, e);
			}
		} catch (InterruptedException e) {
			// We've been interrupted, so we're done.
			return;
		} finally {
			log.debug("shutting down listener {}", name);
			me.setName(oldThreadName);
		}
	}

	private void mainLoop() throws IOException, InterruptedException {
		while (session.isOpen() && !isClosed()) {
			var msg = receiveMessage(TIMEOUT);
			if (msg.isPresent()) {
				handleReceivedMessage(msg.orElseThrow());
			} // Otherwise was a timeout; go round the loop again anyway.
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
		var outgoing = workingBuffer.duplicate();
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
		var me = currentThread();
		var oldThreadName = me.getName();
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
	 * @param recvFrom
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
