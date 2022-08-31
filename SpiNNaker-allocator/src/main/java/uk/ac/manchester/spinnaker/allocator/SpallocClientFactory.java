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
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.IOUtils.readLines;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.allocator.SpallocClient.Job;
import uk.ac.manchester.spinnaker.allocator.SpallocClient.Machine;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;

/**
 * A factory for clients to connect to the Spalloc service.
 * <p>
 * <strong>Implementation Note:</strong> Neither this class nor the client
 * classes it creates maintain state that needs to be closed explicitly.
 *
 * @author Donal Fellows
 */
public class SpallocClientFactory {
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
			.propertyNamingStrategy(KEBAB_CASE).build();

	/**
	 * Add a {@code /} to the end of the path part of a URI.
	 *
	 * @param uri
	 *            The URI to amend. Assumed to be HTTP or HTTPS.
	 * @return The amended URI.
	 */
	static URI asDir(URI uri) {
		var path = uri.getPath();
		if (!path.endsWith("/")) {
			path += "/";
			uri = uri.resolve(path);
		}
		return uri;
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
	 * @throws SpallocClient.Exception
	 *             on other server errors
	 */
	static InputStream checkForError(HttpURLConnection conn,
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
		var s = new ClientSession(baseUrl, username, password);

		return new ClientImpl(s, s.discoverRoot());
	}
	// TODO Make a constructor that takes a bearer token

	private abstract class Common {
		private final SpallocClient client;

		final ClientSession s;

		/**
		 * Cache of machines, which don't expire.
		 */
		final Map<String, Machine> machineMap =
				synchronizedMap(new HashMap<>());

		Common(SpallocClient client, ClientSession s) {
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
				w.setMachineRef(null);
				return w;
			});
		}
	}

	private final class ClientImpl extends Common implements SpallocClient {
		private Version v;

		private URI jobs;

		private URI machines;

		private ClientImpl(ClientSession s, RootInfo ri) {
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
				if (first != null) {
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
				if (first != null) {
					return true;
				}
				return next != null;
			}
		}

		private Stream<Job> listJobs(URI flags) throws IOException {
			var basicData = new JobLister(
					flags != null ? jobs.resolve(flags) : jobs);
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

		JobImpl(SpallocClient client, ClientSession session, URI uri) {
			super(client, session);
			this.uri = uri;
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

		@Override
		public void delete(String reason) throws IOException {
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
			return s.withRenewal(() -> {
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

		MachineImpl(SpallocClient client, ClientSession session,
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
			var nbmd = s.withRenewal(() -> {
				var conn = s.connection(bmd.uri, WAIT_FLAG);
				try (var is = checkForError(conn,
						"couldn't wait for state change")) {
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
			return whereis(bmd.uri.resolve(
					format("board-ip?address=%s", encode(address, UTF_8))));
		}
	}
}
