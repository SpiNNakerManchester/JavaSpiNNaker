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

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.IOUtils.readLines;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.allocator.SpallocClient.Job;
import uk.ac.manchester.spinnaker.allocator.SpallocClient.Machine;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;

/**
 * A factory for clients to connect to the Spalloc service.
 * <p>
 * <strong>Implementation Note:</strong>
 * Neither this class nor the client classes it creates maintain state that
 * needs to be closed explicitly.
 *
 * @author Donal Fellows
 */
public class SpallocClientFactory {
	private static final Logger log = getLogger(SpallocClientFactory.class);

	private static final String CONTENT_TYPE = "Content-Type";

	private static final String COOKIE = "Cookie";

	private static final String SET_COOKIE = "Set-Cookie";

	private static final String TEXT_PLAIN = "text/plain; charset=UTF-8";

	private static final String APPLICATION_JSON = "application/json";

	private static final String FORM_ENCODED =
			"application/x-www-form-urlencoded";

	private static final URI LOGIN_FORM = URI.create("system/login.html");

	private static final URI LOGIN_HANDLER = URI.create("system/perform_login");

	private static final URI SPALLOC_ROOT = URI.create("srv/spalloc");

	private static final URI KEEPALIVE = URI.create("keepalive");

	private static final URI MACHINE = URI.create("machine");

	private static final URI POWER = URI.create("power");

	private static final URI WAIT_FLAG = URI.create("?wait=true");

	/** Used to convert to/from JSON. */
	static final JsonMapper JSON_MAPPER = JsonMapper.builder()
			.findAndAddModules().disable(WRITE_DATES_AS_TIMESTAMPS)
			.propertyNamingStrategy(KEBAB_CASE).build();

	/**
	 * Cache of machines, which don't expire.
	 */
	private final Map<String, Machine> machineMap =
			synchronizedMap(new HashMap<>());

	/**
	 * Add a {@code /} to the end of the path part of a URI.
	 *
	 * @param uri
	 *            The URI to amend. Assumed to be HTTP or HTTPS.
	 * @return The amended URI.
	 */
	static URI asDir(URI uri) {
		String path = uri.getPath();
		if (!path.endsWith("/")) {
			path += "/";
			uri = uri.resolve(path);
		}
		return uri;
	}

	private static String encode(String string)
			throws UnsupportedEncodingException {
		return URLEncoder.encode(string, UTF_8.name());
	}

	private static <T> T readJson(InputStream is, Class<T> cls)
			throws IOException {
		return JSON_MAPPER.readValue(is, cls);
	}

	private static void writeForm(HttpURLConnection connection,
			Map<String, String> map) throws IOException {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (Entry<String, String> e : map.entrySet()) {
			sb.append(sep).append(e.getKey()).append("=")
					.append(encode(e.getValue()));
			sep = "&";
		}

		connection.setDoOutput(true);
		connection.setRequestProperty(CONTENT_TYPE, FORM_ENCODED);
		try (Writer w =
				new OutputStreamWriter(connection.getOutputStream(), UTF_8)) {
			w.write(sb.toString());
		}
	}

	private void writeObject(HttpURLConnection connection, Object object)
			throws IOException {
		connection.setDoOutput(true);
		connection.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
		try (OutputStream out = connection.getOutputStream()) {
			JSON_MAPPER.writeValue(out, object);
		}
	}

	private static void writeString(HttpURLConnection connection, String string)
			throws IOException {
		connection.setDoOutput(true);
		connection.setRequestProperty(CONTENT_TYPE, TEXT_PLAIN);
		try (Writer w =
				new OutputStreamWriter(connection.getOutputStream(), UTF_8)) {
			w.write(string);
		}
	}

	/**
	 * Checks for errors in the response.
	 *
	 * @param conn
	 *            The HTTP connection
	 * @param errorMessage
	 *            The message to use on error (describes what did not work at a
	 *            higher level)
	 * @return The input stream so any non-error response content can be
	 *         obtained.
	 * @throws IOException
	 *             If things go wrong with comms.
	 * @throws FileNotFoundException
	 *             on a {@link HttpURLConnection#HTTP_NOT_FOUND}
	 * @throws SpallocClient.Exception
	 *             on other server errors
	 */
	private static InputStream checkForError(HttpURLConnection conn,
			String errorMessage) throws IOException {
		if (conn.getResponseCode() == HTTP_NOT_FOUND) {
			// Special case
			throw new FileNotFoundException(errorMessage);
		}
		if (conn.getResponseCode() >= HTTP_BAD_REQUEST) {
			throw new SpallocClient.Exception(conn.getErrorStream(),
					conn.getResponseCode());
		}
		return conn.getInputStream();
	}

	/**
	 * Manages the login session. This allows us to avoid the (heavy) cost of
	 * the password hashing algorithm used, at least most of the time.
	 *
	 * @author Donal Fellows
	 */
	private static final class Session {
		private static final String HTTP_UNAUTHORIZED_MESSAGE =
				"Server returned HTTP response code: 401";

		private static final String SESSION_NAME = "JSESSIONID";

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
		Session(URI baseURI, String username, String password)
				throws IOException {
			baseUri = asDir(baseURI);
			this.username = username;
			this.password = password;
			// This does the actual logging in process
			renew(null);
		}

		private HttpURLConnection createConnection(URL url) throws IOException {
			log.debug("will connect to {}", url);
			HttpURLConnection c = (HttpURLConnection) url.openConnection();
			c.setUseCaches(false);
			return c;
		}

		/**
		 * Create a connection that's part of the session.
		 *
		 * @param url
		 *            The URL (relative or absolute) for where to access.
		 * @param forStateChange
		 *            If {@code true}, the connection will be configured so that
		 *            it includes a relevant CSRF token.
		 * @return the partially-configured connection;
		 *         {@link HttpURLConnection#setRequestMethod(String)},
		 *         {@link URLConnection#doOutput(boolean)} and
		 *         {@link URLConnection#setRequestProperty(String,String)} may
		 *         still need to be called.
		 * @throws IOException
		 *             If things go wrong
		 */
		HttpURLConnection connection(URI url, boolean forStateChange)
				throws IOException {
			URI realUrl = baseUri.resolve(url);
			HttpURLConnection c = createConnection(realUrl.toURL());
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
		 *            If {@code true}, the connection will be configured so that
		 *            it includes a relevant CSRF token.
		 * @return the partially-configured connection;
		 *         {@link HttpURLConnection#setRequestMethod(String)},
		 *         {@link URLConnection#doOutput(boolean)} and
		 *         {@link URLConnection#setRequestProperty(String,String)} may
		 *         still need to be called.
		 * @throws IOException
		 *             If things go wrong
		 */
		HttpURLConnection connection(URI url, URI url2, boolean forStateChange)
				throws IOException {
			URI realUrl = baseUri.resolve(url).resolve(url2);
			HttpURLConnection c = createConnection(realUrl.toURL());
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
		 * @return the connection, which should not be used to change the
		 *         service state.
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
		 * @return the connection, which should not be used to change the
		 *         service state.
		 * @throws IOException
		 *             If things go wrong
		 */
		HttpURLConnection connection(URI url) throws IOException {
			return connection(url, false);
		}

		private void authorizeConnection(HttpURLConnection c,
				boolean forStateChange) {
			/*
			 * For some really stupid reason, Java doesn't let you set a cookie
			 * manager on a per-connection basis, so we need to manage the
			 * session cookie ourselves.
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
		boolean trackCookie(HttpURLConnection conn) {
			// Careful: spec allows for multiple Set-Cookie fields
			boolean found = false;
			for (int i = 0; true; i++) {
				String key = conn.getHeaderFieldKey(i);
				if (key == null) {
					break;
				}
				if (!key.equalsIgnoreCase(SET_COOKIE)) {
					continue;
				}
				String setCookie = conn.getHeaderField(i);
				if (setCookie != null) {
					Matcher m = SESSION_ID_RE.matcher(setCookie);
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
			Matcher m = CSRF_ID_RE.matcher(line);
			Set<String> s = emptySet();
			if (m.find()) {
				s = singleton(m.group(1));
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
			HttpURLConnection c = connection(LOGIN_FORM);
			try (InputStream is = checkForError(c, "couldn't get login form")) {
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
			Map<String, String> form = new HashMap<>();
			form.put("_csrf", tempCsrf);
			form.put("username", username);
			form.put("password", password);
			form.put("submit", "submit");

			HttpURLConnection c = connection(LOGIN_HANDLER, true);
			c.setRequestMethod("POST");
			writeForm(c, form);
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
		private void renew(Action<?> action) throws IOException {
			// Create a temporary session so we can log in
			String tempCsrf = makeTemporarySession();

			// This makes the real session
			logSessionIn(tempCsrf);

			if (action != null) {
				action.act();
			}
		}

		/**
		 * Carry out an action, applying session renewal <em>once</em> if
		 * needed.
		 *
		 * @param <T>
		 *            The type of the return value.
		 * @param action
		 *            The action to be repeated if it fails due to session
		 *            expiry.
		 * @return The result of the action
		 * @throws IOException
		 *             If things go wrong.
		 */
		<T> T withRenewal(Action<T> action) throws IOException {
			try {
				return action.act();
			} catch (SpallocClient.Exception e) {
				if (e.getResponseCode() == HTTP_UNAUTHORIZED) {
					renew(this::discoverRoot);
					return action.act();
				}
				throw e;
			} catch (IOException e) {
				// Need to read the error message, like a barbarian!
				if (e.getMessage().contains(HTTP_UNAUTHORIZED_MESSAGE)) {
					renew(this::discoverRoot);
					return action.act();
				}
				throw e;
			}
		}

		/**
		 * Discovers the root of a Spalloc service. Also sets up the true CSRF
		 * token handling.
		 *
		 * @return The service root information.
		 * @throws IOException
		 *             If access fails.
		 */
		RootInfo discoverRoot() throws IOException {
			HttpURLConnection conn = connection(SPALLOC_ROOT);
			try (InputStream is =
					checkForError(conn, "couldn't read service root")) {
				RootInfo root = readJson(is, RootInfo.class);
				this.csrfHeader = root.csrfHeader;
				this.csrf = root.csrfToken;
				root.csrfHeader = null;
				root.csrfToken = null;
				return root;
			}
		}
	}

	/**
	 * Create a client.
	 *
	 * @param baseUrl
	 *            Where the server is.
	 * @param username
	 *            The username to log in with.
	 * @param password
	 *            The password to log in with.
	 * @return client API for the given server
	 * @throws IOException
	 *             If the server doesn't respond or logging in fails.
	 */
	public SpallocClient createClient(URI baseUrl, String username,
			String password) throws IOException {
		Session s = new Session(baseUrl, username, password);

		return new ClientImpl(s, s.discoverRoot());
	}

	/**
	 * An action used by {@link Session#withRenewal(Action)}.
	 *
	 * @param <T>
	 *            The type of the result of the action.
	 */
	private interface Action<T> {
		/**
		 * Perform the action.
		 *
		 * @return The result of the action.
		 * @throws IOException
		 *             If network I/O fails.
		 */
		T act() throws IOException;
	}

	private abstract class Common {
		private final SpallocClient client;

		final Session s;

		Common(SpallocClient client, Session s) {
			this.client = client != null ? client : (SpallocClient) this;
			this.s = s;
		}

		final Machine getMachine(String name) throws IOException {
			Machine m;
			do {
				m = machineMap.get(name);
				if (m == null) {
					client.listMachines();
				}
			} while (m == null);
			return m;
		}

		final WhereIs whereis(URI uri) throws IOException {
			return s.withRenewal(() -> {
				HttpURLConnection conn = s.connection(uri);
				WhereIs w;
				try (InputStream is =
						checkForError(conn, "couldn't get board information")) {
					if (conn.getResponseCode() == HTTP_NO_CONTENT) {
						throw new IOException("machine not allocated");
					}
					w = readJson(is, WhereIs.class);
				} catch (FileNotFoundException e) {
					return null;
				} finally {
					s.trackCookie(conn);
				}
				w.setMachineHandle(getMachine(w.getMachineName()));
				return w;
			});
		}
	}

	private final class ClientImpl extends Common implements SpallocClient {
		private Version v;

		private URI jobs;

		private URI machines;

		private ClientImpl(Session s, RootInfo ri) {
			super(null, s);
			this.v = ri.version;
			this.jobs = ri.jobsURI;
			this.machines = ri.machinesURI;
		}

		@Override
		public Version getVersion() {
			return v;
		}

		@Override
		public List<Job> listJobs(boolean wait) throws IOException {
			return s.withRenewal(() -> {
				HttpURLConnection conn =
						wait ? s.connection(jobs, WAIT_FLAG)
								: s.connection(jobs);
				try (InputStream is =
						checkForError(conn, "couldn't list jobs")) {
					return readJson(is, Jobs.class).jobs.stream().map(this::job)
							.collect(toList());
				} finally {
					s.trackCookie(conn);
				}
			});
		}

		@Override
		public Job createJob(CreateJob createInstructions)
				throws IOException {
			URI uri = s.withRenewal(() -> {
				HttpURLConnection conn = s.connection(jobs, true);
				writeObject(conn, createInstructions);
				// Get the response entity... and discard it
				try (InputStream is =
						checkForError(conn, "job create failed")) {
					readLines(is, UTF_8);
					// But we do want the Location header
					return URI.create(conn.getHeaderField("Location"));
				} finally {
					s.trackCookie(conn);
				}
			});
			return job(uri);
		}

		Job job(URI uri) {
			return new JobImpl(this, s, asDir(uri));
		}

		@Override
		public List<Machine> listMachines() throws IOException {
			return s.withRenewal(() -> {
				HttpURLConnection conn = s.connection(machines);
				try (InputStream is =
						checkForError(conn, "list machines failed")) {
					Machines ms = readJson(is, Machines.class);
					// Assume we can cache this
					for (BriefMachineDescription bmd : ms.machines) {
						machineMap.computeIfAbsent(bmd.name,
								name -> new MachineImpl(this, s, bmd));
					}
					return ms.machines.stream()
							.map(bmd -> machineMap.get(bmd.name))
							.collect(toList());
				} finally {
					s.trackCookie(conn);
				}
			});
		}
	}

	private final class JobImpl extends Common implements Job {
		private final URI uri;

		JobImpl(SpallocClient client, Session session, URI uri) {
			super(client, session);
			this.uri = uri;
		}

		@Override
		public JobDescription describe(boolean wait) throws IOException {
			return s.withRenewal(() -> {
				HttpURLConnection conn =
						wait ? s.connection(uri, WAIT_FLAG) : s.connection(uri);
				try (InputStream is =
						checkForError(conn, "couldn't get job state")) {
					return readJson(is, JobDescription.class);
				} finally {
					s.trackCookie(conn);
				}
			});
		}

		@Override
		public void keepalive() throws IOException {
			s.withRenewal(() -> {
				HttpURLConnection conn = s.connection(uri, KEEPALIVE, true);
				conn.setRequestMethod("PUT");
				writeString(conn, "alive");
				try (InputStream is =
						checkForError(conn, "couldn't keep job alive")) {
					return readLines(is, UTF_8);
					// Ignore the output
				} finally {
					s.trackCookie(conn);
				}
			});
		}

		@Override
		public void delete(String reason) throws IOException {
			s.withRenewal(() -> {
				HttpURLConnection conn = s.connection(uri,
						URI.create("?reason=" + encode(reason)), true);
				conn.setRequestMethod("DELETE");
				try (InputStream is =
						checkForError(conn, "couldn't delete job")) {
					readLines(is, UTF_8);
					// Ignore the output
				} finally {
					s.trackCookie(conn);
				}
				return this;
			});
		}

		@Override
		public AllocatedMachine machine() throws IOException {
			AllocatedMachine am = s.withRenewal(() -> {
				HttpURLConnection conn = s.connection(uri, MACHINE);
				try (InputStream is = checkForError(conn,
						"couldn't get allocation description")) {
					if (conn.getResponseCode() == HTTP_NO_CONTENT) {
						throw new IOException("machine not allocated");
					}
					return readJson(is, AllocatedMachine.class);
				} finally {
					s.trackCookie(conn);
				}
			});
			am.setMachine(getMachine(am.getMachineName()));
			return am;
		}

		@Override
		public boolean getPower() throws IOException {
			return s.withRenewal(() -> {
				HttpURLConnection conn = s.connection(uri, POWER);
				try (InputStream is =
						checkForError(conn, "couldn't get power state")) {
					if (conn.getResponseCode() == HTTP_NO_CONTENT) {
						throw new IOException("machine not allocated");
					}
					Power power = readJson(is, Power.class);
					return "ON".equals(power.power);
				} finally {
					s.trackCookie(conn);
				}
			});
		}

		@Override
		public boolean setPower(boolean switchOn) throws IOException {
			Power power = new Power();
			power.power = (switchOn ? "ON" : "OFF");
			return s.withRenewal(() -> {
				HttpURLConnection conn = s.connection(uri, POWER, true);
				conn.setRequestMethod("PUT");
				writeObject(conn, power);
				try (InputStream is =
						checkForError(conn, "couldn't set power state")) {
					if (conn.getResponseCode() == HTTP_NO_CONTENT) {
						throw new IOException("machine not allocated");
					}
					Power power2 = readJson(is, Power.class);
					return "ON".equals(power2.power);
				} finally {
					s.trackCookie(conn);
				}
			});
		}

		@Override
		public WhereIs whereIs(HasChipLocation chip) throws IOException {
			return whereis(uri.resolve(
					format("chip?x=%d&y=%d", chip.getX(), chip.getY())));
		}
	}

	private final class MachineImpl extends Common implements Machine {
		private static final int TRIAD = 3;

		private final BriefMachineDescription bmd;

		private List<BoardCoords> deadBoards;

		private List<DeadLink> deadLinks;

		MachineImpl(SpallocClient client, Session session,
				BriefMachineDescription bmd) {
			super(client, session);
			this.bmd = bmd;
			this.deadBoards = bmd.deadBoards;
			this.deadLinks = bmd.deadLinks;
		}

		@Override
		public String getName() {
			return bmd.name;
		}

		@Override
		public List<String> getTags() {
			return bmd.tags;
		}

		@Override
		public int getWidth() {
			return bmd.width;
		}

		@Override
		public int getHeight() {
			return bmd.height;
		}

		@Override
		public int getLiveBoardCount() {
			return bmd.width * bmd.height * TRIAD - bmd.deadBoards.size();
		}

		@Override
		public List<BoardCoords> getDeadBoards() {
			return deadBoards;
		}

		@Override
		public List<DeadLink> getDeadLinks() {
			return deadLinks;
		}

		@Override
		public void waitForChange() throws IOException {
			BriefMachineDescription nbmd = s.withRenewal(() -> {
				HttpURLConnection conn = s.connection(bmd.uri, WAIT_FLAG);
				try (InputStream is =
						checkForError(conn, "couldn't wait for state change")) {
					return readJson(is, BriefMachineDescription.class);
				} finally {
					s.trackCookie(conn);
				}
			});
			this.deadBoards = nbmd.deadBoards;
			this.deadLinks = nbmd.deadLinks;
		}

		@Override
		public WhereIs getBoardByTriad(int x, int y, int z) throws IOException {
			return whereis(bmd.uri
					.resolve(format("logical-board?x=%d&y=%d&z=%d", x, y, z)));
		}

		@Override
		public WhereIs getBoardByPhysicalCoords(int cabinet, int frame,
				int board) throws IOException {
			return whereis(bmd.uri.resolve(
					format("physical-board?cabinet=%d&frame=%d&board=%d",
							cabinet, frame, board)));
		}

		@Override
		public WhereIs getBoardByChip(HasChipLocation chip) throws IOException {
			return whereis(bmd.uri.resolve(
					format("chip?x=%d&y=%d", chip.getX(), chip.getY())));
		}

		@Override
		public WhereIs getBoardByIPAddress(String address) throws IOException {
			return whereis(bmd.uri
					.resolve(format("board-ip?address=%s", encode(address))));
		}
	}
}
