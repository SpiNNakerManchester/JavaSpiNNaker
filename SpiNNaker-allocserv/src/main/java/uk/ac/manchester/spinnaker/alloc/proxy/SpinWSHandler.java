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
import static java.util.concurrent.Executors.newCachedThreadPool;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.proxy.Utils.getFieldFromTemplate;

import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
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

	private static final String JOB = SpinWSHandler.class + ":job";

	private static final String PROXY = SpinWSHandler.class + ":proxy";

	@Autowired
	private SpallocAPI spallocCore;

	private ExecutorService executor;

	private ConnectionIDIssuer idIssuer = new ConnectionIDIssuer();

	// TODO move to proper configuration bean
	@Value("${spalloc.proxy.writeCounts:true}")
	private boolean writeCounts;

	public SpinWSHandler() {
		ThreadGroup group = new ThreadGroup("ws/udp workers");
		executor = newCachedThreadPool(r -> {
			Thread t = new Thread(group, r, "ws/udp worker");
			t.setDaemon(true);
			return t;
		});
	}

	/** The path that we match in this handler. */
	public static final String PATH = "proxy/{id:\\d+}";

	/** The name of the field in the {@link #PATH}. */
	private static final String ID = "id";

	/** The {@link #PATH} as a template. */
	private final UriTemplate template = new UriTemplate(PATH);

	// -----------------------------------------------------------
	// Satisfy the APIs that we use to plug into Spring

	@PreDestroy
	private void stopPool() {
		/*
		 * The threads inside don't need to be explicitly stopped as they're all
		 * daemon threads.
		 */
		executor.shutdown();
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) {
		return lookUpJobFromPath(request).map(job -> {
			// If we have a job, remember it and succeed
			attributes.put(JOB, job);
			return job;
		}).isPresent();
	}

	@Override
	public void afterHandshake(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception exception) {
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		initProxyCore(session, attr(session, JOB));
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session,
			CloseStatus status) {
		closed(session, attr(session, PROXY), attr(session, JOB));
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session,
			BinaryMessage message) throws Exception {
		delegateToProxy(message, attr(session, PROXY));
	}

	@Override
	public void handleTransportError(WebSocketSession session,
			Throwable exception) throws Exception {
		if (!(exception instanceof EOFException)) {
			// We don't log EOFException
			log.warn("transport error for {}", session, exception);
		}
		// Don't need to close; afterConnectionClosed() will be called next
	}

	// -----------------------------------------------------------
	// General implementation methods

	private Optional<Job> lookUpJobFromPath(ServerHttpRequest request) {
		return getFieldFromTemplate(template, request.getURI(), ID)
				// Convert to integer
				.flatMap(Utils::parseInteger)
				// IDs are only ever valid if positive
				.filter(Utils::positive)
				// Do the lookup of the job
				.flatMap(this::getJob);
	}

	/**
	 * How to look up the job. Note that the underlying object is security
	 * aware, so we need the authorisation step.
	 *
	 * @param jobId
	 *            The job identifier
	 * @return The job, if one is known and the current user is allowed to read
	 *         details of it (owner or admin).
	 */
	private Optional<Job> getJob(int jobId) {
		Permit permit = new Permit(SecurityContextHolder.getContext());
		// How to look up a job; the permit is needed!
		return permit.authorize(() -> spallocCore.getJob(permit, jobId));
	}

	@SuppressWarnings("unchecked")
	private static <T> T attr(WebSocketSession session, String key) {
		// Fetch something from the attributes and auto-cast it
		return (T) session.getAttributes().get(key);
	}

	/**
	 * Connection established and job looked up. Make a proxy.
	 *
	 * @param session
	 *            The Websocket session
	 * @param job
	 *            The job extracted from the websocket path
	 * @throws NotFound
	 *             If the job ID can't be mapped to a job
	 * @throws RequestFailedException
	 *             If the job doesn't have an allocated machine
	 */
	protected final void initProxyCore(WebSocketSession session, Job job) {
		ProxyCore proxy = job.getMachine()
				.map(machine -> new ProxyCore(session, machine.getConnections(),
						executor, idIssuer::issueId, writeCounts))
				.orElseThrow(
						() -> new RequestFailedException(SERVICE_UNAVAILABLE,
								"job not in state where proxying permitted"));
		session.getAttributes().put(PROXY, proxy);
		job.rememberProxy(proxy);
		log.info("user {} has web socket {} connected for job {}",
				session.getPrincipal(), session, job.getId());
	}

	/**
	 * Connection closed.
	 *
	 * @param session
	 *            The Websocket session
	 * @param proxy
	 *            The proxy handler for our custom protocol
	 * @param job
	 *            What job we are working for
	 */
	protected final void closed(WebSocketSession session, ProxyCore proxy,
			Job job) {
		if (proxy != null) {
			proxy.close();
			job.forgetProxy(proxy);
		}
		log.info("user {} has disconnected web socket {}",
				session.getPrincipal(), session);
	}

	/**
	 * Message was sent to us.
	 *
	 * @param message
	 *            The body of the message.
	 * @param proxy
	 *            The socket proxy will be handling the message
	 * @throws IOException
	 *             If the proxy fails to handle the message in a bad way
	 */
	protected final void delegateToProxy(BinaryMessage message, ProxyCore proxy)
			throws IOException {
		proxy.handleClientMessage(message.getPayload().order(LITTLE_ENDIAN));
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

abstract class Utils {
	private Utils() {
	}

	/**
	 * Match the template against the path of the URI and extract a field.
	 *
	 * @param template
	 *            The template
	 * @param uri
	 *            The URI
	 * @param key
	 *            The name of the field to extract
	 * @return The content of the field, if present. Otherwise
	 *         {@link Optional#empty()}.
	 */
	static Optional<String> getFieldFromTemplate(UriTemplate template,
			URI uri, String key) {
		Map<String, String> templateResults = template.match(uri.getPath());
		if (templateResults == null) {
			return Optional.empty();
		}
		String val = templateResults.get(key);
		return Optional.ofNullable(val);
	}

	/**
	 * Parse a string as a decimal integer.
	 *
	 * @param val
	 *            The string to parse.
	 * @return The integer, or {@link Optional#empty()} if the parse fails.
	 */
	static Optional<Integer> parseInteger(String val) {
		try {
			return Optional.of(parseInt(val));
		} catch (NumberFormatException ignored) {
			// Do nothing here
		}
		return Optional.empty();
	}

	static boolean positive(int n) {
		return n > 0;
	}

	@SuppressWarnings("unused")
	private abstract static class Use {
		Use(NotFound q) {
		}
	}
}
