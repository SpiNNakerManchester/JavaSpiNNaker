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

import static java.lang.Integer.parseInt;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriTemplate;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.SubMachine;
import uk.ac.manchester.spinnaker.alloc.security.Permit;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException.NotFound;

/**
 * Initial handler for web sockets. Maps a particular websocket to a
 * {@linkplain ProxyCore proxy handler} that processes messages received and
 * sends messages the other way.
 *
 * @author Donal Fellows
 */
@Component
public class SpinWSHandler extends BinaryWebSocketHandler
		implements HandshakeInterceptor {
	private static final Logger log = getLogger(SpinWSHandler.class);

	private static final String NO_JOB = "0";

	private static final String JOB_ID = "job-id";

	@Autowired
	private SpallocAPI spallocCore;

	/** Maps a session to how we handle it locally. */
	private Map<WebSocketSession, ProxyCore> map = new HashMap<>();

	private WeakHashMap<WebSocketSession, Integer> onDeath =
			new WeakHashMap<>();

	private ThreadGroup threadGroup;

	private ConnectionIDIssuer idIssuer = new ConnectionIDIssuer();

	public SpinWSHandler() {
		threadGroup = new ThreadGroup("WebSocket proxy handlers");
	}

	/** The path that we match in this handler. */
	public static final String PATH = "proxy/{id:\\d+}";

	/** The {@link #PATH} as a template. */
	private final UriTemplate template = new UriTemplate(PATH);

	@Override
	public boolean beforeHandshake(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws Exception {
		int jobId = parseInt(template.match(request.getURI().getPath())
				.getOrDefault("id", NO_JOB));
		log.info("parsed {} with {} to get {}", request.getURI().getPath(),
				template, jobId);
		if (jobId > 0) {
			attributes.put(JOB_ID, jobId);
			return true;
		}
		return false;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception exception) {
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		Map<String, Object> attrs = session.getAttributes();
		if (attrs.containsKey(JOB_ID)) {
			initProxyCore(session, (Integer) attrs.get(JOB_ID));
			return;
		}
	}

	private Optional<Job> getJob(Permit permit, int jobId) {
		return permit.authorize(() -> spallocCore.getJob(permit, jobId));
	}

	/**
	 * Connection established, but must check that user can see this job.
	 * Fortunately that's easy, as we must retrieve the job to get the set of
	 * boards that we can proxy to.
	 *
	 * @param session
	 *            The Websocket session
	 * @param jobId
	 *            The job ID (not yet validated) extracted from the websocket
	 *            path
	 * @throws NotFound
	 *             If the job ID can't be mapped to a job
	 */
	protected void initProxyCore(WebSocketSession session, int jobId) {
		Job job = getJob(new Permit(session), jobId)
				.orElseThrow(() -> new NotFound("no such job"));
		SubMachine machine = job.getMachine().orElseThrow(
				() -> new RequestFailedException(SERVICE_UNAVAILABLE,
						"job not in state where proxying permitted"));
		ProxyCore proxy = new ProxyCore(session, machine.getConnections(),
				threadGroup, idIssuer::issueId);
		map.put(session, proxy);
		onDeath.put(session, jobId);
		job.rememberProxy(proxy);
		log.info("user {} has web socket {} connected for job {}",
				session.getPrincipal(), session, jobId);
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
			getJob(new Permit(session), id)
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

	/**
	 * Handles giving each proxy connection its own ID. We give them unique IDs
	 * so that if someone opens multiple websockets to the same job, they can't
	 * get the same ID for the connections underneath; that would be just too
	 * confusing!
	 */
	private static class ConnectionIDIssuer {
		private int id;

		/**
		 * Issue an ID.
		 *
		 * @return A new ID. Never zero.
		 */
		private synchronized int issueId() {
			int thisId;
			do {
				thisId = ++id;
			} while (thisId == 0);
			return thisId;
		}
	}
}
