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
import static org.springframework.web.socket.CloseStatus.BAD_DATA;
import static org.springframework.web.socket.CloseStatus.SERVER_ERROR;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import uk.ac.manchester.spinnaker.alloc.model.ConnectionInfo;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * The main proxy class for a particular web socket session. It's bound to a
 * job, which should be running at the time that the web socket is opened. The
 * protocol that is supported is a binary protocol that has three messages:
 * <table border>
 * <tr>
 * <th>Name</th>
 * <th>Request Layout (words)</th>
 * <th>Response Layout (words)</th>
 * <th>Meaning</th>
 * </tr>
 * <tr>
 * <td>{@linkplain #openConnection(ByteBuffer) Open Connection}</td>
 * <td><table border>
 * <tr>
 * <td>{@link ProxyOp#OPEN 0}
 * <td>Chip X
 * <td>Chip Y
 * <td>UDP Port on Chip
 * </tr>
 * </table>
 * </td>
 * <td><table border>
 * <tr>
 * <td>{@link ProxyOp#OPEN 0}
 * <td>Connection ID
 * </tr>
 * </table>
 * </td>
 * <td>Establish a UDP socket that will talk to the given Ethernet chip within
 * the allocation. Returns an ID that can be used to refer to that connection.
 * Note that opening a socket declares that you are prepared to receive messages
 * from SpiNNaker on it, but does not mean that SpiNNaker will send any messages
 * that way.</td>
 * </tr>
 * <tr>
 * <td>{@linkplain #closeConnection(ByteBuffer) Close Connection}</td>
 * <td><table border>
 * <tr>
 * <td>{@link ProxyOp#CLOSE 1}
 * <td>Connection ID
 * </tr>
 * </table>
 * </td>
 * <td><table border>
 * <tr>
 * <td>{@link ProxyOp#CLOSE 1}
 * <td>Connection ID (if closed)
 * </tr>
 * </table>
 * </td>
 * <td>Close an established UDP socket, given its ID. Returns the ID on success,
 * and an empty message on failure (e.g., because the socket is already
 * closed).</td>
 * </tr>
 * <tr>
 * <td>{@linkplain #sendMessage(ByteBuffer) Send Message}</td>
 * <td><table border>
 * <tr>
 * <td>{@link ProxyOp#MESSAGE 2}
 * <td>Connection ID
 * <td>Raw message bytes...
 * </tr>
 * </table>
 * </td>
 * <td>N/A</td>
 * <td>Send a message to SpiNNaker on a particular established UDP
 * configuration. This is technically one-way, but messages come back in the
 * same format (i.e., a 4 byte prefix to say that it is a message, and another 4
 * bytes to say what socket this is talking about). The raw message bytes
 * (<em>including</em> the half-word of ethernet frame padding) follow the
 * header.</td>
 * </tr>
 * </table>
 *
 * @author Donal Fellows
 */
public class ProxyCore implements AutoCloseable {
	private static final Logger log = getLogger(ProxyCore.class);

	private static final int MAX_PORT = 65535;

	private final WebSocketSession session;

	private final Map<ChipLocation, InetAddress> hosts = new HashMap<>();

	private final Map<Integer, ProxyUDPConnection> conns = new HashMap<>();

	private int count;

	private final ThreadGroup threadGroup;

	ProxyCore(WebSocketSession s, List<ConnectionInfo> connections,
			ThreadGroup threadGroup) {
		session = s;
		this.threadGroup = threadGroup;
		for (ConnectionInfo ci : connections) {
			try {
				hosts.put(ci.getChip(),
						InetAddress.getByName(ci.getHostname()));
			} catch (UnknownHostException e) {
				log.warn("unexpectedly unknown board address: {}",
						ci.getHostname(), e);
			}
		}
	}

	/**
	 * Handle a message sent to this service on the web socket associated with
	 * this object.
	 *
	 * @param message
	 *            The content of the message.
	 * @throws IOException
	 */
	public void handleClientMessage(ByteBuffer message) throws IOException {
		try {
			ByteBuffer reply;
			switch (ProxyOp.values()[message.getInt()]) {
			case OPEN:
				reply = openConnection(message);
				break;
			case CLOSE:
				reply = closeConnection(message);
				break;
			case MESSAGE:
				reply = sendMessage(message);
				break;
			default:
				reply = null;
			}
			if (reply != null) {
				reply.flip();
				session.sendMessage(new BinaryMessage(reply));
			}
		} catch (IOException e) {
			session.close(SERVER_ERROR);
		} catch (IllegalArgumentException | BufferUnderflowException e) {
			session.close(BAD_DATA);
		}
	}

	/**
	 * Open a connection. Note that no control over the local (to the service)
	 * port number or address is provided, nor is a mechanism given to easily
	 * make available what address is used (though it can be obtained from the
	 * IPTag).
	 *
	 * @param message
	 *            The message received. The initial 4-byte type code will have
	 *            been already read out of the buffer.
	 * @return The response message to send, in the bytes leading up to the
	 *         position. The caller will {@linkplain ByteBuffer#flip() flip} the
	 *         message. If {@code null}, no response will be sent.
	 * @throws IOException
	 *             If the proxy connection can't be opened.
	 * @throws IllegalArgumentException
	 *             If bad arguments are supplied.
	 */
	protected ByteBuffer openConnection(ByteBuffer message) throws IOException {
		int x = message.getInt();
		int y = message.getInt();
		int port = message.getInt();
		if (port < 1 || port > MAX_PORT) {
			throw new IllegalArgumentException("bad port number");
		}
		InetAddress who = hosts.get(new ChipLocation(x, y));
		if (who == null) {
			throw new IllegalArgumentException("unrecognised ethernet chip");
		}

		int id = ++count;
		ProxyUDPConnection conn = new ProxyUDPConnection(session, who, port, id,
				() -> removeDeadConnection(id));
		synchronized (conns) {
			conns.put(id, conn);
		}

		// Start sending messages received from the board
		Thread t = new Thread(threadGroup, conn::receiveLoop,
				"WS handler for " + who);
		t.setDaemon(true);
		t.start();

		log.info("opened proxy connection {}:{} to {}:{}", session, id, who,
				port);

		ByteBuffer msg = allocate(2 * WORD_SIZE).order(LITTLE_ENDIAN);
		msg.putInt(ProxyOp.OPEN.ordinal());
		msg.putInt(id);
		return msg;
	}

	/**
	 * Close a connection. It's not an error to close a connection twice
	 *
	 * @param message
	 *            The message received. The initial 4-byte type code will have
	 *            been already read out of the buffer.
	 * @return The response message to send, in the bytes leading up to the
	 *         position. The caller will {@linkplain ByteBuffer#flip() flip} the
	 *         message. If {@code null}, no response will be sent.
	 * @throws IOException
	 *             If the proxy connection can't be opened.
	 */
	protected ByteBuffer closeConnection(ByteBuffer message)
			throws IOException {
		Integer id = message.getInt();
		ProxyUDPConnection conn;
		synchronized (conns) {
			conn = conns.remove(id);
		}
		ByteBuffer msg = allocate(2 * WORD_SIZE).order(LITTLE_ENDIAN);
		msg.putInt(ProxyOp.CLOSE.ordinal());
		if (conn != null && !conn.isClosed()) {
			conn.close();
			msg.putInt(id);
			log.info("closed proxy connection {}:{}", session, id);
		}
		return msg;
		// Thread will shut down now that the proxy is closed
	}

	/**
	 * Send a message on a connection. It's not an error to send on a
	 * non-existant or closed connection.
	 *
	 * @param message
	 *            The message received. The initial 4-byte type code will have
	 *            been already read out of the buffer.
	 * @return The response message to send, in the bytes leading up to the
	 *         position. The caller will {@linkplain ByteBuffer#flip() flip} the
	 *         message. If {@code null}, no response will be sent (expected case
	 *         for this operation!)
	 * @throws IOException
	 *             If the proxy connection can't be opened.
	 */
	protected ByteBuffer sendMessage(ByteBuffer message) throws IOException {
		Integer id = message.getInt();
		ProxyUDPConnection conn;
		synchronized (conns) {
			conn = conns.get(id);
		}
		if (conn != null && !conn.isClosed()) {
			conn.send(message.slice());
		}
		return null;
	}

	private void removeDeadConnection(int id) {
		synchronized (conns) {
			conns.remove(id);
		}
	}

	@Override
	public void close() {
		// Take a copy immediately
		ArrayList<ProxyUDPConnection> copy;
		synchronized (conns) {
			copy = new ArrayList<>(conns.values());
		}
		for (ProxyUDPConnection conn : copy) {
			if (conn != null && !conn.isClosed()) {
				try {
					conn.close();
				} catch (IOException e) {
					// Don't stop what we're doing; everything must go!
				}
			}
		}
	}
}
