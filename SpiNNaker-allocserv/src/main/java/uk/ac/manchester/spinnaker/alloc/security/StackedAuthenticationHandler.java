/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.security;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

import org.slf4j.Logger;
import org.springframework.security.core.Authentication;

/**
 * Handles the applying of an {@linkplain Authentication authentication} to the
 * current thread as a context. Doesn't make any particular statement about what
 * the authentication does.
 *
 * @author Donal Fellows
 */
class StackedAuthenticationHandler implements AutoCloseable {
	private static final Logger log =
			getLogger(StackedAuthenticationHandler.class);

	/** The authentication we have applied. */
	private final Authentication ourAuth;

	/** The authentication we will restore. */
	private final Authentication oldAuth;

	/**
	 * Compare two values using their actual object identities.
	 *
	 * @param <T>
	 *            The type of values to compare.
	 * @param a
	 *            The first value reference. May be {@code null}.
	 * @param b
	 *            The second value reference. May be {@code null}.
	 * @return Whether the references actually are to the same object (or are
	 *         both {@code null}).
	 */
	private static <T> boolean identical(T a, T b) {
		return a == b;
	}

	StackedAuthenticationHandler(Authentication auth) {
		ourAuth = auth;
		var c = getContext();
		oldAuth = c.getAuthentication();
		c.setAuthentication(auth);
		if (identical(ourAuth, oldAuth)) {
			log.warn(
					"unexpectedly identical authentications when pushing "
							+ "thread security context: had {} pushing {}",
					oldAuth, ourAuth);
		}
	}

	@Override
	public void close() {
		var c = getContext();
		var got = c.getAuthentication();
		if (identical(got, ourAuth)) {
			c.setAuthentication(oldAuth);
		} else {
			log.warn(
					"unexpected authentication token when popping "
							+ "thread security context: expected {} got {}",
					ourAuth, got);
		}
	}
}
