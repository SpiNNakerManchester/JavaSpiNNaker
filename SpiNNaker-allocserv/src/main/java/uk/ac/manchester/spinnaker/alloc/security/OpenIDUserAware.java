/*
 * Copyright (c) 2021-2022 The University of Manchester
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

import static org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.SUB;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.EMAIL;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.EMAIL_VERIFIED;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.NAME;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

import java.util.Objects;
import java.util.Optional;

import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * An object that can say something about what user it was derived from. Note
 * that you never have both the user information and the token at the same time,
 * but they contain fairly similar contents.
 */
public interface OpenIDUserAware {
	/**
	 * Get the underlying OpenID user information.
	 *
	 * @return The user info, if known. Pay attention to the attributes.
	 */
	Optional<OAuth2User> getOpenIdUser();

	/**
	 * Get a claim/attribute that is a string.
	 *
	 * @param claimName
	 *            The name of the claim.
	 * @return The string.
	 * @throws IllegalStateException
	 *             If {@link #getOpenIdUser()} doesn't provide anything we can
	 *             get claims from.
	 */
	default String getStringClaim(String claimName) {
		return getOpenIdUser()
				.map(u -> Objects.toString(u.getAttribute(claimName)))
						.orElseThrow(() -> new IllegalStateException(
								"no user or token to supply claim"));
	}

	/**
	 * @return The OpenID subject identifier.
	 * @throws IllegalStateException
	 *             If the object was made without either a user or a token.
	 */
	default String getOpenIdSubject() {
		return getStringClaim(SUB);
	}

	/**
	 * @return The preferred OpenID user name. <em>We don't use this
	 *         directly</em> as it may clash with other user names.
	 * @throws IllegalStateException
	 *             If the object was made without either a user or a token.
	 */
	default String getOpenIdUserName() {
		return getStringClaim(PREFERRED_USERNAME);
	}

	/**
	 * @return The real name of the OpenID user.
	 * @throws IllegalStateException
	 *             If the object was made without either a user or a token.
	 */
	default String getOpenIdName() {
		return getStringClaim(NAME);
	}

	/**
	 * @return The verified email address of the OpenID user, if available.
	 */
	default Optional<String> getOpenIdEmail() {
		var email = getOpenIdUser().map(uu -> uu.getAttributes())
				.filter(uu -> uu.containsKey(EMAIL_VERIFIED)
						&& (Boolean) uu.get(EMAIL_VERIFIED))
				.map(uu -> uu.get(EMAIL).toString());
		if (email.isPresent()) {
			return email;
		}
		return null;
	}
}
