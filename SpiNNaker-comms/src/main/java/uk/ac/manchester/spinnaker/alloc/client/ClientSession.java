/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.IOUtils.readLines;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.client.ProxyProtocol.CLOSE;
import static uk.ac.manchester.spinnaker.alloc.client.ProxyProtocol.MSG;
import static uk.ac.manchester.spinnaker.alloc.client.ProxyProtocol.MSG_TO;
import static uk.ac.manchester.spinnaker.alloc.client.ProxyProtocol.OPEN;
import static uk.ac.manchester.spinnaker.alloc.client.ProxyProtocol.OPEN_U;
import static uk.ac.manchester.spinnaker.alloc.client.SpallocClientFactory.checkForError;
import static uk.ac.manchester.spinnaker.alloc.client.SpallocClientFactory.readJson;
import static uk.ac.manchester.spinnaker.alloc.client.SpallocClientFactory.writeForm;
import static uk.ac.manchester.spinnaker.utils.InetFactory.getByAddressQuietly;

import java.io.EOFException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.alloc.client.SpallocClient.SpallocException;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Manages the login session. This allows us to avoid the (heavy) cost of the
 * password hashing algorithm used, at least most of the time.
 *
 * @author Donal Fellows
 */
@UsedInJavadocOnly(URLConnection.class)
final class ClientSession implements Session {
	private static final Logger log = getLogger(ClientSession.class);

	private static final String HTTP_UNAUTHORIZED_MESSAGE =
			"Server returned HTTP response code: 401";

	private static final String COOKIE = "Cookie";

	private static final String SET_COOKIE = "Set-Cookie";

	private static final String SESSION_NAME = "JSESSIONID";

	private static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

	private static final URI LOGIN_FORM = URI.create("system/login.html");

	private static final URI LOGIN_HANDLER = URI.create("system/perform_login");

	private static final URI SPALLOC_ROOT = URI.create("srv/spalloc");

	/**
	 * RE to find a session handle in a {@code Set-Cookie} header.
	 * <p>
	 * Expression: {@code SESSIONID=([A-Z0-9]+);}
	 */
	private static final Pattern SESSION_ID_RE =
			Pattern.compile("JSESSIONID=([A-Z0-9]+);");

	/**
	 * RE to find a CSRF token in an HTML form.
	 * <p>
	 * Expression: {@code name="_csrf" value="([-a-z0-9]+)"}
	 */
	private static final Pattern CSRF_ID_RE =
			Pattern.compile("name=\"_csrf\" value=\"([-a-z0-9]+)\"");

	private final URI baseUri;

	private final String username;

	private final String password;

	private String session;

	private String csrfHeader;

	private String csrf;

	/**
	 * Create a session and log it in.
	 *
	 * @param baseUri
	 *            The service base URI. <em>Must</em> be absolute! Must end in a
	 *            {@code /} character! <em>Must not</em> include a username or
	 *            password!
	 * @param username
	 *            The username to use. Not {@code null}.
	 * @param password
	 *            The password to use. Not {@code null}.
	 * @throws IOException
	 *             If things go wrong.
	 */
	ClientSession(URI baseUri, String username, String password)
			throws IOException {
		this.baseUri = baseUri;
		this.username = username;
		this.password = password;
		// This does the actual logging in process
		renew(false);
	}

	/**
	 * Create a session and log it in.
	 *
	 * @param baseUri
	 *            The service base URI. <em>Must</em> be absolute! <em>Must
	 *            not</em> include a username or password!
	 * @param headers
	 *            The headers to use to authenticate. Not {@code null}.
	 * @param cookies
	 *            The cookies to use to authenticate. Not {@code null}.
	 * @throws IOException
	 *             If things go wrong.
	 */
	ClientSession(URI baseUri, Map<String, String> headers,
			Map<String, String> cookies) throws IOException {
		this.baseUri = baseUri;
		this.username = null;
		this.password = null;
		this.session = cookies.get(SESSION_NAME);
		this.csrf = headers.get(CSRF_HEADER_NAME);
		if (this.csrf != null) {
			this.csrfHeader = CSRF_HEADER_NAME;
		}
		if (session == null) {
			// This does the actual logging in process
			renew(false);
		}
	}

	private static HttpURLConnection createConnection(URI url)
			throws IOException {
		log.debug("will connect to {}", url);
		var c = (HttpURLConnection) url.toURL().openConnection();
		c.setUseCaches(false);
		return c;
	}

	@Override
	public HttpURLConnection connection(URI url, boolean forStateChange)
			throws IOException {
		var realUrl = baseUri.resolve(url);
		var c = createConnection(realUrl);
		authorizeConnection(c, forStateChange);
		return c;
	}

	@Override
	public HttpURLConnection connection(URI url, URI url2,
			boolean forStateChange) throws IOException {
		var realUrl = baseUri.resolve(url).resolve(url2);
		var c = createConnection(realUrl);
		authorizeConnection(c, forStateChange);
		return c;
	}

	@Override
	public HttpURLConnection connection(URI url, String url2,
			boolean forStateChange) throws IOException {
		var realUrl = baseUri.resolve(url).resolve(url2);
		var c = createConnection(realUrl);
		authorizeConnection(c, forStateChange);
		return c;
	}

	/**
	 * A websocket client that implements the Spalloc-proxied protocol.
	 */
	class ProxyProtocolClientImpl extends WebSocketClient
			implements ProxyProtocolClient {
		/** Correlation ID is always second 4-byte word. */
		private static final int CORRELATION_ID_POSITION = 4;

		/** Size of IPv4 address. SpiNNaker always uses IPv4. */
		private static final int INET_SIZE = 4;

		/**
		 * Where to put the response messages from a call. Keys are correlation
		 * IDs.
		 */
		private final Map<Integer, CompletableFuture<ByteBuffer>> replyHandlers;

		/** What channels are we remembering. Keys are channel IDs. */
		private final Map<Integer, ChannelBase> channels;

		private int correlationCounter;

		private Exception failure;

		/**
		 * @param uri
		 *            The address of the websocket.
		 */
		ProxyProtocolClientImpl(URI uri) {
			super(uri);
			replyHandlers = new HashMap<>();
			channels = new HashMap<>();
		}

		@Override
		public void onWebsocketPong(WebSocket conn, Framedata f) {
			log.debug("pong received");
		}

		private synchronized int issueCorrelationId() {
			return correlationCounter++;
		}

		/**
		 * Do a synchronous call. Only some proxy operations support this.
		 *
		 * @param message
		 *            The composed call message. The second word of this will be
		 *            changed to be the correlation ID; all protocol messages
		 *            that have correlated replies have a correlation ID at that
		 *            position.
		 * @return The payload of the response. The header (protocol message
		 *         type ID, correlation ID) will have been stripped from the
		 *         message body prior to returning.
		 * @throws InterruptedException
		 *             If interrupted waiting for a reply.
		 * @throws RuntimeException
		 *             If an unexpected exception occurs.
		 */
		ByteBuffer call(ByteBuffer message) throws InterruptedException {
			int correlationId = issueCorrelationId();
			var event = new CompletableFuture<ByteBuffer>();
			message.putInt(CORRELATION_ID_POSITION, correlationId);

			// Prepare to handle the reply
			replyHandlers.put(correlationId, event);

			// Do the send
			send(message);

			// Wait for the reply
			try {
				return event.get();
			} catch (ExecutionException e) {
				// Decode the cause
				try {
					throw requireNonNull(e.getCause(),
							"cause of execution exception was null");
				} catch (Error | RuntimeException | InterruptedException ex) {
					throw ex;
				} catch (Throwable ex) {
					throw new RuntimeException("unexpected exception", ex);
				}
			}
		}

		@Override
		public void onOpen(ServerHandshake handshakedata) {
			log.info("websocket connection opened");
		}

		@Override
		public void onMessage(String message) {
			log.warn("Unexpected text message on websocket: {}", message);
		}

		private IOException manufactureException(ByteBuffer message) {
			var bytes = new byte[message.remaining()];
			message.get(bytes);
			return new IOException(new String(bytes, UTF_8));
		}

		@Override
		public void onMessage(ByteBuffer message) {
			message.order(LITTLE_ENDIAN);
			int code = message.getInt();
			switch (ProxyProtocol.values()[code]) {
			case OPEN:
			case CLOSE:
			case OPEN_U:
				requireNonNull(replyHandlers.remove(message.getInt()),
						"uncorrelated response").complete(message);
				break;
			case MSG:
				requireNonNull(channels.get(message.getInt()),
						"unrecognised channel").receive(message);
				break;
			case ERR:
				requireNonNull(replyHandlers.remove(message.getInt()),
						"uncorrelated response")
						.completeExceptionally(manufactureException(message));
				break;
			// case MSG_TO: // Never sent
			default:
				log.error("unexpected message code: {}", code);
			}
		}

		@Override
		public ConnectedChannel openChannel(ChipLocation chip, int port,
				BlockingQueue<ByteBuffer> receiveQueue)
				throws InterruptedException {
			requireNonNull(receiveQueue);

			var b = OPEN.allocate();
			b.putInt(0); // dummy
			b.putInt(chip.getX());
			b.putInt(chip.getY());
			b.putInt(port);
			b.flip();

			int channelId = call(b).getInt();

			return new ConnectedChannelImpl(channelId, receiveQueue);
		}

		@Override
		public UnconnectedChannel openUnconnectedChannel(
				BlockingQueue<ByteBuffer> receiveQueue)
				throws InterruptedException {
			requireNonNull(receiveQueue);

			var b = OPEN_U.allocate();
			b.putInt(0); // dummy
			b.flip();

			var msg = call(b);

			int channelId = msg.getInt();
			var addr = new byte[INET_SIZE];
			msg.get(addr);
			int port = msg.getInt();
			return new UnconnectedChannelImpl(channelId,
					getByAddressQuietly(addr), port, receiveQueue);
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			log.info("websocket connection closed: {}", reason);
		}

		@Override
		public void onError(Exception ex) {
			log.error("Failure on websocket", ex);
			failure = ex;
		}

		private void rethrowFailure() throws IOException, InterruptedException {
			try {
				throw failure;
			} catch (IOException | InterruptedException | RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new IOException("unexpected exception", e);
			}
		}

		/** Base class for channels routed via the proxy. */
		private abstract class ChannelBase implements AutoCloseable {
			/** Channel ID. Issued by server. */
			final int id;

			/** Whether this channel is closed. */
			boolean closed;

			/** Where we enqueue the received messages. */
			private final BlockingQueue<ByteBuffer> receiveQueue;

			/**
			 * @param id
			 *            The ID of the channel.
			 * @param receiveQueue
			 *            Where to enqueue received messages.
			 */
			ChannelBase(int id, BlockingQueue<ByteBuffer> receiveQueue) {
				this.id = id;
				this.receiveQueue = receiveQueue;
				channels.put(id, this);
			}

			/**
			 * The receive handler. Strips the header and sends the contents to
			 * the registered receiver handler.
			 *
			 * @param msg
			 *            The message off the websocket.
			 */
			private void receive(ByteBuffer msg) {
				msg = msg.slice();
				msg.order(LITTLE_ENDIAN);
				receiveQueue.add(msg);
			}

			/**
			 * Close this channel.
			 *
			 * @throws IOException
			 *             If things fail.
			 */
			@Override
			public void close() throws IOException {
				if (closed) {
					return;
				}

				var b = CLOSE.allocate();
				b.putInt(0); // dummy
				b.putInt(id);
				b.flip();

				try {
					int reply = call(b).getInt();
					channels.remove(id);
					if (reply != id) {
						log.warn("did not properly close channel");
					}
					closed = true;
				} catch (InterruptedException e) {
					throw new IOException("failed to close channel", e);
				}
			}

			/**
			 * Forward a (fully prepared) message to the websocket, provided the
			 * channel is open.
			 *
			 * @param fullMessage
			 *            The fully prepared message to send, <em>including the
			 *            proxy protocol header</em>.
			 * @throws EOFException
			 *             If the channel is closed.
			 */
			final void sendPreparedMessage(ByteBuffer fullMessage)
					throws EOFException {
				if (closed) {
					throw new EOFException("connection closed");
				}
				send(fullMessage);
			}
		}

		/**
		 * A channel that is connected to a particular board.
		 */
		private class ConnectedChannelImpl extends ChannelBase
				implements ProxyProtocolClient.ConnectedChannel {
			/**
			 * @param id
			 *            The ID of the channel.
			 * @param receiveQueue
			 *            Where to enqueue received messages.
			 */
			ConnectedChannelImpl(int id,
					BlockingQueue<ByteBuffer> receiveQueue) {
				super(id, receiveQueue);
			}

			@Override
			public void send(ByteBuffer msg) throws IOException {
				var b = MSG.allocate();
				b.putInt(id);
				b.put(msg);
				b.flip();

				sendPreparedMessage(b);
			}

			@Override
			public String toString() {
				return "Connected Channel " + id;
			}
		}

		/**
		 * A channel that is not connected to any particular board.
		 */
		private class UnconnectedChannelImpl extends ChannelBase
				implements ProxyProtocolClient.UnconnectedChannel {
			private final Inet4Address addr;

			private final int port;

			/**
			 * @param id
			 *            The ID of the channel.
			 * @param addr
			 *            The "local" address for this channel (on the server)
			 * @param port
			 *            The "local" port for this channel (on the server)
			 * @param receiveQueue
			 *            Where to enqueue received messages.
			 * @throws RuntimeException
			 *             If the address can't be parsed. Really not expected!
			 */
			UnconnectedChannelImpl(int id, Inet4Address addr, int port,
					BlockingQueue<ByteBuffer> receiveQueue) {
				super(id, receiveQueue);
				this.addr = addr;
				this.port = port;
			}

			@Override
			public Inet4Address getAddress() {
				return addr;
			}

			@Override
			public int getPort() {
				return port;
			}

			@Override
			public void send(ChipLocation chip, int port, ByteBuffer msg)
					throws IOException {
				var b = MSG_TO.allocate();
				b.putInt(id);
				b.putInt(chip.getX());
				b.putInt(chip.getY());
				b.putInt(port);
				b.put(msg);
				b.flip();

				sendPreparedMessage(b);
			}

			@Override
			public String toString() {
				return "Unconnected channel " + id;
			}
		}
	}

	/** Time between pings of the websocket. 30s. */
	private static final int PING_DELAY = 30;

	@Override
	public ProxyProtocolClient websocket(URI url)
			throws InterruptedException, IOException {
		var wsc = new ProxyProtocolClientImpl(url);
		if (nonNull(session)) {
			log.debug("Attaching websocket to session {}", session);
			wsc.addHeader(COOKIE, SESSION_NAME + "=" + session);
		}
		if (nonNull(csrfHeader) && nonNull(csrf)) {
			log.debug("Marking websocket with token {}={}", csrfHeader, csrf);
			wsc.addHeader(csrfHeader, csrf);
		}
		if (!wsc.connectBlocking()) {
			if (nonNull(wsc.failure)) {
				wsc.rethrowFailure();
			}
			// Don't know what went wrong! Log might say
			throw new IOException("undiagnosed connection failure");
		}
		wsc.setConnectionLostTimeout(PING_DELAY);
		return wsc;
	}

	private synchronized void authorizeConnection(HttpURLConnection c,
			boolean forStateChange) {
		/*
		 * For some really stupid reason, Java doesn't let you set a cookie
		 * manager on a per-connection basis, so we need to manage the session
		 * cookie ourselves.
		 */
		if (session != null) {
			log.debug("Attaching to session {}", session);
			c.setRequestProperty(COOKIE, SESSION_NAME + "=" + session);
		}

		if (csrfHeader != null && csrf != null && forStateChange) {
			log.debug("Marking session with token {}={}", csrfHeader, csrf);
			c.setRequestProperty(csrfHeader, csrf);
		}
		c.setInstanceFollowRedirects(false);
	}

	/**
	 * Check for and handle any session cookie changes.
	 * <p>
	 * Assumes that the session key is in the {@code JSESSIONID} cookie.
	 *
	 * @param conn
	 *            Connection that's had a transaction processed.
	 * @return Whether the session cookie was set. Normally uninteresting.
	 */
	@Override
	public synchronized boolean trackCookie(HttpURLConnection conn) {
		// Careful: spec allows for multiple Set-Cookie fields
		boolean found = false;
		var headerFields = conn.getHeaderFields();
		var cookiesHeader = headerFields.get(SET_COOKIE);
		if (cookiesHeader != null) {
			for (String setCookie : cookiesHeader) {
				log.debug("Cookie header: {}", setCookie);
				var m = SESSION_ID_RE.matcher(setCookie);
				if (m.find()) {
					session = m.group(1);
					found = true;
				}
			}
		}
		return found;
	}

	/** Helper for digging CSRF token info out of HTML. */
	private Stream<String> getCSRF(String line) {
		var m = CSRF_ID_RE.matcher(line);
		return m.find() ? Stream.of(m.group(1)) : Stream.empty();
	}

	/**
	 * Initialise a new anonymous temporary session.
	 *
	 * @return The temporary CSRF token. Allows us to log in.
	 * @throws IOException
	 *             If things go wrong.
	 */
	private String makeTemporarySession() throws IOException {
		var c = connection(LOGIN_FORM);
		try (var is = checkForError(c, "couldn't get login form")) {
			// There's a session cookie at this point; we need it!
			if (!trackCookie(c)) {
				throw new IOException("could not establish session");
			}
			// This is nasty; parsing the HTML source
			return readLines(is, UTF_8).stream().flatMap(this::getCSRF)
					.findFirst().orElseThrow(() -> new IOException(
							"could not parse CSRF token"));
		}
	}

	/**
	 * Upgrade an anonymous session to a logged-in one.
	 *
	 * @param tempCsrf
	 *            The temporary CSRF token.
	 * @throws IOException
	 *             If things go wrong.
	 */
	private void logSessionIn(String tempCsrf) throws IOException {
		var c = connection(LOGIN_HANDLER, true);
		c.setRequestMethod("POST");
		writeForm(c, ofEntries(entry("_csrf", tempCsrf),
				entry("submit", "submit"), entry("username", username),
				entry("password", password)));
		try (var ignored = checkForError(c, "login failed")) {
			/*
			 * The result should be a redirect; the body is irrelevant but the
			 * headers matter. In particular, there should be a new session
			 * cookie after login.
			 */
			if (!trackCookie(c)) {
				throw new IOException("could not establish session");
			}
		}
	}

	/**
	 * Renew the session credentials.
	 *
	 * @param postRenew
	 *            Whether to rediscover the root data after session renewal.
	 * @throws IOException
	 *             If things go wrong.
	 */
	private synchronized void renew(boolean postRenew) throws IOException {

		// Create a temporary session so we can log in
		var tempCsrf = makeTemporarySession();

		// If we didn't use a bearer token, we need to log in properly
		logSessionIn(tempCsrf);

		if (postRenew) {
			discoverRoot();
		}
	}

	@Override
	public <T, Exn extends Exception> T withRenewal(Action<T, Exn> action)
			throws Exn, IOException {
		try {
			return action.call();
		} catch (SpallocException e) {
			if (e.getResponseCode() == HTTP_UNAUTHORIZED) {
				renew(true);
				return action.call();
			}
			throw e;
		} catch (IOException e) {
			// Need to read the error message, like a barbarian!
			if (e.getMessage().contains(HTTP_UNAUTHORIZED_MESSAGE)) {
				renew(true);
				return action.call();
			}
			throw e;
		}
	}

	@Override
	public synchronized RootInfo discoverRoot() throws IOException {
		var conn = connection(SPALLOC_ROOT);
		try (var is = checkForError(conn, "couldn't read service root")) {
			var root = readJson(is, RootInfo.class);
			this.csrfHeader = root.csrfHeader;
			this.csrf = root.csrfToken;
			root.csrfHeader = null;
			root.csrfToken = null;
			return root;
		}
	}
}
