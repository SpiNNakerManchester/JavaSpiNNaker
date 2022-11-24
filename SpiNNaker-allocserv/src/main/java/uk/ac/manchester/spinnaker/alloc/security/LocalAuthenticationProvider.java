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
package uk.ac.manchester.spinnaker.alloc.security;

import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_ADMIN;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.google.errorprone.annotations.RestrictedApi;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;

/**
 * Locally-defined authentication providers include the capability to create
 * users.
 *
 * @param <TestAPI>
 *            The type of the test interface. Not important for non-test
 *            purposes.
 * @author Donal Fellows
 */
public interface LocalAuthenticationProvider<TestAPI>
		extends AuthenticationProvider {
	/**
	 * Create a user. Only admins can create users.
	 *
	 * @param username
	 *            The user name to use.
	 * @param password
	 *            The <em>unencoded</em> password to use.
	 * @param trustLevel
	 *            How much is the user trusted.
	 * @return True if the user was created, false if the user already existed.
	 */
	@PreAuthorize(IS_ADMIN)
	boolean createUser(String username, String password, TrustLevel trustLevel);

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
			Collection<GrantedAuthority> resultCollection);

	/**
	 * Map the token to authorities, adding them to the result.
	 *
	 * @param token
	 *            The token to map.
	 * @param resultCollection
	 *            Where to add the authorities.
	 */
	void mapAuthorities(Jwt token,
			Collection<GrantedAuthority> resultCollection);

	/**
	 * Map the attributes taken from introspecting an opaque token to
	 * authorities.
	 *
	 * @param attributes
	 *            The attributes from the introspection. Read-only.
	 * @return The authorities to add.
	 */
	Collection<GrantedAuthority> mapOpaqueTokenAttributes(
			Map<String, Object> attributes);

	/**
	 * @return The test interface.
	 * @deprecated This interface is just for testing.
	 */
	@ForTestingOnly
	@RestrictedApi(explanation = "just for testing", link = "index.html",
			allowedOnPath = ".*/src/test/java/.*")
	@Deprecated
	TestAPI getTestAPI();
}
