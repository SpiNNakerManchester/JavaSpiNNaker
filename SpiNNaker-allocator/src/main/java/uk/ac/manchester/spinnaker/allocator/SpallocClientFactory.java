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
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.IOUtils.readLines;
import static org.slf4j.LoggerFactory.getLogger;
import static picocli.CommandLine.populateCommand;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.ws.http.HTTPException;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.json.JsonMapper;

import picocli.CommandLine.Parameters;
import uk.ac.manchester.spinnaker.allocator.SpallocClient.AllocatedMachine;
import uk.ac.manchester.spinnaker.allocator.SpallocClient.BoardCoords;
import uk.ac.manchester.spinnaker.allocator.SpallocClient.DeadLink;
import uk.ac.manchester.spinnaker.allocator.SpallocClient.Job;
import uk.ac.manchester.spinnaker.allocator.SpallocClient.JobDescription;
import uk.ac.manchester.spinnaker.allocator.SpallocClient.Machine;
import uk.ac.manchester.spinnaker.allocator.SpallocClient.WhereIs;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;

/**
 * A factory for clients to connect to the Spalloc service.
 *
 * @author Donal Fellows
 */
public class SpallocClientFactory {
	private static final Logger log = getLogger(SpallocClientFactory.class);

	private static final URI LOGIN_FORM = URI.create("system/login.html");

	private static final URI LOGIN_HANDLER = URI.create("system/perform_login");

	private static final URI SPALLOC_ROOT = URI.create("srv/spalloc");

	private static final URI KEEPALIVE = URI.create("keepalive");

	private static final URI POWER = URI.create("power");

	private static final URI WAIT_FLAG = URI.create("?wait=true");

	private static final Pattern SESSION_ID_RE =
			Pattern.compile("JSESSIONID=([A-Z0-9]+);");

	private static final Pattern CSRF_ID_RE =
			Pattern.compile("name=\"_csrf\" value=\"([-a-z0-9]+)\"");

	private JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules()
			.disable(WRITE_DATES_AS_TIMESTAMPS)
			.propertyNamingStrategy(KEBAB_CASE).build();

	private final Map<String, Machine> machineMap =
			synchronizedMap(new HashMap<>());

	private static URI asDir(URI uri) {
		String path = uri.getPath();
		if (!path.endsWith("/")) {
			path += "/";
			uri = uri.resolve(path);
		}
		return uri;
	}

	/**
	 * Manages the login session. This allows us to avoid the (heavy) cost of
	 * the password hashing algorithm used, at least most of the time.
	 *
	 * @author Donal Fellows
	 */
	private class Session {
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
			log.debug("will connect to {}", realUrl);
			HttpURLConnection c =
					(HttpURLConnection) realUrl.toURL().openConnection();
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
			log.debug("will connect to {}", realUrl);
			HttpURLConnection c =
					(HttpURLConnection) realUrl.toURL().openConnection();
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
				c.setRequestProperty("Cookie", "JSESSIONID=" + session);
			}

			if (csrfHeader != null && csrf != null && forStateChange) {
				log.debug("Marking session with token {}={}", csrfHeader, csrf);
				c.setRequestProperty(csrfHeader, csrf);
			}
			c.setInstanceFollowRedirects(false);
		}

		private Set<String> getCSRF(String line) {
			Matcher m = CSRF_ID_RE.matcher(line);
			if (!m.find()) {
				return emptySet();
			}
			return singleton(m.group(1));
		}

		/**
		 * Check for and handle any session cookie changes.
		 *
		 * @param conn
		 *            Connection that's had a transaction processed.
		 * @return Whether the session cookie was set. Normally uninteresting.
		 * @throws IOException
		 *             If things go wrong.
		 */
		boolean trackCookie(HttpURLConnection conn) {
			String setCookie = conn.getHeaderField("Set-Cookie");
			if (setCookie != null) {
				Matcher m = SESSION_ID_RE.matcher(setCookie);
				if (m.find()) {
					session = m.group(1);
					return true;
				}
			}
			return false;
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
			try (InputStream is = c.getInputStream()) {
				// There's a session cookie at this point; we need it!
				if (!trackCookie(c)) {
					throw new IOException("could not establish session");
				}
				// This is nasty; parsing the HTML source
				return readLines(is, UTF_8).stream()
						.flatMap(l -> getCSRF(l).stream()).findFirst()
						.orElseThrow(() -> new IOException(
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
			HttpURLConnection c = connection(LOGIN_HANDLER, true);
			c.setDoOutput(true);
			c.setRequestMethod("POST");
			try {
				StringBuilder body = new StringBuilder();
				String u = UTF_8.name();
				body.append("_csrf=").append(encode(tempCsrf, u)) //
						.append("&username=").append(encode(username, u)) //
						.append("&password=").append(encode(password, u)) //
						.append("&submit=submit");
				try (Writer wr =
						new OutputStreamWriter(c.getOutputStream(), UTF_8)) {
					wr.write(body.toString());
				}

				if (c.getResponseCode() >= HTTP_BAD_REQUEST) {
					for (String line : readLines(c.getErrorStream(), UTF_8)) {
						log.error(line);
					}
					throw new IOException(
							"login failed: " + c.getResponseCode());
				}
				// There should be a new session cookie after login
				if (!trackCookie(c)) {
					throw new IOException("could not establish session");
				}
			} finally {
				c.disconnect();
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
		 * @param csrfRenewer
		 *            How to renew the CSRF token, if that's desired.
		 * @return The result of the action
		 * @throws IOException
		 *             If things go wrong.
		 */
		<T> T withRenewal(Action<?> csrfRenewer, Action<T> action)
				throws IOException {
			try {
				return action.act();
			} catch (HTTPException e) {
				if (e.getStatusCode() == HTTP_UNAUTHORIZED) {
					renew(csrfRenewer);
					return action.act();
				}
				throw e;
			}
		}

		/**
		 * Install the real CSRF controls. The session does not know how to
		 * obtain these values!
		 *
		 * @param csrfHeader
		 *            the name of the CSRF header
		 * @param csrfToken
		 *            the value to put in the CSRF header
		 */
		void setCSRF(String csrfHeader, String csrfToken) {
			this.csrfHeader = csrfHeader;
			this.csrf = csrfToken;
		}
	}

	static class RootInfo {
		/** Service version. */
		Version version;

		/** Where to look up jobs. */
		URI jobsURI;

		/** Where to look up machines. */
		URI machinesURI;

		/** CSRF header name. */
		String csrfHeader;

		/** CSRF token value. */
		String csrfToken;

		public void setVersion(Version version) {
			this.version = version;
		}

		@JsonAlias("jobs-ref")
		public void setJobsURI(URI jobsURI) {
			this.jobsURI = jobsURI;
		}

		@JsonAlias("machines-ref")
		public void setMachinesURI(URI machinesURI) {
			this.machinesURI = machinesURI;
		}

		@JsonAlias("csrf-header")
		public void setCsrfHeader(String csrfHeader) {
			this.csrfHeader = csrfHeader;
		}

		@JsonAlias("csrf-token")
		public void setCsrfToken(String csrfToken) {
			this.csrfToken = csrfToken;
		}
	}

	static class Machines {
		/** The machine info. */
		List<BriefMachineDescription> machines;

		public void setMachines(List<BriefMachineDescription> machines) {
			this.machines = machines;
		}
	}

	static class BriefMachineDescription {
		/** The machine name. */
		String name;

		/** The tags of the machine. */
		List<String> tags;

		/** The URI to the machine. */
		URI uri;

		/** The width of the machine, in triads. */
		int width;

		/** The height of the machine, in triads. */
		int height;

		/** The dead boards of the machine. */
		List<BoardCoords> deadBoards;

		/** The dead links of the machine. */
		List<DeadLink> deadLinks;

		public void setName(String name) {
			this.name = name;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}

		public void setUri(URI uri) {
			this.uri = asDir(uri);
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		@JsonAlias("dead-boards")
		public void setDeadBoards(List<BoardCoords> deadBoards) {
			this.deadBoards = deadBoards;
		}

		@JsonAlias("dead-links")
		public void setDeadLinks(List<DeadLink> deadLinks) {
			this.deadLinks = deadLinks;
		}
	}

	static class Jobs {
		/** The jobs of the machine. */
		List<URI> jobs;

		public void setJobs(List<URI> jobs) {
			this.jobs = jobs;
		}
	}

	static class Power {
		/** The power state. */
		String power;

		public void setPower(String power) {
			this.power = power;
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

		URI jobs, machines;
		Version v;

		RootInfo ri = s.withRenewal(null, () -> {
			HttpURLConnection conn = s.connection(SPALLOC_ROOT);
			try (InputStream is = conn.getInputStream()) {
				return jsonMapper.readValue(is, RootInfo.class);
			}
		});
		s.setCSRF(ri.csrfHeader, ri.csrfToken);
		jobs = ri.jobsURI;
		machines = ri.machinesURI;
		v = ri.version;

		Action<Object> csrfRenew = () -> {
			HttpURLConnection conn = s.connection(SPALLOC_ROOT);
			try (InputStream is = conn.getInputStream()) {
				RootInfo root = jsonMapper.readValue(is, RootInfo.class);
				s.setCSRF(root.csrfHeader, root.csrfToken);
			}
			return null;
		};

		return new SpallocClient() {
			Session session = s;

			@Override
			public Version getVersion() {
				return v;
			}

			@Override
			public List<Job> listJobs(boolean wait) throws IOException {
				return session.withRenewal(csrfRenew, () -> {
					HttpURLConnection conn =
							wait ? session.connection(jobs, WAIT_FLAG)
									: session.connection(jobs);
					try (InputStream is = conn.getInputStream()) {
						return jsonMapper.readValue(is, Jobs.class).jobs
								.stream().map(this::job).collect(toList());
					} finally {
						session.trackCookie(conn);
					}
				});
			}

			@Override
			public Job createJob(CreateJob createInstructions)
					throws IOException {
				URI uri = session.withRenewal(csrfRenew, () -> {
					HttpURLConnection conn = session.connection(jobs, true);
					conn.setDoOutput(true);
					conn.setRequestProperty("Content-Type", "application/json");
					try (OutputStream os = conn.getOutputStream()) {
						jsonMapper.writeValue(os, createInstructions);
					}
					// Get the response entity... and discard it
					try (InputStream is = conn.getInputStream()) {
						readLines(is, UTF_8);
					} finally {
						session.trackCookie(conn);
					}
					return URI.create(conn.getHeaderField("Location"));
				});
				return job(uri);
			}

			Job job(URI uri) {
				return new JobImpl(this, session, csrfRenew, asDir(uri));
			}

			@Override
			public List<Machine> listMachines() throws IOException {
				return session.withRenewal(csrfRenew, () -> {
					HttpURLConnection conn = session.connection(machines);
					try (InputStream is = conn.getInputStream()) {
						Machines ms = jsonMapper.readValue(is, Machines.class);
						// Assume we can cache this
						for (BriefMachineDescription bmd : ms.machines) {
							machineMap.computeIfAbsent(bmd.name,
									name -> new MachineImpl(this, session,
											csrfRenew, bmd));
						}
						return ms.machines.stream()
								.map(bmd -> machineMap.get(bmd.name))
								.collect(toList());
					} finally {
						session.trackCookie(conn);
					}
				});
			}
		};
	}

	private interface Action<T> {
		T act() throws IOException;
	}

	private class Reinit {
		private final SpallocClient client;

		final Session s;

		final Action<?> csrfRenew;

		Reinit(SpallocClient client, Session s, Action<?> csrfRenew) {
			this.client = client;
			this.s = s;
			this.csrfRenew = csrfRenew;
		}

		Machine getMachine(String name) throws IOException {
			Machine m;
			do {
				m = machineMap.get(name);
				if (m == null) {
					client.listMachines();
				}
			} while (m == null);
			return m;
		}

		WhereIs whereis(URI uri) throws IOException {
			return s.withRenewal(csrfRenew, () -> {
				HttpURLConnection conn = s.connection(uri);
				WhereIs w;
				try (InputStream is = conn.getInputStream()) {
					if (conn.getResponseCode() == HTTP_NO_CONTENT) {
						throw new IOException("machine not allocated");
					}
					w = jsonMapper.readValue(is, WhereIs.class);
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

	private class JobImpl extends Reinit implements Job {
		private final URI uri;

		JobImpl(SpallocClient client, Session session, Action<?> csrfRenew,
				URI uri) {
			super(client, session, csrfRenew);
			this.uri = uri;
		}

		@Override
		public JobDescription describe(boolean wait) throws IOException {
			return s.withRenewal(csrfRenew, () -> {
				HttpURLConnection conn =
						wait ? s.connection(uri, WAIT_FLAG) : s.connection(uri);
				try (InputStream is = conn.getInputStream()) {
					return jsonMapper.readValue(is, JobDescription.class);
				} finally {
					s.trackCookie(conn);
				}
			});
		}

		@Override
		public void keepalive() throws IOException {
			s.withRenewal(csrfRenew, () -> {
				HttpURLConnection conn = s.connection(uri, KEEPALIVE, true);
				conn.setRequestMethod("PUT");
				conn.setDoOutput(true);
				conn.setRequestProperty("Content-Type", "text/plain");
				try (PrintWriter pw = new PrintWriter(conn.getOutputStream())) {
					pw.println("alive");
				}
				try (InputStream is = conn.getInputStream()) {
					return readLines(is, UTF_8);
					// Ignore the output
				} finally {
					s.trackCookie(conn);
				}
			});
		}

		@Override
		public void delete(String reason) throws IOException {
			s.withRenewal(csrfRenew, () -> {
				HttpURLConnection conn = s.connection(uri,
						URI.create("?reason=" + encode(reason, UTF_8.name())),
						true);
				conn.setRequestMethod("DELETE");
				try (InputStream is = conn.getInputStream()) {
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
			AllocatedMachine am = s.withRenewal(csrfRenew, () -> {
				HttpURLConnection conn = s.connection(uri);
				try (InputStream is = conn.getInputStream()) {
					if (conn.getResponseCode() == HTTP_NO_CONTENT) {
						throw new IOException("machine not allocated");
					}
					return jsonMapper.readValue(is, AllocatedMachine.class);
				} finally {
					s.trackCookie(conn);
				}
			});
			am.setMachine(getMachine(am.getMachineName()));
			return am;
		}

		@Override
		public boolean getPower() throws IOException {
			return s.withRenewal(csrfRenew, () -> {
				HttpURLConnection conn = s.connection(uri, POWER);
				try (InputStream is = conn.getInputStream()) {
					if (conn.getResponseCode() == HTTP_NO_CONTENT) {
						throw new IOException("machine not allocated");
					}
					Power power = jsonMapper.readValue(is, Power.class);
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
			return s.withRenewal(csrfRenew, () -> {
				HttpURLConnection conn = s.connection(uri, POWER, true);
				conn.setRequestMethod("PUT");
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setDoOutput(true);
				try (OutputStream os = conn.getOutputStream()) {
					jsonMapper.writeValue(os, power);
				}
				try (InputStream is = conn.getInputStream()) {
					if (conn.getResponseCode() == HTTP_NO_CONTENT) {
						throw new IOException("machine not allocated");
					}
					Power power2 = jsonMapper.readValue(is, Power.class);
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

	private class MachineImpl extends Reinit implements Machine {
		private static final int TRIAD = 3;

		private final BriefMachineDescription bmd;

		private List<BoardCoords> deadBoards;

		private List<DeadLink> deadLinks;

		MachineImpl(SpallocClient client, Session session, Action<?> csrfRenew,
				BriefMachineDescription bmd) {
			super(client, session, csrfRenew);
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
			BriefMachineDescription nbmd = s.withRenewal(csrfRenew, () -> {
				HttpURLConnection conn = s.connection(bmd.uri, WAIT_FLAG);
				try (InputStream is = conn.getInputStream()) {
					return jsonMapper.readValue(is,
							BriefMachineDescription.class);
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
					format("physical-board?" + "cabinet=%d&frame=%d&board=%d",
							cabinet, frame, board)));
		}

		@Override
		public WhereIs getBoardByChip(HasChipLocation chip) throws IOException {
			return whereis(bmd.uri.resolve(
					format("chip?x=%d&y=%d", chip.getX(), chip.getY())));
		}

		@Override
		public WhereIs getBoardByIPAddress(String address) throws IOException {
			return whereis(bmd.uri.resolve(format("board-ip?address=%s",
					encode(address, UTF_8.name()))));
		}
	}

	// TODO move testing stuff elsewhere
	static class TestingClientArgs {
		@Parameters(index = "0", paramLabel = "BaseURL")
		private URI baseUrl;

		@Parameters(index = "1", paramLabel = "UserName")
		private String username;

		@Parameters(index = "2", paramLabel = "PassWord")
		private String password;
	}

	private static final int SHORT_SLEEP = 100;

	public static void main(String... args)
			throws URISyntaxException, IOException, InterruptedException {
		TestingClientArgs a = populateCommand(new TestingClientArgs(), args);
		SpallocClientFactory factory = new SpallocClientFactory();
		SpallocClient client =
				factory.createClient(a.baseUrl, a.username, a.password);

		// Just so that the server gets its logging out the way first
		Thread.sleep(SHORT_SLEEP);

		System.out.println(client.getVersion());
		System.out.println(client.listMachines().stream().map(m -> m.getName())
				.collect(toList()));
		for (Machine m : client.listMachines()) {
			WhereIs where = m.getBoardByTriad(0, 0, 1);
			if (where == null) {
				System.out
						.println("board (0,0,1) not in machine " + m.getName());
				continue;
			}
			System.out.println(where.getMachineHandle().getWidth());
			System.out.println(where.getLogicalCoords());
			System.out.println(where.getPhysicalCoords());
			System.out.println(where.getBoardChip());
			System.out.println(where.getChip());
			System.out.println(where.getJobId());
		}
		// Check this directly here
		factory.jsonMapper.readValue("{\"x\":1,\"y\":2,\"z\":3,"
				+ "\"cabinet\":4,\"frame\":5,\"board\":6,"
				+ "\"address\":\"127.0.0.2\"}", BoardCoords.class);
	}
}
