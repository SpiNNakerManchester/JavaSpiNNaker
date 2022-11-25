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

import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AuthProperties;

/**
 * Implements basic and bearer auth challenge presentation.
 */
@Component
public class BasicAuthEntryPoint implements AuthenticationEntryPoint {
	private static final Logger log = getLogger(BasicAuthEntryPoint.class);

	private static final String AUTH = "WWW-Authenticate";

	@Autowired
	private AuthProperties props;

	private String basicChallenge() {
		return format("Basic realm=\"%s\"", props.getRealm());
	}

	private String openidChallenge() {
		var scopes = props.getOpenid().getScopes();
		return format("Bearer realm=\"%s\", scope=\"%s\"", //
				props.getRealm(), scopes.stream().collect(joining(", ")));
	}

	@Override
	public void commence(HttpServletRequest request,
			HttpServletResponse response, AuthenticationException authEx)
			throws IOException {
		log.debug("issuing request for log in to {}", request.getRemoteAddr());
		/*
		 * The two API auth methods should be separate headers; the specs say
		 * they can be one, but that's an area where things get "exciting"
		 * (i.e., ambiguous) so we don't!
		 */
		if (props.isBasic()) {
			response.addHeader(AUTH, basicChallenge());
		}
		if (props.getOpenid().isEnable()) {
			response.addHeader(AUTH, openidChallenge());
		}
		response.setStatus(SC_UNAUTHORIZED);

		// Provide a basic body; NB, don't need to close the writer
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/plain");
		var writer = response.getWriter();
		writer.println("log in required, "
				+ "either with BASIC auth or HBP/EBRAINS bearer token "
				+ "(if they are enabled)");
		writer.flush(); // Commit the response
	}
}
