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

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.IOUtils.readLines;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.client.ClientUtils.asDir;
import static uk.ac.manchester.spinnaker.utils.InetFactory.getByNameQuietly;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.alloc.client.SpallocClient.Job;
import uk.ac.manchester.spinnaker.alloc.client.SpallocClient.Machine;
import uk.ac.manchester.spinnaker.alloc.client.SpallocClient.SpallocException;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MachineVersion;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.storage.ProxyInformation;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;
import uk.ac.manchester.spinnaker.utils.Daemon;

/**
 * A factory for clients to connect to the Spalloc service.
 * <p>
 * <strong>Implementation Note:</strong> Neither this class nor the client
 * classes it creates maintain state that needs to be closed explicitly
 * <em>except</em> for
 * {@linkplain SpallocClient.Job#getTransceiver() transceivers}, as transceivers
 * usually need to be closed.
 *
 * @author Donal Fellows
 */
public class SpallocClientFactory {
	private static final Logger log = getLogger(SpallocClientFactory.class);

	private static final String CONTENT_TYPE = "Content-Type";

	private static final String TEXT_PLAIN = "text/plain; charset=UTF-8";

	private static final String APPLICATION_JSON = "application/json";

	private static final String FORM_ENCODED =
			"application/x-www-form-urlencoded";

	private static final URI KEEPALIVE = URI.create("keepalive");

	private static final URI MACHINE = URI.create("machine");

	private static final URI POWER = URI.create("power");

	private static final URI WAIT_FLAG = URI.create("?wait=true");

	/** Used to convert to/from JSON. */
	static final JsonMapper JSON_MAPPER = JsonMapper.builder()
			.findAndAddModules().disable(WRITE_DATES_AS_TIMESTAMPS)
			.addModule(new JavaTimeModule())
			.propertyNamingStrategy(KEBAB_CASE).build();

	private final URI baseUrl;

	/**
	 * Cache of machines, which don't expire.
	 */
	private static final Map<String, Machine> MACHINE_MAP =
			synchronizedMap(new HashMap<>());

	/**
	 * Create a factory that can talk to a given service.
	 *
	 * @param baseUrl
	 *            Where the server is.
	 */
	public SpallocClientFactory(URI baseUrl) {
		this.baseUrl = asDir(baseUrl);
	}

	/**
	 * Get a handle to a job given its proxy access information (derived from a
	 * database query).
	 *
	 * @param proxy
	 *            The proxy information from the database. Handles {@code null}.
	 * @return The job handle, or {@code null} if {@code proxy==null}.
	 * @throws IOException
	 *             If connecting to the job fails.
	 */
	public static Job getJobFromProxyInfo(ProxyInformation proxy)
			throws IOException {
		if (proxy == null) {
			return null;
		}
		log.info("Using proxy {} for connections", proxy.spallocUrl);
		return new SpallocClientFactory(URI.create(proxy.spallocUrl))
				.getJob(proxy.jobUrl, proxy.headers, proxy.cookies);
	}

	/**
	 * Read an object from a stream.
	 *
	 * @param <T>
	 *            The type of the object to read.
	 * @param is
	 *            The stream
	 * @param cls
	 *            The class of object to read.
	 * @return The object
	 * @throws IOException
	 *             If an I/O error happens or the content on the stream can't be
	 *             made into an instance of the given class.
	 */
	static <T> T readJson(InputStream is, Class<T> cls) throws IOException {
		return JSON_MAPPER.readValue(is, cls);
	}

	/**
	 * Outputs a form to a connection in
	 * {@code application/x-www-form-urlencoded} format.
	 *
	 * @param connection
	 *            The connection. Must have the right verb set.
	 * @param map
	 *            The contents of the form.
	 * @throws IOException
	 *             If I/O fails.
	 */
	static void writeForm(HttpURLConnection connection, Map<String, String> map)
			throws IOException {
		var form = map.entrySet().stream()
				.map(e -> e.getKey() + "=" + encode(e.getValue(), UTF_8))
				.collect(joining("&"));

		connection.setDoOutput(true);
		connection.setRequestProperty(CONTENT_TYPE, FORM_ENCODED);
		try (var w =
				new OutputStreamWriter(connection.getOutputStream(), UTF_8)) {
			w.write(form);
		}
	}

	/**
	 * Outputs an object to a connection in {@code application/json} format.
	 *
	 * @param connection
	 *            The connection. Must have the right verb set.
	 * @param object
	 *            The object to write.
	 * @throws IOException
	 *             If I/O fails.
	 */
	static void writeObject(HttpURLConnection connection, Object object)
			throws IOException {
		connection.setDoOutput(true);
		connection.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
		try (var out = connection.getOutputStream()) {
			JSON_MAPPER.writeValue(out, object);
		}
	}

	/**
	 * Outputs a string to a connection in {@code text/plain} format.
	 *
	 * @param connection
	 *            The connection. Must have the right verb set.
	 * @param string
	 *            The string to write.
	 * @throws IOException
	 *             If I/O fails.
	 */
	static void writeString(HttpURLConnection connection, String string)
			throws IOException {
		connection.setDoOutput(true);
		connection.setRequestProperty(CONTENT_TYPE, TEXT_PLAIN);
		try (var w = new OutputStreamWriter(connection.getOutputStream(),
				UTF_8)) {
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
	 * @throws SpallocException
	 *             on other server errors
	 */
	@MustBeClosed
	static InputStream checkForError(HttpURLConnection conn,
			String errorMessage) throws IOException {
		if (conn.getResponseCode() == HTTP_NOT_FOUND) {
			// Special case
			throw new FileNotFoundException(errorMessage);
		}
		if (conn.getResponseCode() >= HTTP_BAD_REQUEST) {
			throw new SpallocException(conn.getErrorStream(),
					conn.getResponseCode());
		}
		return conn.getInputStream();
	}

	/**
	 * Create a client and log in.
	 *
	 * @param username
	 *            The username to log in with.
	 * @param password
	 *            The password to log in with.
	 * @return The client API for the given server.
	 * @throws IOException
	 *             If the server doesn't respond or logging in fails.
	 */
	public SpallocClient login(String username, String password)
			throws IOException {
		var s = new ClientSession(baseUrl, username, password);

		return new ClientImpl(s, s.discoverRoot());
	}

	/**
	 * Get direct access to a Job.
	 *
	 * @param uri
	 *            The URI of the job
	 * @param headers
	 *            The headers to read authentication from.
	 * @param cookies
	 *            The cookies to read authentication from.
	 * @return A job.
	 * @throws IOException
	 *             If there is an error communicating with the server.
	 */
	public Job getJob(String uri, Map<String, String> headers,
			Map<String, String> cookies) throws IOException {
		var u = URI.create(uri);
		var s = new ClientSession(baseUrl, headers, cookies);
		var c = new ClientImpl(s, s.discoverRoot());
		log.info("Connecting to job on {}", u);
		return c.job(u);
	}

	private abstract static class Common {
		private final SpallocClient client;

		final Session s;

		Common(SpallocClient client, Session s) {
			this.client = client != null ? client : (SpallocClient) this;
			this.s = s;
		}

		final Machine getMachine(String name) throws IOException {
			Machine m = MACHINE_MAP.get(name);
			if (m == null) {
				client.listMachines();
				m = MACHINE_MAP.get(name);
			}
			if (m == null) {
				throw new IOException("Machine " + name + " not found");
			}
			return m;
		}

		private WhereIs whereis(HttpURLConnection conn) throws IOException {
			try (var is = checkForError(conn,
					"couldn't get board information")) {
				if (conn.getResponseCode() == HTTP_NO_CONTENT) {
					throw new FileNotFoundException("machine not allocated");
				}
				return readJson(is, WhereIs.class);
			} finally {
				s.trackCookie(conn);
			}
		}

		final WhereIs whereis(URI uri) throws IOException {
			return s.withRenewal(() -> {
				var conn = s.connection(uri);
				var w = whereis(conn);
				w.setMachineHandle(getMachine(w.getMachineName()));
				w.clearMachineRef();
				return w;
			});
		}
	}

	private static final class ClientImpl extends Common
			implements SpallocClient {
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

		/**
		 * Slightly convoluted class to fetch jobs. The complication means we
		 * get the initial failure exception nice and early, while we're ready
		 * for it. This code would be quite a lot simpler if we didn't want to
		 * get the exception during construction.
		 */
		private class JobLister extends ListFetchingIter<URI> {
			private URI next;

			private List<URI> first;

			JobLister(URI initial) throws IOException {
				var first = getJobList(s.connection(initial));
				next = first.next;
				this.first = first.jobs;
			}

			private Jobs getJobList(HttpURLConnection conn) throws IOException {
				try (var is = checkForError(conn, "couldn't list jobs")) {
					return readJson(is, Jobs.class);
				} finally {
					s.trackCookie(conn);
				}
			}

			@Override
			List<URI> fetchNext() throws IOException {
				if (nonNull(first)) {
					try {
						return first;
					} finally {
						first = null;
					}
				}
				var j = getJobList(s.connection(next));
				next = j.next;
				return j.jobs;
			}

			@Override
			boolean canFetchMore() {
				if (nonNull(first)) {
					return true;
				}
				return nonNull(next);
			}
		}

		private Stream<Job> listJobs(URI flags) throws IOException {
			var basicData = new JobLister(
					nonNull(flags) ? jobs.resolve(flags) : jobs);
			return basicData.stream().flatMap(Collection::stream)
					.map(this::job);
		}

		@Override
		public List<Job> listJobs(boolean wait) throws IOException {
			return s.withRenewal(() -> listJobs(WAIT_FLAG)).collect(toList());
		}

		@Override
		public Stream<Job> listJobsWithDeleted(boolean wait)
				throws IOException {
			var opts = new StringBuilder("?deleted=true");
			if (wait) {
				opts.append("&wait=true");
			}
			return s.withRenewal(() -> listJobs(URI.create(opts.toString())));
		}

		@Override
		public Job createJob(CreateJob createInstructions) throws IOException {
			var uri = s.withRenewal(() -> {
				var conn = s.connection(jobs, true);
				writeObject(conn, createInstructions);
				// Get the response entity... and discard it
				try (var is = checkForError(conn, "job create failed")) {
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
				var conn = s.connection(machines);
				try (var is = checkForError(conn, "list machines failed")) {
					var ms = readJson(is, Machines.class);
					// Assume we can cache this
					for (var bmd : ms.machines) {
						log.debug("Machine {} found", bmd.name);
						MACHINE_MAP.put(bmd.name,
								new MachineImpl(this, s, bmd));
					}
					return new ArrayList<Machine>(MACHINE_MAP.values());
				} finally {
					s.trackCookie(conn);
				}
			});
		}
	}

	private static final class JobImpl extends Common implements Job {
		private final URI uri;

		private volatile boolean dead;

		@GuardedBy("lock")
		private ProxyProtocolClient proxy;

		private final Object lock = new Object();

		JobImpl(SpallocClient client, Session session, URI uri) {
			super(client, session);
			this.uri = uri;
			this.dead = false;
			startKeepalive();
		}

		@Override
		public JobDescription describe(boolean wait) throws IOException {
			return s.withRenewal(() -> {
				var conn = wait ? s.connection(uri, WAIT_FLAG)
						: s.connection(uri);
				try (var is = checkForError(conn, "couldn't get job state")) {
					return readJson(is, JobDescription.class);
				} finally {
					s.trackCookie(conn);
				}
			});
		}

		@Override
		public void keepalive() throws IOException {
			s.withRenewal(() -> {
				var conn = s.connection(uri, KEEPALIVE, true);
				conn.setRequestMethod("PUT");
				writeString(conn, "alive");
				try (var is = checkForError(conn, "couldn't keep job alive")) {
					return readLines(is, UTF_8);
					// Ignore the output
				} finally {
					s.trackCookie(conn);
				}
			});
		}

		private static final int DELAY = 20 * MSEC_PER_SEC;

		private void startKeepalive() {
			if (dead) {
				throw new IllegalStateException("job is already deleted");
			}
			var t = new Daemon(() -> {
				try {
					while (true) {
						sleep(DELAY);
						if (dead) {
							break;
						}
						keepalive();
					}
				} catch (IOException e) {
					log.warn("failed to keep job alive for {}", this, e);
				} catch (InterruptedException e) {
					// If interrupted, we're simply done
				}
			});
			t.setName("keepalive for " + uri);
			t.setUncaughtExceptionHandler((th, e) -> {
				log.warn("unexpected exception in {}", th, e);
			});
			t.start();
		}

		@Override
		public void delete(String reason) throws IOException {
			dead = true;
			s.withRenewal(() -> {
				var conn = s.connection(uri, "?reason=" + encode(reason, UTF_8),
						true);
				conn.setRequestMethod("DELETE");
				try (var is = checkForError(conn, "couldn't delete job")) {
					readLines(is, UTF_8);
					// Ignore the output
				} finally {
					s.trackCookie(conn);
				}
				return this;
			});
			synchronized (lock) {
				if (haveProxy()) {
					proxy.close();
					proxy = null;
				}
			}
		}

		@Override
		public AllocatedMachine machine() throws IOException {
			var am = s.withRenewal(() -> {
				var conn = s.connection(uri, MACHINE);
				try (var is = checkForError(conn,
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
				var conn = s.connection(uri, POWER);
				try (var is = checkForError(conn, "couldn't get power state")) {
					if (conn.getResponseCode() == HTTP_NO_CONTENT) {
						throw new IOException("machine not allocated");
					}
					return "ON".equals(readJson(is, Power.class).power);
				} finally {
					s.trackCookie(conn);
				}
			});
		}

		@Override
		public boolean setPower(boolean switchOn) throws IOException {
			var power = new Power();
			power.power = (switchOn ? "ON" : "OFF");
			boolean powered = s.withRenewal(() -> {
				var conn = s.connection(uri, POWER, true);
				conn.setRequestMethod("PUT");
				writeObject(conn, power);
				try (var is = checkForError(conn, "couldn't set power state")) {
					if (conn.getResponseCode() == HTTP_NO_CONTENT) {
						throw new IOException("machine not allocated");
					}
					return "ON".equals(readJson(is, Power.class).power);
				} finally {
					s.trackCookie(conn);
				}
			});
			if (!powered) {
				// If someone turns the power off, close the proxy
				synchronized (lock) {
					if (haveProxy()) {
						proxy.close();
						proxy = null;
					}
				}
			}
			return powered;
		}

		@Override
		public WhereIs whereIs(HasChipLocation chip) throws IOException {
			return whereis(uri.resolve(
					format("chip?x=%d&y=%d", chip.getX(), chip.getY())));
		}

		@GuardedBy("lock")
		private boolean haveProxy() {
			return nonNull(proxy) && proxy.isOpen();
		}

		/**
		 * @return The websocket-based proxy.
		 * @throws IOException
		 *             if we can't connect
		 * @throws InterruptedException
		 *             if we're interrupted while connecting
		 */
		private ProxyProtocolClient getProxy()
				throws IOException, InterruptedException {
			synchronized (lock) {
				if (haveProxy()) {
					return proxy;
				}
			}
			var wssAddr = describe(false).getProxyAddress();
			if (isNull(wssAddr)) {
				throw new IOException("machine not allocated");
			}
			synchronized (lock) {
				if (!haveProxy()) {
					proxy = s.withRenewal(() -> s.websocket(wssAddr));
				}
				return proxy;
			}
		}

		@MustBeClosed
		@Override
		public TransceiverInterface getTransceiver()
				throws IOException, InterruptedException, SpinnmanException {
			var ws = getProxy();
			var am = machine();
			// TODO: We should know if the machine wraps around or not here...
			var version = MachineVersion.TRIAD_NO_WRAPAROUND;
			var conns = new ArrayList<Connection>();
			var hostToChip = new HashMap<Inet4Address, ChipLocation>();
			InetAddress bootChipAddress = null;
			for (var bc : am.getConnections()) {
				var chipAddr = getByNameQuietly(bc.getHostname());
				var chipLoc = bc.getChip().asChipLocation();
				conns.add(new ProxiedSCPConnection(chipLoc, ws, chipAddr));
				hostToChip.put(chipAddr, bc.getChip());
				if (chipLoc.equals(ZERO_ZERO)) {
					bootChipAddress = chipAddr;
				}
			}
			if (bootChipAddress != null) {
				conns.add(new ProxiedBootConnection(ws, bootChipAddress));
			}
			return new ProxiedTransceiver(version, conns, hostToChip, ws);
		}

		@Override
		public String toString() {
			return "Job(" + uri + ")";
		}
	}

	private static final class MachineImpl extends Common implements Machine {
		private static final int TRIAD = 3;

		private final BriefMachineDescription bmd;

		private List<BoardCoords> deadBoards;

		private List<DeadLink> deadLinks;

		MachineImpl(SpallocClient client, Session session,
				BriefMachineDescription bmd) {
			super(client, session);
			this.bmd = bmd;
			this.deadBoards = List.copyOf(bmd.deadBoards);
			this.deadLinks = List.copyOf(bmd.deadLinks);
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
			var nbmd = s.withRenewal(() -> {
				var conn = s.connection(bmd.uri, WAIT_FLAG);
				try (var is = checkForError(conn,
						"couldn't wait for state change")) {
					return readJson(is, BriefMachineDescription.class);
				} finally {
					s.trackCookie(conn);
				}
			});
			this.deadBoards = List.copyOf(nbmd.deadBoards);
			this.deadLinks = List.copyOf(nbmd.deadLinks);
		}

		@Override
		public WhereIs getBoard(TriadCoords coords) throws IOException {
			return whereis(
					bmd.uri.resolve(format("logical-board?x=%d&y=%d&z=%d",
							coords.x, coords.y, coords.z)));
		}

		@Override
		public WhereIs getBoard(PhysicalCoords coords) throws IOException {
			return whereis(bmd.uri.resolve(
					format("physical-board?cabinet=%d&frame=%d&board=%d",
							coords.c, coords.f, coords.b)));
		}

		@Override
		public WhereIs getBoard(HasChipLocation chip) throws IOException {
			return whereis(bmd.uri.resolve(
					format("chip?x=%d&y=%d", chip.getX(), chip.getY())));
		}

		@Override
		public WhereIs getBoard(String address) throws IOException {
			return whereis(bmd.uri.resolve(
					format("board-ip?address=%s", encode(address, UTF_8))));
		}
	}
}
