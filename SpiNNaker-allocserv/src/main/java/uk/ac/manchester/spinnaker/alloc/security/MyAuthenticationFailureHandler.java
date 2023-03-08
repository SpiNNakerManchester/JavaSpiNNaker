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

import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_SUPPORT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.io.IOException;
import java.time.Instant;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Role;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.json.JsonMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

		var message = BLAND_AUTH_MSG;
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
