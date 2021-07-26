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

import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.IOUtils.readLines;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.allocator.SpallocClient.Machine;
import uk.ac.manchester.spinnaker.allocator.SpallocClient.WhereIs;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;

public class SpallocClientFactory {
	private static final Logger log = getLogger(SpallocClientFactory.class);

	private static final URI LOGIN_FORM = URI.create("system/login.html");
	private static final URI LOGIN_HANDLER = URI.create("system/perform_login");
	private static final URI SPALLOC_ROOT = URI.create("srv/spalloc");

	private static final Pattern SESSION_ID_RE =
			Pattern.compile("JSESSIONID=([A-Z0-9]+);");
	private static final Pattern CSRF_ID_RE =
			Pattern.compile("name=\"_csrf\" value=\"([-a-z0-9]+)\"");

	JsonMapper jsonMapper = new JsonMapper();

	private class Session {
		private final URI baseUri;
		private String session;
		private String csrfHeader;
		private String csrf;

		HttpURLConnection connection(URI url, boolean forStateChange)
				throws IOException {
			URI realUrl = baseUri.resolve(url);
			log.info("will connect to {}", realUrl);
			HttpURLConnection c =
					(HttpURLConnection) realUrl.toURL().openConnection();
			if (session != null) {
				log.debug("Attaching to session {}", session);
				c.setRequestProperty("Cookie", "JSESSIONID=" + session);
			}

			if (csrfHeader != null && csrf != null && forStateChange) {
				log.debug("Marking session with token {}={}", csrfHeader, csrf);
				c.setRequestProperty(csrfHeader, csrf);
			}
			if (forStateChange) {
				c.setInstanceFollowRedirects(false);
			}
			return c;
		}

		HttpURLConnection connection(URI url) throws IOException {
			return connection(url, false);
		}

		private Set<String> getCSRF(String line) {
			Matcher m = CSRF_ID_RE.matcher(line);
			if (!m.find()) {
				return emptySet();
			}
			return singleton(m.group(1));
		}

		void trackCookie(HttpURLConnection c) throws IOException {
			String setCookie = c.getHeaderField("Set-Cookie");
			Matcher m = SESSION_ID_RE.matcher(setCookie);
			if (!m.find()) {
				throw new IOException("could not establish session");
			}
			session = m.group(1);
		}

		Session(URI baseURI, String username, String password)
				throws IOException {
			baseUri = baseURI;

			// Create a temporary session so we can log in
			makeTemporarySession();

			// This makes the real session
			logSessionIn(username, password);
		}

		private void makeTemporarySession() throws IOException {
			HttpURLConnection c = connection(LOGIN_FORM);
			try {
				List<String> lines = readLines(c.getInputStream(), UTF_8);
				csrf = lines.stream().flatMap(l -> getCSRF(l).stream())
						.findFirst().orElseThrow(() -> new IOException(
								"could not parse CSRF token"));
				// There's a session cookie at this point; we need it!
				trackCookie(c);
			} finally {
				c.disconnect();
			}
		}

		private void logSessionIn(String username, String password)
				throws IOException, ProtocolException,
				UnsupportedEncodingException {
			HttpURLConnection c = connection(LOGIN_HANDLER, true);
			c.setDoOutput(true);
			c.setRequestMethod("POST");
			try {
				StringBuilder body = new StringBuilder();
				String u = UTF_8.name();
				body.append("_csrf=").append(encode(csrf, u)) //
						.append("&username=").append(encode(username, u)) //
						.append("&password=").append(encode(password, u)) //
						.append("&submit=submit");
				try (Writer wr =
						new OutputStreamWriter(c.getOutputStream(), UTF_8)) {
					wr.write(body.toString());
				}

				if (c.getResponseCode() >= 400) {
					for (String line : readLines(c.getErrorStream(), UTF_8)) {
						log.error(line);
					}
					throw new IOException(
							"login failed: " + c.getResponseCode());
				}
				// There's a new session cookie after login
				trackCookie(c);
			} finally {
				c.disconnect();
			}
		}
	}

	static class RootInfo {
		public Version version;
		@JsonAlias("jobs-ref")
		public URI jobsURI;
		@JsonAlias("machines-ref")
		public URI machinesURI;
		@JsonAlias("csrf-header")
		public String csrfHeader;
		@JsonAlias("csrf-token")
		public String csrfToken;
	}

	static class Machines {
		public List<BriefMachineDescription> machines;
	}

	static class BriefMachineDescription {
		public String name;

		/** The tags of the machine. */
		public List<String> tags;

		/** The URI to the machine. */
		public URI uri;

		public void setURI(String s) {
			if (!s.endsWith("/")) {
				s += "/";
			}
			uri = URI.create(s);
		}

		/** The width of the machine, in triads. */
		public int width;

		/** The height of the machine, in triads. */
		public int height;

		@JsonAlias("dead-boards")
		public List<Object> deadBoards; // FIXME

		@JsonAlias("dead-links")
		public List<Object> deadLinks; // FIXME
	}

	static class Jobs {
		public List<URI> jobs;
	}

	static class JobDescription {
		// FIXME is this needed?
	}

	public SpallocClient createClient(URI baseUrl, String username,
			String password) throws IOException {
		Session s = new Session(baseUrl, username, password);

		URI jobs, machines;
		Version v;

		HttpURLConnection conn = s.connection(SPALLOC_ROOT);
		try (InputStream is = conn.getInputStream()) {
			RootInfo ri = jsonMapper.readValue(is, RootInfo.class);
			s.csrfHeader = ri.csrfHeader;
			s.csrf = ri.csrfToken;
			jobs = ri.jobsURI;
			machines = ri.machinesURI;
			v = ri.version;
		}

		return new SpallocClient() {
			@Override
			public Version getVersion() {
				return v;
			}

			@Override
			public List<Job> listJobs() throws IOException {
				HttpURLConnection conn = s.connection(jobs);
				try (InputStream is = conn.getInputStream()) {
					List<URI> js = jsonMapper.readValue(is, Jobs.class).jobs;
					return js.stream().map(this::job).collect(toList());
				}
			}

			Map<String, Machine> machineMap = new HashMap<>();

			Job job(URI uri) {
				if (!uri.getPath().endsWith("/")) {
					// Bleagh
					uri = URI.create(uri + "/");
				}
				final URI realUri = uri;
				return new Job() {
					@Override
					public String describe() throws IOException {
						HttpURLConnection conn = s.connection(realUri);
						try (InputStream is = conn.getInputStream()) {
							return readLines(is, UTF_8).toString();
						}
					}

					@Override
					public void keepalive() throws IOException {
						// TODO Auto-generated method stub

					}

					@Override
					public void delete(String reason) throws IOException {
						// TODO Auto-generated method stub

					}

					@Override
					public String machine() throws IOException {
						HttpURLConnection conn = s.connection(realUri);
						try (InputStream is = conn.getInputStream()) {
							return readLines(is, UTF_8).toString();
						}
					}

					@Override
					public boolean getPower() throws IOException {
						HttpURLConnection conn = s.connection(realUri);
						try (InputStream is = conn.getInputStream()) {
							readLines(is, UTF_8).toString();
						}
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public void setPower(boolean switchOn) throws IOException {
						// TODO Auto-generated method stub

					}

					@Override
					public WhereIs whereIs(HasChipLocation chip)
							throws IOException {
						HttpURLConnection conn = s.connection(
								realUri.resolve(format("chip?x=%d&y=%d",
										chip.getX(), chip.getY())));
						WhereIs w;
						try (InputStream is = conn.getInputStream()) {
							w = jsonMapper.readValue(is, WhereIs.class);
						} catch (FileNotFoundException e) {
							return null;
						}
						w.machineHandle = machineMap.get(w.machineName);
						return w;
					}
				};
			}

			@Override
			public List<Machine> listMachines() throws IOException {
				HttpURLConnection conn = s.connection(machines);
				try (InputStream is = conn.getInputStream()) {
					Machines ms = jsonMapper.readValue(is, Machines.class);
					// Assume we can cache this
					for (BriefMachineDescription bmd : ms.machines) {
						machineMap.put(bmd.name, new Machine() {
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
								return bmd.width * bmd.height * 3
										- bmd.deadBoards.size();
							}

							private WhereIs whereis(URI uri)
									throws IOException {
								HttpURLConnection conn = s.connection(uri);
								WhereIs w;
								try (InputStream is = conn.getInputStream()) {
									w = jsonMapper.readValue(is, WhereIs.class);
								} catch (FileNotFoundException e) {
									return null;
								}
								w.machineHandle = machineMap.get(w.machineName);
								return w;
							}

							@Override
							public WhereIs getBoardByTriad(int x, int y, int z)
									throws IOException {
								return whereis(bmd.uri.resolve(
										format("logical-board?x=%d&y=%d&z=%d",
												x, y, z)));
							}

							@Override
							public WhereIs getBoardByPhysicalCoords(int cabinet,
									int frame, int board) throws IOException {
								return whereis(bmd.uri.resolve(format(
										"physical-board?"
												+ "cabinet=%d&frame=%d&board=%d",
										cabinet, frame, board)));
							}

							@Override
							public WhereIs getBoardByChip(HasChipLocation chip)
									throws IOException {
								return whereis(
										bmd.uri.resolve(format("chip?x=%d&y=%d",
												chip.getX(), chip.getY())));
							}

							@Override
							public WhereIs getBoardByIPAddress(String address)
									throws IOException {
								return whereis(bmd.uri.resolve(format(
										"board-ip?address=%s",
										encode(address, UTF_8.name()))));
							}
						});
					}
					return ms.machines.stream()
							.map(bmd -> machineMap.get(bmd.name))
							.collect(toList());
				}
			}
		};
	}

	public static void main(String... args)
			throws URISyntaxException, IOException, InterruptedException {
		if (args.length != 3) {
			throw new RuntimeException(
					"need three arguments: <BaseURL> <UserName> <PassWord>");
		}
		URI u = new URI(args[0]);
		SpallocClient client =
				new SpallocClientFactory().createClient(u, args[1], args[2]);
		// Just so that the server gets its logging out the way first
		Thread.sleep(100);
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
			System.out.println(where.machineHandle.getWidth());
			System.out.println(where.logicalCoords);
			System.out.println(where.physicalCoords);
			System.out.println(where.boardChip);
			System.out.println(where.chip);
			System.out.println(where.jobId);
		}
	}
}
