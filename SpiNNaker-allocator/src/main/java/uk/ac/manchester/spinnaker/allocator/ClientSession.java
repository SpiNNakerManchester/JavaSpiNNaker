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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.apache.commons.io.IOUtils.readLines;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.allocator.SpallocClientFactory.asDir;
import static uk.ac.manchester.spinnaker.allocator.SpallocClientFactory.checkForError;
import static uk.ac.manchester.spinnaker.allocator.SpallocClientFactory.readJson;
import static uk.ac.manchester.spinnaker.allocator.SpallocClientFactory.writeForm;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;

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

	/**
	 * An action used by {@link ClientSession#withRenewal(Action)
	 * withRenewal()}. The action will be performed once, and if it fails with a
	 * permission fault, the session will be renewed and the action performed
	 * exactly once more.
	 *
	 * @param <T>
	 *            The type of the result of the action.
	 */
	interface Action<T> {
		/**
		 * Perform the action.
		 *
		 * @return The result of the action.
		 * @throws IOException
		 *             If network I/O fails.
		 */
		T act() throws IOException;
	}

	private static HttpURLConnection createConnection(URI url)
			throws IOException {
		log.debug("will connect to {}", url);
		HttpURLConnection c = (HttpURLConnection) url.toURL().openConnection();
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
		URI realUrl = baseUri.resolve(url);
		HttpURLConnection c = createConnection(realUrl);
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
		URI realUrl = baseUri.resolve(url).resolve(url2);
		HttpURLConnection c = createConnection(realUrl);
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
		URI realUrl = baseUri.resolve(url).resolve(url2);
		HttpURLConnection c = createConnection(realUrl);
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
	private synchronized void renew(boolean postRenew) throws IOException {
		// Create a temporary session so we can log in
		String tempCsrf = makeTemporarySession();

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
	 * @param action
	 *            The action to be repeated if it fails due to session expiry.
	 * @return The result of the action
	 * @throws IOException
	 *             If things go wrong.
	 */
	<T> T withRenewal(Action<T> action) throws IOException {
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
