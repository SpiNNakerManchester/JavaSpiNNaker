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
package uk.ac.manchester.spinnaker.alloc.security;

import static java.util.Collections.unmodifiableCollection;
import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_READER;
import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_USER;
import static uk.ac.manchester.spinnaker.alloc.security.TrustLevel.USER;

import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
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

	private List<GrantedAuthority> authorities = new ArrayList<>();

	private static final String[] STDAUTH = {
		GRANT_ADMIN, GRANT_READER, GRANT_USER
	};

	/**
	 * Build a permit.
	 *
	 * @param context
	 *            The originating security context.
	 */
	public Permit(javax.ws.rs.core.SecurityContext context) {
		for (var role : STDAUTH) {
			if (context.isUserInRole(role)) {
				authorities.add(new SimpleGrantedAuthority(role));
			}
		}
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
		authorities =
				new ArrayList<>(context.getAuthentication().getAuthorities());
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
		USER.getGrants().forEach(authorities::add);
		admin = false;
		name = serviceUser;
	}

	/**
	 * The permit used for web socket handling.
	 *
	 * @param session
	 */
	public Permit(WebSocketSession session) {
		USER.getGrants().forEach(authorities::add);
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
		class TempAuth implements Authentication {
			// The permit already proves we're authenticated
			private boolean auth = true;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				return unmodifiableCollection(authorities);
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
