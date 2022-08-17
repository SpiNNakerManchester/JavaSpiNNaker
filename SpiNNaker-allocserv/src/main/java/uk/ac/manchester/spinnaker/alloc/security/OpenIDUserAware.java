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

import static org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.SUB;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.EMAIL;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.EMAIL_VERIFIED;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.NAME;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

import java.util.Objects;
import java.util.Optional;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;

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
	 * Get the underlying OpenID user token.
	 *
	 * @return The user's bearer token. Pay attention to the claims.
	 */
	Optional<Jwt> getOpenIdToken();

	/**
	 * Get a claim/attribute that is a string.
	 *
	 * @param claimName
	 *            The name of the claim.
	 * @return The string.
	 * @throws IllegalStateException
	 *             If neither {@link #getOpenIdUser()} nor
	 *             {@link #getOpenIdToken()} provide anything we can get claims
	 *             from.
	 */
	default String getStringClaim(String claimName) {
		return getOpenIdUser()
				.map(u -> Objects.toString(u.getAttribute(claimName)))
				.orElseGet(() -> getOpenIdToken()
						.map(t -> t.getClaimAsString(claimName))
						.orElseThrow(() -> new IllegalStateException(
								"no user or token to supply claim")));
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
		Optional<String> email = getOpenIdUser().map(uu -> uu.getAttributes())
				.filter(uu -> uu.containsKey(EMAIL_VERIFIED)
						&& (Boolean) uu.get(EMAIL_VERIFIED))
				.map(uu -> uu.get(EMAIL).toString());
		if (email.isPresent()) {
			return email;
		}
		return getOpenIdToken().filter(t -> t.getClaim(EMAIL_VERIFIED))
				.map(t -> t.getClaimAsString(EMAIL));
	}
}
