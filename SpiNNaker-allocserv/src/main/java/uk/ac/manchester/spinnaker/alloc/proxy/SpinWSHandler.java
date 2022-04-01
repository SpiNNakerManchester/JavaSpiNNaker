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
import static org.springframework.web.socket.CloseStatus.BAD_DATA;
import static org.springframework.web.socket.CloseStatus.SERVER_ERROR;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.model.ConnectionInfo;
import uk.ac.manchester.spinnaker.alloc.security.Permit;
import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

@Component
public class SpinWSHandler extends BinaryWebSocketHandler {
	private Map<WebSocketSession, SpinProxy> map;

	@Autowired
	private SpallocAPI spallocCore;

	private ThreadGroup threadGroup;

	public SpinWSHandler() {
		threadGroup = new ThreadGroup("WebSocket proxy handlers");
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		// Connection established, but need to check auth and session binding
		String[] parts = session.getUri().getPath().split("/");
		int jobId = Integer.parseInt(parts[parts.length - 1]);
		spallocCore.getJob(new Permit(session), jobId)
				.ifPresent(job -> job.getMachine().ifPresent(
						machine -> map.put(session, new SpinProxy(session,
								machine.getConnections(), threadGroup))));
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session,
			CloseStatus status) {
		SpinProxy p = map.remove(session);
		if (p != null) {
			p.close();
		}
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session,
			BinaryMessage message) throws Exception {
		SpinProxy p = map.get(session);
		if (p != null) {
			p.handleClientMessage(message.getPayload().order(LITTLE_ENDIAN));
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session,
			Throwable exception) throws Exception {
		SpinProxy p = map.remove(session);
		if (p != null) {
			p.close();
		}
	}
}

enum ProxyOp {
	/** Ask for a bidirectional connection to a board to be opened. */
	OPEN,
	/** Ask for a bidirectional connection to a board to be closed. */
	CLOSE,
	/** A message going to or from a board. Connection must be open already. */
	MESSAGE
}

class ProxyUDPConnection extends UDPConnection<Optional<ByteBuffer>> {
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

	void receiveLoop() {
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

class SpinProxy {
	private static final int MAX_PORT = 65535;

	private final WebSocketSession session;

	private final Map<ChipLocation, InetAddress> hosts = new HashMap<>();

	private final Map<Integer, ProxyUDPConnection> conns = new HashMap<>();

	private int count;

	private final ThreadGroup threadGroup;

	SpinProxy(WebSocketSession s, List<ConnectionInfo> connections,
			ThreadGroup threadGroup) {
		session = s;
		this.threadGroup = threadGroup;
		for (ConnectionInfo ci : connections) {
			try {
				hosts.put(ci.getChip(),
						InetAddress.getByName(ci.getHostname()));
			} catch (UnknownHostException e) {
				// TODO log this
			}
		}
	}

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

	private ByteBuffer openConnection(ByteBuffer message) throws IOException {
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
		ProxyUDPConnection conn =
				new ProxyUDPConnection(session, who, port, id);
		conns.put(id, conn);

		// Start sending messages received from the board
		Thread t = new Thread(threadGroup, conn::receiveLoop,
				"WS handler for " + who);
		t.setDaemon(true);
		t.start();

		ByteBuffer msg = allocate(2 * WORD_SIZE).order(LITTLE_ENDIAN);
		msg.putInt(ProxyOp.OPEN.ordinal());
		msg.putInt(id);
		return msg;
	}

	private ByteBuffer closeConnection(ByteBuffer message) throws IOException {
		Integer id = message.getInt();
		ProxyUDPConnection conn = conns.remove(id);
		ByteBuffer msg = allocate(2 * WORD_SIZE).order(LITTLE_ENDIAN);
		msg.putInt(ProxyOp.CLOSE.ordinal());
		if (conn != null && !conn.isClosed()) {
			conn.close();
			msg.putInt(id);
		}
		return msg;
		// Thread will shut down now that the proxy is closed
	}

	private ByteBuffer sendMessage(ByteBuffer message) throws IOException {
		Integer id = message.getInt();
		ProxyUDPConnection conn = conns.get(id);
		if (conn != null && !conn.isClosed()) {
			conn.send(message.slice());
		}
		return null;
	}

	void close() {
		// Take a copy immediately
		for (ProxyUDPConnection conn : new ArrayList<>(conns.values())) {
			if (conn != null && !conn.isClosed()) {
				try {
					conn.close();
				} catch (IOException e) {
					// Don't stop what we're doing; everything must go!
					// TODO Auto-generated catch block
				}
			}
		}
	}

}
