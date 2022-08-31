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
package uk.ac.manchester.spinnaker.tools;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getEncoder;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.tools.EBRAINSDevCredentialsUtils.encodeForm;
import static uk.ac.manchester.spinnaker.tools.EBRAINSDevCredentialsUtils.makeBasicAuth;
import static uk.ac.manchester.spinnaker.tools.GetDevId.HBP_OPENID_BASE;
import static uk.ac.manchester.spinnaker.tools.GetDevId.REALM;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.StringJoiner;

import org.keycloak.representations.AccessTokenResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Developer credentials for initial access to client registration using the
 * HBP/EBRAINS Keycloak service.
 *
 * @author Donal Fellows
 */
interface EBRAINSDevCredentials {
	/** The content type for submitting an HTML form (or equivalent). */
	String FORM_ENCODED = "application/x-www-form-urlencoded; charset=UTF-8";

	/** Where to get the developer access token from. This is non-standard. */
	String OIDC_TOKEN_URL = HBP_OPENID_BASE + "realms/" + REALM
			+ "/protocol/openid-connect/token";

	/**
	 * @return The user that will do the registration. Must not be {@code null}.
	 */
	String getUser();

	/**
	 * @return The password for the user that will do the registration. Must not
	 *         be {@code null}.
	 */
	String getPass();

	/**
	 * Convert the username and password into an access token.
	 *
	 * @return The access token.
	 * @throws IOException
	 *             If anything goes wrong. In particular, the user must be known
	 *             to the Keycloak service and be authorized to publish service
	 *             descriptions.
	 */
	default String getToken() throws IOException {
		var user = requireNonNull(getUser(), "user must be given");
		var pass = requireNonNull(getPass(), "password must be given");
		var mapper = new ObjectMapper();

		var url = new URL(OIDC_TOKEN_URL);
		var http = (HttpURLConnection) url.openConnection();
		http.setRequestMethod("POST");
		http.setDoOutput(true);

		// UGLY!
		http.setRequestProperty("Authorization", makeBasicAuth("developer:"));

		var out = encodeForm(Map.ofEntries(entry("grant_type", "password"),
				entry("username", user), entry("password", pass)));
		http.setFixedLengthStreamingMode(out.length);
		http.setRequestProperty("Content-Type", FORM_ENCODED);
		try (var os = http.getOutputStream()) {
			os.write(out);
		}
		try (var is = http.getInputStream()) {
			return mapper.readValue(is, AccessTokenResponse.class).getToken();
		}
	}
}

/** Utilities for {@link EBRAINSDevCredentials}. */
abstract class EBRAINSDevCredentialsUtils {
	private EBRAINSDevCredentialsUtils() {
	}

	/**
	 * Convert a map to HTML form. Technically the target format supports a
	 * multimap, but we don't need that!
	 *
	 * @param arguments
	 *            The map of arguments.
	 * @return The encoded form.
	 */
	static byte[] encodeForm(Map<String, String> arguments) {
		var sj = new StringJoiner("&");
		for (var entry : arguments.entrySet()) {
			sj.add(encode(entry.getKey(), UTF_8) + "="
					+ encode(entry.getValue(), UTF_8));
		}
		return sj.toString().getBytes(UTF_8);
	}

	/**
	 * Convert a username/password pair to the contents of an
	 * {@code Authorization} header.
	 *
	 * @param userPass
	 *            The username and password, separated by a colon.
	 * @return the header contents
	 */
	static String makeBasicAuth(String userPass) {
		return "Basic " + new String(
				getEncoder().encode(userPass.getBytes(UTF_8)), UTF_8);
	}
}
