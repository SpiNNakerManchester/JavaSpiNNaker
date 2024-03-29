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
package uk.ac.manchester.spinnaker.alloc.proxy;

import static java.net.InetAddress.getByName;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.socket.CloseStatus.BAD_DATA;
import static org.springframework.web.socket.CloseStatus.SERVER_ERROR;
import static uk.ac.manchester.spinnaker.alloc.proxy.ProxyOp.CLOSE;
import static uk.ac.manchester.spinnaker.alloc.proxy.ProxyOp.OPEN_UNCONNECTED;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.KILOBYTE;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.IntSupplier;

import org.slf4j.Logger;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.alloc.model.ConnectionInfo;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * The main proxy class for a particular web socket session. It's bound to a
 * job, which should be running at the time that the web socket is opened.
 *
 * @author Donal Fellows
 */
public class ProxyCore implements AutoCloseable {
	private static final Logger log = getLogger(ProxyCore.class);

	private static final int MAX_PORT = 65535;

	private static final int RESPONSE_WORDS = 2;

	private static final int ID_WORDS = 1;

	private static final int IP_ADDR_AND_PORT_WORDS = 2;

	/**
	 * Max amount of time to wait to send a message on the websocket, in
	 * milliseconds.
	 */
	private static final int SEND_TIME_LIMIT_MS = 10 * MSEC_PER_SEC;

	/**
	 * Max amount of space (in bytes) that may be used to buffer messages
	 * waiting to be sent on the websocket.
	 */
	private static final int SEND_BUFFER_SIZE = 512 * KILOBYTE;

	private final WebSocketSession session;

	private final Map<ChipLocation, InetAddress> hosts = new HashMap<>();

	@GuardedBy("itself")
	private final Map<Integer, ProxyUDPConnection> conns = new HashMap<>();

	private final Set<InetAddress> recvFrom;

	private final IntSupplier idIssuer;

	private final Executor executor;

	private final boolean writeCounts;

	private final InetAddress localHost;

	/**
	 * @param s
	 *            The websocket session.
	 * @param connections
	 *            What boards may this session talk to.
	 * @param executor
	 *            What runs the worker tasks.
	 * @param idIssuer
	 *            Provides connection IDs. These will never be zero.
	 * @param writeCounts
	 *            Whether to write the number of messages sent and received on
	 *            the proxied connections to the log.
	 * @param localHost
	 *            The local address for sockets talking to the machines. If
	 *            {@code null}, opening a general receiver socket will not
	 *            work.
	 */
	ProxyCore(WebSocketSession s, List<ConnectionInfo> connections,
			Executor executor, IntSupplier idIssuer, boolean writeCounts,
			InetAddress localHost) {
		session = new ConcurrentWebSocketSessionDecorator(s, SEND_TIME_LIMIT_MS,
				SEND_BUFFER_SIZE);
		this.executor = executor;
		this.idIssuer = idIssuer;
		this.writeCounts = writeCounts;
		this.localHost = localHost;
		for (var ci : connections) {
			try {
				hosts.put(ci.getChip(), getByName(ci.getHostname()));
			} catch (UnknownHostException e) {
				log.warn("unexpectedly unknown board address: {}",
						ci.getHostname(), e);
			}
		}
		recvFrom = Set.copyOf(hosts.values());
	}

	@FunctionalInterface
	private interface Impl {
		ByteBuffer call(ByteBuffer message) throws IOException;
	}

	private Impl decode(int opcode) {
		switch (ProxyOp.values()[opcode]) {
		case OPEN:
			return this::openConnectedChannel;
		case CLOSE:
			return this::closeChannel;
		case MESSAGE:
			return this::sendMessage;
		case OPEN_UNCONNECTED:
			return this::openUnconnectedChannel;
		case MESSAGE_TO:
			return this::sendMessageTo;
		default:
			log.warn("unexpected proxy opcode: {}", opcode);
			throw new IllegalArgumentException("bad opcode");
		}
	}

	/**
	 * Handle a message sent to this service on the web socket associated with
	 * this object.
	 *
	 * @param message
	 *            The content of the message.
	 * @throws IOException
	 *             Not expected; implementations don't actually throw
	 */
	public final void handleClientMessage(ByteBuffer message)
			throws IOException {
		try {
			var impl = decode(message.getInt());
			var reply = impl != null ? impl.call(message) : null;
			if (reply != null) {
				session.sendMessage(new BinaryMessage(reply.flip()));
			}
		} catch (IOException e) {
			log.error("Closing data on server error", e);
			session.close(SERVER_ERROR);
		} catch (IllegalArgumentException | BufferUnderflowException e) {
			log.error("Closing session on bad data", e);
			session.close(BAD_DATA);
		}
	}

	private static ByteBuffer response(ProxyOp op, int correlationId,
			int additionalWords) {
		int nWords = RESPONSE_WORDS + additionalWords;
		var msg = allocate(nWords * WORD_SIZE).order(LITTLE_ENDIAN);
		msg.putInt(op.ordinal());
		msg.putInt(correlationId);
		return msg;
	}

	private static final int MAX_EXN_LENGTH = 1000;

	private static void assertEndOfMessage(ByteBuffer message) {
		if (message.hasRemaining()) {
			throw new IllegalArgumentException("extra content ("
					+ message.remaining() + " bytes) supplied in message");
		}
	}

	private static ByteBuffer composeError(int corId, Exception ex) {
		var msg = ex.getMessage();
		while (true) {
			if (msg == null) {
				msg = "";
			}
			var msgBytes = msg.getBytes(UTF_8);
			if (msgBytes.length > MAX_EXN_LENGTH) {
				msg = null;
				continue;
			}
			var data = allocate(WORD_SIZE + WORD_SIZE + msgBytes.length);
			data.order(LITTLE_ENDIAN);
			data.putInt(ProxyOp.ERROR.ordinal());
			data.putInt(corId);
			data.put(msgBytes);
			return data;
		}
	}

	/**
	 * Open a connected channel in response to a {@link ProxyOp#OPEN} message.
	 * Note that no control over the local (to the service) port number or
	 * address is provided, nor is a mechanism given to easily make available
	 * what address is used (though it can be obtained from the IPTag).
	 *
	 * @param message
	 *            The message received. The initial 4-byte type code will have
	 *            been already read out of the buffer.
	 * @return The response message to send, in the bytes leading up to the
	 *         position. The caller will {@linkplain ByteBuffer#flip() flip} the
	 *         message. Underlying failures that are not due to outright
	 *         protocol abuse will be reported to users as a
	 *         {@link ProxyOp#ERROR} message.
	 * @throws IllegalArgumentException
	 *             If insufficient or too many arguments are supplied.
	 */
	protected ByteBuffer openConnectedChannel(ByteBuffer message) {
		// This method handles message parsing/assembly and validation
		int corId = message.getInt();
		int x = message.getInt();
		int y = message.getInt();
		int port = message.getInt();
		assertEndOfMessage(message);

		try {
			var who = getTargetHost(x, y);
			validatePort(port);

			int id = openConnected(who, port);

			var msg = response(ProxyOp.OPEN, corId, ID_WORDS);
			msg.putInt(id);
			return msg;
		} catch (Exception e) {
			log.error("failed to open connected channel", e);
			return composeError(corId, e);
		}
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
	private int openConnected(InetAddress who, int port) throws IOException {
		// This method actually makes a connection and listener thread
		int id = idIssuer.getAsInt();
		var conn = new ProxyUDPConnection(session, who, port, id,
				() -> removeConnection(id), null);
		setConnection(id, conn);

		// Start sending messages received from the board
		executor.execute(conn::connectedReceiverTask);

		log.info("opened proxy connected channel {}:{} to {}:{}", session, id,
				who, port);
		return id;
	}

	private InetAddress getTargetHost(int x, int y) {
		var who = hosts.get(new ChipLocation(x, y));
		if (who == null) {
			throw new IllegalArgumentException("unrecognised ethernet chip");
		}
		return who;
	}

	/**
	 * Open an unconnected channel in response to a
	 * {@link ProxyOp#OPEN_UNCONNECTED} message. Note that no control over the
	 * local (to the service) port number or address is provided, but the IP
	 * address and port opened are in the return message.
	 *
	 * @param message
	 *            The message received. The initial 4-byte type code will have
	 *            been already read out of the buffer.
	 * @return The response message to send, in the bytes leading up to the
	 *         position. The caller will {@linkplain ByteBuffer#flip() flip} the
	 *         message. Underlying failures that are not due to outright
	 *         protocol abuse will be reported to users as a
	 *         {@link ProxyOp#ERROR} message.
	 * @throws IllegalArgumentException
	 *             If insufficient or too many arguments are supplied.
	 */
	protected ByteBuffer openUnconnectedChannel(ByteBuffer message) {
		// This method handles message parsing/assembly and validation
		int corId = message.getInt();
		assertEndOfMessage(message);

		try {
			var localAddress = new ValueHolder<InetAddress>();
			var localPort = new ValueHolder<Integer>();
			int id = openUnconnected(localAddress, localPort);
			var addr = localAddress.getValue().getAddress();
			log.debug("Unconnected channel local address: {}", addr);

			var msg = response(OPEN_UNCONNECTED, corId,
					ID_WORDS + IP_ADDR_AND_PORT_WORDS);
			msg.putInt(id);
			msg.put(addr);
			msg.putInt(localPort.getValue());
			return msg;
		} catch (Exception e) {
			log.error("failed to open unconnected channel", e);
			return composeError(corId, e);
		}
	}

	private int openUnconnected(ValueHolder<InetAddress> localAddress,
			ValueHolder<Integer> localPort) throws IOException {
		int id = idIssuer.getAsInt();
		var conn = new ProxyUDPConnection(session, null, 0, id,
				() -> removeConnection(id), localHost);
		setConnection(id, conn);
		var who = conn.getLocalIPAddress();
		int port = conn.getLocalPort();

		// Start sending messages received from the board
		executor.execute(() -> conn.eieioReceiverTask(recvFrom));

		log.info("opened proxy unconnected channel {}:{} from {}:{}", session,
				id, who, port);
		// Arrange for values to be sent out
		localAddress.setValue(who);
		localPort.setValue(port);
		return id;
	}

	/**
	 * Close a channel in response to a {@link ProxyOp#CLOSE} message. It's not
	 * an error to close a channel twice
	 *
	 * @param message
	 *            The message received. The initial 4-byte type code will have
	 *            been already read out of the buffer.
	 * @return The response message to send, in the bytes leading up to the
	 *         position. The caller will {@linkplain ByteBuffer#flip() flip} the
	 *         message. Underlying failures that are not due to outright
	 *         protocol abuse will be reported to users as a
	 *         {@link ProxyOp#ERROR} message.
	 * @throws IllegalArgumentException
	 *             If insufficient or too many arguments are supplied.
	 */
	protected ByteBuffer closeChannel(ByteBuffer message) {
		int corId = message.getInt();
		int id = message.getInt();
		assertEndOfMessage(message);
		try {
			var msg = response(CLOSE, corId, ID_WORDS);
			msg.putInt(closeChannel(id));
			return msg;
		} catch (Exception e) {
			log.error("failed to close channel", e);
			return composeError(corId, e);
		}
	}

	private int closeChannel(int id) throws IOException {
		@SuppressWarnings("resource")
		var conn = removeConnection(id);
		if (!isValid(conn)) {
			return 0;
		}
		conn.close();
		// Thread will shut down now that the proxy is closed
		log.debug("closed proxy channel {}:{}", session, id);
		if (writeCounts) {
			conn.writeCountsToLog();
		}
		return id;
	}

	/**
	 * Send a message on a channel in response to a {@link ProxyOp#MESSAGE}
	 * message. It's not an error to send on a non-existant or closed channel.
	 *
	 * @param message
	 *            The message received. The initial 4-byte type code will have
	 *            been already read out of the buffer.
	 * @return The response message to send, in the bytes leading up to the
	 *         position. The caller will {@linkplain ByteBuffer#flip() flip} the
	 *         message. If {@code null}, no response will be sent (expected case
	 *         for this operation!)
	 * @throws IOException
	 *             If the proxy connection can't be used.
	 */
	protected ByteBuffer sendMessage(ByteBuffer message) throws IOException {
		int id = message.getInt();
		log.trace("got message for channel {}", id);
		var conn = getConnection(id);
		if (isValid(conn) && conn.getRemoteIPAddress() != null) {
			var payload = message.slice();
			log.trace("sending message to {} of length {}", conn,
					payload.remaining());
			conn.sendMessage(payload);
		}
		return null;
	}

	/**
	 * Send a message to a particular destination on a channel in response to a
	 * {@link ProxyOp#MESSAGE_TO} message. It's not an error to send on a
	 * non-existent or closed channel. It is an error to use this operation on a
	 * channel that has a bound remote host address.
	 *
	 * @param message
	 *            The message received. The initial 4-byte type code will have
	 *            been already read out of the buffer.
	 * @return The response message to send, in the bytes leading up to the
	 *         position. The caller will {@linkplain ByteBuffer#flip() flip} the
	 *         message. If {@code null}, no response will be sent (expected case
	 *         for this operation!)
	 * @throws IOException
	 *             If the proxy connection can't be used.
	 * @throws IllegalArgumentException
	 *             If the target doesn't exist in the job, the port number is
	 *             out of range, or the channel has a bound address.
	 */
	protected ByteBuffer sendMessageTo(ByteBuffer message) throws IOException {
		int id = message.getInt();
		int x = message.getInt();
		int y = message.getInt();
		var who = getTargetHost(x, y);

		int port = message.getInt();
		validatePort(port);

		log.trace("got message for channel {} for {}:{}", id, who, port);
		var conn = getConnection(id);
		if (isValid(conn)) {
			if (conn.getRemoteIPAddress() != null) {
				throw new IllegalArgumentException("channel is connected");
			}
			var payload = message.slice();
			log.trace("sending message to {} of length {}", conn,
					payload.remaining());
			conn.sendMessage(payload, who, port);
		}
		return null;
	}

	private static boolean isValid(ProxyUDPConnection conn) {
		return conn != null && !conn.isClosed();
	}

	private static void validatePort(int port) {
		if (port < 1 || port > MAX_PORT) {
			throw new IllegalArgumentException("bad port number");
		}
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

	private List<ProxyUDPConnection> listConnections() {
		synchronized (conns) {
			return List.copyOf(conns.values());
		}
	}

	@Override
	public void close() {
		// Take a copy immediately
		var copy = listConnections();
		for (var conn : copy) {
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
