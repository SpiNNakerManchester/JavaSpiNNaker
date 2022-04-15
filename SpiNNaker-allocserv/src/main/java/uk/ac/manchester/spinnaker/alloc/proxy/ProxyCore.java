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
import java.util.function.IntSupplier;

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
 * <td>Correlation&nbsp;ID
 * <td>Chip&nbsp;X
 * <td>Chip&nbsp;Y
 * <td>UDP&nbsp;Port on Chip
 * </tr>
 * </table>
 * </td>
 * <td><table border>
 * <tr>
 * <td>{@link ProxyOp#OPEN 0}
 * <td>Correlation&nbsp;ID
 * <td>Connection&nbsp;ID
 * </tr>
 * </table>
 * </td>
 * <td>Establish a UDP socket that will talk to the given Ethernet chip within
 * the allocation. Returns an ID that can be used to refer to that connection.
 * Note that opening a socket declares that you are prepared to receive messages
 * from SpiNNaker on it, but does not mean that SpiNNaker will send any messages
 * that way. The correlation ID is caller-nominated, and just passed back
 * uninterpreted in the response message.</td>
 * </tr>
 * <tr>
 * <td>{@linkplain #closeConnection(ByteBuffer) Close Connection}</td>
 * <td><table border>
 * <tr>
 * <td>{@link ProxyOp#CLOSE 1}
 * <td>Correlation&nbsp;ID
 * <td>Connection&nbsp;ID
 * </tr>
 * </table>
 * </td>
 * <td><table border>
 * <tr>
 * <td>{@link ProxyOp#CLOSE 1}
 * <td>Correlation&nbsp;ID
 * <td>Connection&nbsp;ID (if closed) or {@code 0} (not closed)
 * </tr>
 * </table>
 * </td>
 * <td>Close an established UDP socket, given its ID. Returns the ID on success,
 * and zero on failure (e.g., because the socket is already closed). The
 * correlation ID is caller-nominated, and just passed back uninterpreted in the
 * response message.</td>
 * </tr>
 * <tr>
 * <td>{@linkplain #sendMessage(ByteBuffer) Send Message}</td>
 * <td><table border>
 * <tr>
 * <td>{@link ProxyOp#MESSAGE 2}
 * <td>Connection&nbsp;ID
 * <td>Raw&nbsp;message&nbsp;bytes...
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

	private static final int RESPONSE_WORDS = 3;

	private final WebSocketSession session;

	private final Map<ChipLocation, InetAddress> hosts = new HashMap<>();

	private final Map<Integer, ProxyUDPConnection> conns = new HashMap<>();

	private final IntSupplier idIssuer;

	private final ThreadGroup threadGroup;

	/**
	 * @param s
	 *            The websocket session.
	 * @param connections
	 *            What boards may this session talk to.
	 * @param threadGroup
	 *            Where to group the worker threads.
	 * @param idIssuer
	 *            Provides connection IDs. These will never be zero.
	 */
	ProxyCore(WebSocketSession s, List<ConnectionInfo> connections,
			ThreadGroup threadGroup, IntSupplier idIssuer) {
		session = s;
		this.threadGroup = threadGroup;
		this.idIssuer = idIssuer;
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

	private static ByteBuffer response(ProxyOp op, int correlationId) {
		ByteBuffer msg =
				allocate(RESPONSE_WORDS * WORD_SIZE).order(LITTLE_ENDIAN);
		msg.putInt(op.ordinal());
		msg.putInt(correlationId);
		return msg;
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
		// This method handles message parsing/assembly and validation
		int corId = message.getInt();

		int x = message.getInt();
		int y = message.getInt();
		InetAddress who = getTargetHost(x, y);

		int port = message.getInt();
		if (port < 1 || port > MAX_PORT) {
			throw new IllegalArgumentException("bad port number");
		}

		int id = openConnection(who, port);

		ByteBuffer msg = response(ProxyOp.OPEN, corId);
		msg.putInt(id);
		return msg;
	}

	/**
	 * Open a connection.
	 *
	 * @param who
	 *            What board IP address are we connecting to?
	 * @param port
	 *            What port are we connecting to?
	 * @return The connection ID.
	 * @throws IOException
	 *             If the proxy connection can't be opened.
	 */
	private int openConnection(InetAddress who, int port) throws IOException {
		// This method actually makes a connection and listener thread
		int id = idIssuer.getAsInt();
		ProxyUDPConnection conn = new ProxyUDPConnection(session, who, port, id,
				() -> removeConnection(id));
		setConnection(id, conn);

		// Start sending messages received from the board
		Thread t = new Thread(threadGroup, conn::receiveLoop,
				"WS handler for " + who);
		t.setDaemon(true);
		t.start();

		log.info("opened proxy connection {}:{} to {}:{}", session, id, who,
				port);
		return id;
	}

	private InetAddress getTargetHost(int x, int y) {
		InetAddress who = hosts.get(new ChipLocation(x, y));
		if (who == null) {
			throw new IllegalArgumentException("unrecognised ethernet chip");
		}
		return who;
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
		int corId = message.getInt();
		int id = message.getInt();
		ByteBuffer msg = response(ProxyOp.CLOSE, corId);
		msg.putInt(closeConnection(id));
		return msg;
	}

	private int closeConnection(int id) throws IOException {
		ProxyUDPConnection conn = removeConnection(id);
		if (!isValid(conn)) {
			return 0;
		}
		conn.close();
		// Thread will shut down now that the proxy is closed
		log.info("closed proxy connection {}:{}", session, id);
		return id;
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
		log.debug("got message for channel {}", id);
		ProxyUDPConnection conn = getConnection(id);
		if (isValid(conn)) {
			ByteBuffer payload = message.slice();
			log.debug("sending message to {} of length {}", conn,
					payload.remaining());
			conn.send(payload);
		}
		return null;
	}

	private static boolean isValid(ProxyUDPConnection conn) {
		return conn != null && !conn.isClosed();
	}

	private void setConnection(int id, ProxyUDPConnection conn) {
		synchronized (conns) {
			conns.put(id, conn);
		}
	}

	private ProxyUDPConnection getConnection(int id) {
		synchronized (conns) {
			return conns.get(id);
		}
	}

	private ProxyUDPConnection removeConnection(int id) {
		synchronized (conns) {
			return conns.remove(id);
		}
	}

	private ArrayList<ProxyUDPConnection> listConnections() {
		synchronized (conns) {
			return new ArrayList<>(conns.values());
		}
	}

	@Override
	public void close() {
		// Take a copy immediately
		ArrayList<ProxyUDPConnection> copy = listConnections();
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
