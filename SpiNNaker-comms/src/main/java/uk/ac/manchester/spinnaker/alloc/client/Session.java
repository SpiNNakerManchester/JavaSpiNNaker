/*
 * Copyright (c) 2021-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.concurrent.Callable;

/**
 * Operations on the low level session that are intended to be used by the
 * client.
 */
interface Session {
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
			throws IOException;

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
			throws IOException;

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
			throws IOException;

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
	default HttpURLConnection connection(URI url, URI url2) throws IOException {
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
	default HttpURLConnection connection(URI url) throws IOException {
		return connection(url, false);
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
	boolean trackCookie(HttpURLConnection conn);

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
	@FunctionalInterface
	interface Action<T, Exn extends Exception> extends Callable<T> {
		/**
		 * Perform the action.
		 *
		 * @return The result of the action.
		 * @throws IOException
		 *             If network I/O fails.
		 * @throws Exn
		 *             If another failure happens.
		 */
		@Override
		T call() throws Exn, IOException;
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
			throws Exn, IOException;

	/**
	 * Discovers the root of a Spalloc service. Also sets up the true CSRF token
	 * handling.
	 *
	 * @return The service root information.
	 * @throws IOException
	 *             If access fails.
	 */
	RootInfo discoverRoot() throws IOException;

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
			throws InterruptedException, IOException;
}
