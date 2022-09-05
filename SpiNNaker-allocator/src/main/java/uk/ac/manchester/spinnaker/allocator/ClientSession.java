/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.allocator;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.IOUtils.readLines;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.allocator.SpallocClientFactory.asDir;
import static uk.ac.manchester.spinnaker.allocator.SpallocClientFactory.checkForError;
import static uk.ac.manchester.spinnaker.allocator.SpallocClientFactory.readJson;
import static uk.ac.manchester.spinnaker.allocator.SpallocClientFactory.writeForm;
import static uk.ac.manchester.spinnaker.utils.InetFactory.getByAddressQuietly;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.allocator.ClientSession.ProxyProtocolClientImpl;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Manages the login session. This allows us to avoid the (heavy) cost of the
 * password hashing algorithm used, at least most of the time.
 *
 * @author Donal Fellows
 */
@UsedInJavadocOnly(URLConnection.class)
final class ClientSession {
	private static final Logger log = getLogger(ClientSession.class);

	private static final String HTTP_UNAUTHORIZED_MESSAGE =
			"Server returned HTTP response code: 401";

	private static final String COOKIE = "Cookie";

	private static final String SET_COOKIE = "Set-Cookie";

	private static final String SESSION_NAME = "JSESSIONID";

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
	 * @param baseURI
	 *            The service base URI. <em>Must</em> be absolute! <em>Must
	 *            not</em> include a username or password!
	 * @param username
	 *            The username to use
	 * @param password
	 *            The password to use
	 * @throws IOException
	 *             If things go wrong.
	 */
	ClientSession(URI baseURI, String username, String password)
			throws IOException {
		baseUri = asDir(baseURI);
		this.username = username;
		this.password = password;
		// This does the actual logging in process
		renew(false);
	}
	// TODO make a constructor that takes a bearer token

	/**
	 * An action used by {@link ClientSession#withRenewal(Action)
	 * withRenewal()}. The action will be performed once, and if it fails with a
	 * permission fault, the session will be renewed and the action performed
	 * exactly once more.
	 *
	 * @param <T>
	 *            The type of the result of the action.
	 * @param <Exn>
	 *            The extra exceptions that may be thrown by the action.
	 */
	interface Action<T, Exn extends Exception> {
		/**
		 * Perform the action.
		 *
		 * @return The result of the action.
		 * @throws IOException
		 *             If network I/O fails.
		 * @throws Exn
		 *             If another failure happens.
		 */
		T act() throws Exn, IOException;
	}

	private static HttpURLConnection createConnection(URI url)
			throws IOException {
		log.debug("will connect to {}", url);
		var c = (HttpURLConnection) url.toURL().openConnection();
		c.setUseCaches(false);
		return c;
	}

	/**
	 * Create a connection that's part of the session.
	 *
	 * @param url
	 *            The URL (relative or absolute) for where to access.
	 * @param forStateChange
	 *            If {@code true}, the connection will be configured so that it
	 *            includes a relevant CSRF token.
	 * @return the partially-configured connection;
	 *         {@link HttpURLConnection#setRequestMethod(String)},
	 *         {@link URLConnection#doOutput(boolean)} and
	 *         {@link URLConnection#setRequestProperty(String,String)} may still
	 *         need to be called.
	 * @throws IOException
	 *             If things go wrong
	 */
	HttpURLConnection connection(URI url, boolean forStateChange)
			throws IOException {
		var realUrl = baseUri.resolve(url);
		var c = createConnection(realUrl);
		authorizeConnection(c, forStateChange);
		return c;
	}

	/**
	 * Create a connection that's part of the session.
	 *
	 * @param url
	 *            The URL (relative or absolute) for where to access.
	 * @param url2
	 *            Secondary URL, often a path tail and/or query suffix.
	 * @param forStateChange
	 *            If {@code true}, the connection will be configured so that it
	 *            includes a relevant CSRF token.
	 * @return the partially-configured connection;
	 *         {@link HttpURLConnection#setRequestMethod(String)},
	 *         {@link URLConnection#doOutput(boolean)} and
	 *         {@link URLConnection#setRequestProperty(String,String)} may still
	 *         need to be called.
	 * @throws IOException
	 *             If things go wrong
	 */
	HttpURLConnection connection(URI url, URI url2, boolean forStateChange)
			throws IOException {
		var realUrl = baseUri.resolve(url).resolve(url2);
		var c = createConnection(realUrl);
		authorizeConnection(c, forStateChange);
		return c;
	}

	/**
	 * Create a connection that's part of the session.
	 *
	 * @param url
	 *            The URL (relative or absolute) for where to access.
	 * @param url2
	 *            Secondary URL, often a path tail and/or query suffix.
	 * @param forStateChange
	 *            If {@code true}, the connection will be configured so that it
	 *            includes a relevant CSRF token.
	 * @return the partially-configured connection;
	 *         {@link HttpURLConnection#setRequestMethod(String)},
	 *         {@link URLConnection#doOutput(boolean)} and
	 *         {@link URLConnection#setRequestProperty(String,String)} may still
	 *         need to be called.
	 * @throws IOException
	 *             If things go wrong
	 */
	HttpURLConnection connection(URI url, String url2, boolean forStateChange)
			throws IOException {
		var realUrl = baseUri.resolve(url).resolve(url2);
		var c = createConnection(realUrl);
		authorizeConnection(c, forStateChange);
		return c;
	}

	/**
	 * Create a connection that's part of the session.
	 *
	 * @param url
	 *            The URL (relative or absolute) for where to access.
	 * @param url2
	 *            Secondary URL, often a path tail and/or query suffix.
	 * @return the connection, which should not be used to change the service
	 *         state.
	 * @throws IOException
	 *             If things go wrong
	 */
	HttpURLConnection connection(URI url, URI url2) throws IOException {
		return connection(url, url2, false);
	}

	/**
	 * Create a connection that's part of the session.
	 *
	 * @param url
	 *            The URL (relative or absolute) for where to access.
	 * @return the connection, which should not be used to change the service
	 *         state.
	 * @throws IOException
	 *             If things go wrong
	 */
	HttpURLConnection connection(URI url) throws IOException {
		return connection(url, false);
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

		private final Map<Integer, Consumer<ByteBuffer>> replyHandlers;

		private final Map<Integer, Consumer<ByteBuffer>> channelHandlers;

		private int correlationCounter;

		private Exception failure;

		/**
		 * @param uri
		 *            The address of the websocket.
		 */
		ProxyProtocolClientImpl(URI uri) {
			super(uri);
			replyHandlers = new HashMap<>();
			channelHandlers = new HashMap<>();
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
			replyHandlers.put(correlationId, event::complete);

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

		@Override
		public void onMessage(ByteBuffer message) {
			message.order(LITTLE_ENDIAN);
			int code = message.getInt();
			switch (ProxyProtocol.values()[code]) {
			case OPEN:
			case CLOSE:
			case OPEN_U:
				requireNonNull(replyHandlers.remove(message.getInt()),
						"uncorrelated response").accept(message);
				break;
			case MSG:
				requireNonNull(channelHandlers.get(message.getInt()),
						"unrecognised channel").accept(message);
				break;
			default:
				log.error("unexpected message code: {}", code);
			}
		}

		@Override
		public ConnectedChannel openChannel(ChipLocation chip, int port,
				Consumer<ByteBuffer> receiver) throws InterruptedException {
			requireNonNull(receiver);

			var b = ProxyProtocol.OPEN.allocate();
			b.putInt(ProxyProtocol.OPEN.ordinal());
			b.putInt(0); // dummy
			b.putInt(chip.getX());
			b.putInt(chip.getY());
			b.putInt(port);
			b.flip();

			return new ConnectedChannel(call(b).getInt(), this, receiver);
		}

		@Override
		public UnconnectedChannel openUnconnectedChannel(
				Consumer<ByteBuffer> receiver) throws InterruptedException {
			requireNonNull(receiver);

			var b = ProxyProtocol.OPEN_U.allocate();
			b.putInt(ProxyProtocol.OPEN_U.ordinal());
			b.putInt(0); // dummy
			b.flip();

			var msg = call(b);

			int channelId = msg.getInt();
			var addr = new byte[INET_SIZE];
			msg.get(addr);
			int port = msg.getInt();
			return new UnconnectedChannel(channelId,
					getByAddressQuietly(addr), port, this, receiver);
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
	}

	/** Base class for channels routed via the proxy. */
	abstract static class Channel implements AutoCloseable {
		private static final Logger log = getLogger(ClientSession.class);

		/** Channel ID. Issued by server. */
		final int id;

		/** The websocket. */
		private final ProxyProtocolClientImpl client;

		/** Whether this channel is closed. */
		boolean closed;

		private final Consumer<ByteBuffer> receiver;

		/**
		 * @param id
		 *            The ID of the channel.
		 * @param client
		 *            The websocket.
		 * @param receiver
		 *            Where to send received messages, which is probably an
		 *            operation to enqueue them somewhere.
		 */
		Channel(int id, ProxyProtocolClientImpl client,
				Consumer<ByteBuffer> receiver) {
			this.id = id;
			this.client = client;
			this.receiver = receiver;
			client.channelHandlers.put(id, this::receive);
		}

		/**
		 * The receive handler. Strips the header and sends the contents to the
		 * registered receiver handler.
		 *
		 * @param msg
		 *            The message off the websocket.
		 */
		private void receive(ByteBuffer msg) {
			msg = msg.slice();
			msg.order(LITTLE_ENDIAN);
			receiver.accept(msg);
		}

		/**
		 * Close this channel.
		 *
		 * @throws IOException
		 */
		@Override
		public void close() throws IOException {
			if (closed) {
				return;
			}

			var b = ProxyProtocol.CLOSE.allocate();
			b.putInt(ProxyProtocol.CLOSE.ordinal());
			b.putInt(0); // dummy
			b.putInt(id);
			b.flip();

			try {
				int reply = client.call(b).getInt();
				client.channelHandlers.remove(id);
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
		 * @throws IllegalStateException
		 *             If the channel is closed.
		 */
		protected final void sendPreparedMessage(ByteBuffer fullMessage) {
			if (closed) {
				throw new IllegalStateException();
			}
			client.send(fullMessage);
		}
	}

	/**
	 * Connect a Spalloc proxy protocol websocket to the given URL.
	 *
	 * @param url
	 *            Where the websocket connects.
	 * @return The connected websocket.
	 * @throws InterruptedException
	 *             If interrupted during connection
	 * @throws IOException
	 *             If there are network problems
	 * @throws RuntimeException
	 *             For various reasons
	 */
	ProxyProtocolClient websocket(URI url)
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
		try {
			if (!wsc.connectBlocking()) {
				if (nonNull(wsc.failure)) {
					throw wsc.failure;
				}
				// Don't know what went wrong! Log might say
				throw new IOException("undiagnosed connection failure");
			}
		} catch (IOException | InterruptedException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("unexpected exception", e);
		}
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
	 * @throws IOException
	 *             If things go wrong.
	 */
	synchronized boolean trackCookie(HttpURLConnection conn) {
		// Careful: spec allows for multiple Set-Cookie fields
		boolean found = false;
		for (int i = 0; true; i++) {
			var key = conn.getHeaderFieldKey(i);
			if (key == null) {
				break;
			}
			if (!key.equalsIgnoreCase(SET_COOKIE)) {
				continue;
			}
			var setCookie = conn.getHeaderField(i);
			if (setCookie != null) {
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
		Set<String> s = Set.of();
		if (m.find()) {
			s = Set.of(m.group(1));
		}
		return s.stream();
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
		writeForm(c,
				ofEntries(entry("_csrf", tempCsrf), entry("submit", "submit"),
						entry("username", username),
						entry("password", password)));
		checkForError(c, "login failed");
		// There should be a new session cookie after login
		if (!trackCookie(c)) {
			throw new IOException("could not establish session");
		}
	}

	/**
	 * Renew the session credentials.
	 *
	 * @param action
	 *            How to renew the CSRF token, if that's desired.
	 * @throws IOException
	 *             If things go wrong.
	 */
	private synchronized void renew(boolean postRenew) throws IOException {
		// Create a temporary session so we can log in
		var tempCsrf = makeTemporarySession();

		// This makes the real session
		logSessionIn(tempCsrf);

		if (postRenew) {
			discoverRoot();
		}
	}

	/**
	 * Carry out an action, applying session renewal <em>once</em> if needed.
	 *
	 * @param <T>
	 *            The type of the return value.
	 * @param <Exn>
	 *            The extra exceptions that may be thrown by the action.
	 * @param action
	 *            The action to be repeated if it fails due to session expiry.
	 * @return The result of the action
	 * @throws IOException
	 *             If things go wrong.
	 * @throws Exn
	 *             If another kind of failure happens.
	 */
	<T, Exn extends Exception> T withRenewal(Action<T, Exn> action)
			throws Exn, IOException {
		try {
			return action.act();
		} catch (SpallocClient.Exception e) {
			if (e.getResponseCode() == HTTP_UNAUTHORIZED) {
				renew(true);
				return action.act();
			}
			throw e;
		} catch (IOException e) {
			// Need to read the error message, like a barbarian!
			if (e.getMessage().contains(HTTP_UNAUTHORIZED_MESSAGE)) {
				renew(true);
				return action.act();
			}
			throw e;
		}
	}

	/**
	 * Discovers the root of a Spalloc service. Also sets up the true CSRF token
	 * handling.
	 *
	 * @return The service root information.
	 * @throws IOException
	 *             If access fails.
	 */
	synchronized RootInfo discoverRoot() throws IOException {
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

/** Message IDs in the proxy protocol. */
enum ProxyProtocol {
	/** Open a connected channel. */
	OPEN(20),
	/** Close a channel. */
	CLOSE(12),
	/** Send a message on a connected channel. */
	MSG(1600),
	/** Open an unconnected channel. */
	OPEN_U(8),
	/** Send a message on an unconnected channel. */
	MSG_TO(1600);

	private int size;

	ProxyProtocol(int size) {
		this.size = size;
	}

	/**
	 * Create a buffer big enough to hold a message.
	 *
	 * @return A little endian buffer of sufficient size.
	 */
	ByteBuffer allocate() {
		return ByteBuffer.allocate(size).order(LITTLE_ENDIAN);
	}
}

/**
 * A channel that is connected to a particular board.
 */
class ConnectedChannel extends ClientSession.Channel {
	/**
	 * @param id
	 *            The ID of the channel.
	 * @param client
	 *            The websocket.
	 * @param receiver
	 *            Where to send received messages, which is probably an
	 *            operation to enqueue them somewhere.
	 */
	ConnectedChannel(int id, ProxyProtocolClientImpl client,
			Consumer<ByteBuffer> receiver) {
		super(id, client, receiver);
	}

	/**
	 * Send a message to the board that the channel is connected to.
	 *
	 * @param msg
	 *            The payload of the message to send. Might be a serialized SDP
	 *            message, for example.
	 */
	public void send(ByteBuffer msg) {
		var b = ProxyProtocol.MSG.allocate();
		b.putInt(ProxyProtocol.MSG.ordinal());
		b.putInt(id);
		b.put(msg);
		b.flip();

		sendPreparedMessage(b);
	}
}

/**
 * A channel that is not connected to any particular board.
 */
class UnconnectedChannel extends ClientSession.Channel {
	private final Inet4Address addr;

	private final int port;

	/**
	 * @param id
	 *            The ID of the channel.
	 * @param addr
	 *            The "local" address for this channel (on the server)
	 * @param port
	 *            The "local" port for this channel (on the server)
	 * @param client
	 *            The websocket.
	 * @param receiver
	 *            Where to send received messages, which is probably an
	 *            operation to enqueue them somewhere.
	 * @throws RuntimeException
	 *             If the address can't be parsed. Really not expected!
	 */
	UnconnectedChannel(int id, Inet4Address addr, int port,
			ProxyProtocolClientImpl client, Consumer<ByteBuffer> receiver) {
		super(id, client, receiver);
		this.addr = addr;
		this.port = port;
	}

	/**
	 * @return The "local" address for this channel.
	 */
	public Inet4Address getAddress() {
		return addr;
	}

	/**
	 * @return The "local" port for this channel.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Send a message to a board in the allocation of the current job.
	 *
	 * @param chip
	 *            Which ethernet chip to send to.
	 * @param port
	 *            Which UDP port to send to.
	 * @param msg
	 *            The payload of the message to send. Might be a serialized SDP
	 *            message, for example.
	 */
	public void send(ChipLocation chip, int port, ByteBuffer msg) {
		var b = ProxyProtocol.MSG_TO.allocate();
		b.putInt(ProxyProtocol.MSG_TO.ordinal());
		b.putInt(id);
		b.putInt(chip.getX());
		b.putInt(chip.getY());
		b.putInt(port);
		b.put(msg);
		b.flip();

		sendPreparedMessage(b);
	}
}

/**
 * Our interface that specifies what top-level operations can be done by the UDP
 * socket proxying system. Used to hide the details of the websocket from code
 * that doesn't need to care and shouldn't care.
 */
interface ProxyProtocolClient extends AutoCloseable {
	/**
	 * Open a connected channel to a SpiNNaker board in the current job.
	 *
	 * @param chip
	 *            The coordinates of the ethernet chip of the board to connect
	 *            to.
	 * @param port
	 *            The UDP port to connect to.
	 * @param receiver
	 *            Where to send received messages, which is probably an
	 *            operation to enqueue them somewhere.
	 * @return The connected channel.
	 * @throws InterruptedException
	 *             If interrupted while waiting for a reply.
	 */
	ConnectedChannel openChannel(ChipLocation chip, int port,
			Consumer<ByteBuffer> receiver) throws InterruptedException;

	/**
	 * Open an unconnected channel to any SpiNNaker board in the current job.
	 *
	 * @param receiver
	 *            Where to send received messages, which is probably an
	 *            operation to enqueue them somewhere.
	 * @return The unconnected channel.
	 * @throws InterruptedException
	 *             If interrupted while waiting for a reply.
	 */
	UnconnectedChannel openUnconnectedChannel(
			Consumer<ByteBuffer> receiver) throws InterruptedException;

	/**
	 * Is the underlying websocket in the state OPEN.
	 *
	 * @return state equals ReadyState.OPEN
	 */
	boolean isOpen();

	/**
	 * {@inheritDoc}
	 * <p>
	 * Note that this may process the close asynchronously.
	 */
	@Override
	void close();
}
