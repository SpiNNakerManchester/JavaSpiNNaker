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
package uk.ac.manchester.spinnaker.alloc.security;

import static java.util.stream.Collectors.toUnmodifiableList;
import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_READER;
import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_USER;
import static uk.ac.manchester.spinnaker.alloc.security.TrustLevel.USER;

import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketSession;

/**
 * Encodes what a user is permitted to do. Abstracts over several types of
 * security context.
 */
public final class Permit {
	/** Is the user an admin? */
	public final boolean admin;

	/** What is the name of the user? */
	public final String name;

	private final List<GrantedAuthority> authorities;

	private static final List<String> STDAUTH =
			List.of(GRANT_ADMIN, GRANT_READER, GRANT_USER);

	/**
	 * Build a permit.
	 *
	 * @param context
	 *            The originating security context.
	 */
	public Permit(jakarta.ws.rs.core.SecurityContext context) {
		authorities = STDAUTH.stream().filter(context::isUserInRole)
				.map(SimpleGrantedAuthority::new).collect(toUnmodifiableList());
		admin = isAdmin(authorities);
		name = context.getUserPrincipal().getName();
	}

	private static boolean isAdmin(List<GrantedAuthority> auths) {
		return auths.stream().map(GrantedAuthority::getAuthority)
				.anyMatch(GRANT_ADMIN::equals);
	}

	/**
	 * Build a permit.
	 *
	 * @param context
	 *            The originating security context.
	 */
	public Permit(SecurityContext context) {
		authorities = context.getAuthentication().getAuthorities().stream()
				.collect(toUnmodifiableList());
		admin = isAdmin(authorities);
		name = context.getAuthentication().getName();
	}

	/**
	 * Build a permit for a service user. The service user can create jobs and
	 * read job details, but cannot do much with jobs owned by other users.
	 * <em>Only used by the legacy interface.</em>
	 *
	 * @param serviceUser
	 *            The user name. Must exist in order to be actually used.
	 */
	public Permit(String serviceUser) {
		authorities = USER.getGrants().collect(toUnmodifiableList());
		admin = false;
		name = serviceUser;
	}

	/**
	 * The permit used for web socket handling. Note that websockets never have
	 * access to admin facilities (and shouldn't ever need them), even if their
	 * creating user does.
	 *
	 * @param session
	 *            The originating websocket context.
	 */
	public Permit(WebSocketSession session) {
		authorities = USER.getGrants().collect(toUnmodifiableList());
		admin = false;
		name = session.getPrincipal().getName();
	}

	/**
	 * Can something owned by a given user can be shown to the user that this
	 * permit is for?
	 *
	 * @param owner
	 *            The owner of the object.
	 * @return True exactly if the object (or subset of properties) may be
	 *         shown.
	 */
	public boolean unveilFor(String owner) {
		return admin || owner.equals(name);
	}

	/**
	 * Push our special temporary authentication object for the duration of the
	 * inner code. Used to satisfy Spring method security.
	 *
	 * @param <T>
	 *            The type of the result
	 * @param inContext
	 *            The inner code to run with an authentication object applied.
	 * @return Whatever the inner code returns
	 */
	public <T> T authorize(Supplier<T> inContext) {
		/**
		 * A temporarily-installable authentication token. Allows access to
		 * secured APIs in asynchronous worker threads, provided they provide a
		 * {@link Permit} (obtained from a service thread) to show that they may
		 * do so.
		 */
		@SuppressWarnings("serial")
		final class TempAuth implements Authentication {
			// The permit already proves we're authenticated
			private boolean auth = true;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				return authorities;
			}

			@Override
			public Object getCredentials() {
				// You can never get the credentials from this
				return null;
			}

			@Override
			public Permit getDetails() {
				return Permit.this;
			}

			@Override
			public String getPrincipal() {
				return name;
			}

			@Override
			public boolean isAuthenticated() {
				return auth;
			}

			@Override
			public void setAuthenticated(boolean isAuthenticated) {
				if (!isAuthenticated) {
					auth = false;
				}
			}

			private void writeObject(ObjectOutputStream out)
					throws NotSerializableException {
				throw new NotSerializableException("not actually serializable");
			}
		}

		var c = SecurityContextHolder.getContext();
		var old = c.getAuthentication();
		c.setAuthentication(new TempAuth());
		try {
			return inContext.get();
		} finally {
			c.setAuthentication(old);
		}
	}
}
