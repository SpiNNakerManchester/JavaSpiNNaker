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

import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_ADMIN;

import java.util.Collection;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import com.google.errorprone.annotations.RestrictedApi;

import jakarta.servlet.http.HttpServletRequest;
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
	 * @param req
	 *            The request being made.
	 * @param ctx
	 *            The security context.
	 * @return The new authentication (which is also installed into the security
	 *         context), or {@code null} <em>if the authentication is not
	 *         changed</em>.
	 */
	Authentication updateAuthentication(HttpServletRequest req,
			SecurityContext ctx);

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
	 * @return The test interface.
	 * @deprecated This interface is just for testing.
	 * @hidden
	 */
	@ForTestingOnly
	@RestrictedApi(explanation = "just for testing", link = "index.html",
			allowedOnPath = ".*/src/test/java/.*")
	@Deprecated
	TestAPI getTestAPI();
}
