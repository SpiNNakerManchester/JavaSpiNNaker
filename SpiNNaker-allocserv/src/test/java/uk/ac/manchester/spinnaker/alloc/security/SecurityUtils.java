/*
 * Copyright (c) 2022 The University of Manchester
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

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Utilities for security-related testing. */
public abstract class SecurityUtils {
	private SecurityUtils() {
	}

	/** Capability provided by {@link #inContext(InC)} to what it guards. */
	public interface C {
		/**
		 * Install an authentication token.
		 *
		 * @param auth
		 *            The authentication token to install. {@code null} to
		 *            remove.
		 */
		void setAuth(Authentication auth);

		/**
		 * Install the named user as the current user and provide a permit that
		 * allows that user to touch resources.
		 *
		 * @param name
		 *            The user name.
		 * @return The newly-minted permit.
		 */
		default Permit setAuth(String name) {
			@SuppressWarnings("serial")
			Authentication a = new Authentication() {
				@Override
				public String getName() {
					return name;
				}

				@Override
				public Collection<? extends GrantedAuthority> getAuthorities() {
					return new ArrayList<>();
				}

				@Override
				public Object getCredentials() {
					return null;
				}

				@Override
				public Object getDetails() {
					return null;
				}

				@Override
				public Object getPrincipal() {
					return null;
				}

				@Override
				public boolean isAuthenticated() {
					return true;
				}

				@Override
				public void setAuthenticated(boolean isAuthenticated) {
				}
			};
			setAuth(a);
			return new Permit(SecurityContextHolder.getContext());
		}
	}

	/**
	 * An action wrapped by {@link #inContext(InC)}.
	 */
	public interface InC {
		/**
		 * The wrapped action.
		 *
		 * @param c
		 *            The capability to set the current user.
		 */
		void act(C c);
	}

	/**
	 * Run code with the capability to set the current user. Will clean up
	 * afterwards.
	 *
	 * @param inc
	 *            The code to run.
	 */
	public static void inContext(InC inc) {
		SecurityContext context = SecurityContextHolder.getContext();
		try {
			inc.act(context::setAuthentication);
		} finally {
			context.setAuthentication(null);
		}
	}
}
