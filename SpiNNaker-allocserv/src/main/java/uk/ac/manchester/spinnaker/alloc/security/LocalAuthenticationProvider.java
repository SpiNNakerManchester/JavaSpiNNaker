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

import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_ADMIN;

import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

/**
 * Locally-defined authentication providers include the capability to create
 * users.
 *
 * @author Donal Fellows
 */
public interface LocalAuthenticationProvider extends AuthenticationProvider {
	/**
	 * Create a user. Only admins can create users.
	 *
	 * @param username
	 *            The user name to use.
	 * @param password
	 *            The <em>unencoded</em> password to use.
	 * @param trustLevel
	 *            How much is the user trusted.
	 * @param quota
	 *            The user's quota, in board-seconds.
	 * @return True if the user was created, false if the user already existed.
	 */
	@PreAuthorize(IS_ADMIN)
	boolean createUser(String username, String password, TrustLevel trustLevel,
			long quota);

	/**
	 * Unlock any locked users whose lock period has expired.
	 */
	void unlockLockedUsers();

	/**
	 * Convert the type of the authentication in the security context.
	 *
	 * @param ctx
	 *            The security context.
	 * @return The new authentication (which is also installed into the security
	 *         context), or {@code null} <em>if the authentication is not
	 *         changed</em>.
	 */
	Authentication updateAuthentication(SecurityContext ctx);

	/**
	 * Map the authorities, adding them to the result.
	 *
	 * @param authority
	 *            The overall authority to map.
	 * @param resultCollection
	 *            Where to add the authorities.
	 */
	void mapAuthorities(OidcUserAuthority authority,
			Set<GrantedAuthority> resultCollection);
}
