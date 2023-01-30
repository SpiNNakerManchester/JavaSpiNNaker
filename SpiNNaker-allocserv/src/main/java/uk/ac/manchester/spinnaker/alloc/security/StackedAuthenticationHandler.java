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
import static org.springframework.security.core.context.SecurityContextHolder.setContext;

import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.TransientSecurityContext;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;

import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

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

	/** The context we will restore. */
	private final SecurityContext oldContext;

	private final SecurityContext newContext;

	/**
	 * Compare two values using their actual object identities.
	 *
	 * @param <T>
	 *            The type of values being compared.
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

	/**
	 * Set the current user for this thread.
	 *
	 * @param targetUser
	 *            What user (by token) we will run as.
	 * @return The transient context.
	 * @see SwitchUserFilter
	 * @see TransientSecurityContext
	 */
	@UsedInJavadocOnly(SwitchUserFilter.class)
	private static SecurityContext setUserContext(Authentication targetUser) {
		var context = new TransientSecurityContext();
		context.setAuthentication(targetUser);
		setContext(context);
		return context;
	}

	StackedAuthenticationHandler(Authentication auth) {
		ourAuth = auth;
		oldContext = getContext();
		newContext = setUserContext(auth);
		if (identical(ourAuth, oldContext.getAuthentication())) {
			// Changing to ourself? WTF!
			log.warn(
					"unexpectedly identical authentications when pushing "
							+ "thread security context: had {} pushing {}",
					oldContext.getAuthentication(), ourAuth);
		}
	}

	private static final String UNEXPECTED = "unexpected {} when popping "
			+ "thread security context: expected {} got {}";

	@Override
	public void close() {
		var c = getContext();
		if (!identical(newContext, c)) {
			log.warn(UNEXPECTED, "security context", newContext, c);
		}
		var got = c.getAuthentication();
		if (identical(got, ourAuth)) {
			setContext(oldContext);
		} else {
			log.warn(UNEXPECTED, "authentication token", ourAuth, got);
		}
	}
}
