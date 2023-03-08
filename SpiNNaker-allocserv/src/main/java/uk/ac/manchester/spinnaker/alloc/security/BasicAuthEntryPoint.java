/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.security;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

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
