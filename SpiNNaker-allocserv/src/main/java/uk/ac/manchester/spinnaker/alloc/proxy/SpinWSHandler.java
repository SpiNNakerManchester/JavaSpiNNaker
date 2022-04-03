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

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.security.Permit;

/**
 * Initial handler for web sockets. Maps a particular websocket to a
 * {@linkplain ProxyCore proxy handler} that processes messages received and
 * sends messages the other way.
 *
 * @author Donal Fellows
 */
@Component
public class SpinWSHandler extends BinaryWebSocketHandler {
	private static final Logger log = getLogger(SpinWSHandler.class);

	@Autowired
	private SpallocAPI spallocCore;

	/** Maps a session to how we handle it locally. */
	private Map<WebSocketSession, ProxyCore> map = new HashMap<>();

	private WeakHashMap<WebSocketSession, Integer> onDeath =
			new WeakHashMap<>();

	private ThreadGroup threadGroup;

	public SpinWSHandler() {
		threadGroup = new ThreadGroup("WebSocket proxy handlers");
	}

	private static final AntPathMatcher MATCHER = new AntPathMatcher();

	/**
	 * The path that we match in this handler.
	 */
	public static final String PATH = "/system/proxy/{id:\\d+}";

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		// Connection established, but need to check auth and session binding
		int jobId = Integer.parseInt(MATCHER
				.extractUriTemplateVariables(PATH, session.getUri().getPath())
				.get("id"));
		spallocCore.getJob(new Permit(session), jobId)
				.ifPresent(job -> job.getMachine().ifPresent(machine -> {
					ProxyCore p = new ProxyCore(session,
							machine.getConnections(), threadGroup);
					map.put(session, p);
					onDeath.put(session, jobId);
					job.rememberProxy(p);
					log.info("user {} has web socket {} connected for job {}",
							session.getPrincipal(), session, jobId);
				}));
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session,
			CloseStatus status) {
		ProxyCore p = map.remove(session);
		if (p != null) {
			p.close();
		}
		Integer id = onDeath.get(session);
		if (id != null) {
			spallocCore.getJob(new Permit(session), id)
					.ifPresent(job -> job.forgetProxy(p));
		}
		log.info("user {} has disconnected web socket {}",
				session.getPrincipal(), session);
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session,
			BinaryMessage message) throws Exception {
		ProxyCore p = map.get(session);
		if (p != null) {
			p.handleClientMessage(message.getPayload().order(LITTLE_ENDIAN));
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session,
			Throwable exception) throws Exception {
		log.warn("transport error for {}", session, exception);
		// Don't need to close; afterConnectionClosed() will be called next
	}
}
