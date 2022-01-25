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

import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_SUPPORT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.io.IOException;
import java.time.Instant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Role;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AuthProperties;

@Component
@Role(ROLE_SUPPORT)
class MyAuthenticationFailureHandler implements AuthenticationFailureHandler {
	private static final Logger log =
			getLogger(MyAuthenticationFailureHandler.class);

	private static final String BLAND_AUTH_MSG = "computer says no";

	@Autowired
	private AuthProperties properties;

	@Autowired
	private JsonMapper mapper;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request,
			HttpServletResponse response, AuthenticationException e)
			throws IOException, ServletException {
		log.info("auth failure", e);
		response.setStatus(UNAUTHORIZED.value());

		String message = BLAND_AUTH_MSG;
		if (properties.isDebugFailures()) {
			message += ": " + e.getLocalizedMessage();
		}
		mapper.writeValue(response.getOutputStream(),
				new AuthFailureObject(message));
	}

	static class AuthFailureObject {
		private String message;

		private Instant timestamp;

		AuthFailureObject(String message) {
			this.message = message;
			this.timestamp = now();
		}

		public String getMessage() {
			return message;
		}

		public Instant getTimestamp() {
			return timestamp;
		}
	}
}
