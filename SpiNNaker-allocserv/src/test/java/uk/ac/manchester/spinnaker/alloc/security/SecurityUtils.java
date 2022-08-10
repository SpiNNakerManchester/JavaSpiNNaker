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

public abstract class SecurityUtils {
	private SecurityUtils() {
	}

	public interface C {
		void setAuth(Authentication a);

		@SuppressWarnings("serial")
		default void setAuth(String name) {
			setAuth(new Authentication() {
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
			});
		}
	}

	public interface InC {
		void act(C c);
	}

	public static void inContext(InC inc) {
		SecurityContext context = SecurityContextHolder.getContext();
		try {
			inc.act(context::setAuthentication);
		} finally {
			context.setAuthentication(null);
		}
	}

}
