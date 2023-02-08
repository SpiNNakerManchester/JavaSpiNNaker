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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.security.LocalAuthProviderImpl.isUnsupportedAuthTokenClass;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A filter to apply authentication transformation as supplied by the
 * {@link LocalAuthenticationProvider}. Relies on the session ID being updated
 * to ensure that stale login information is not retained.
 *
 * @see LocalAuthenticationProvider#updateAuthentication(SecurityContext)
 */
@Component
public class AppAuthTransformationFilter extends OncePerRequestFilter {
	private static final Logger log =
			getLogger(AppAuthTransformationFilter.class);

	/**
	 * The name of session fixation tokens supported by this class.
	 */
	private static final String TOKEN =
			AppAuthTransformationFilter.class.getName();

	@Autowired
	private LocalAuthenticationProvider<?> localAuthProvider;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request)
			throws ServletException {
		var a = SecurityContextHolder.getContext().getAuthentication();
		return isNull(a) || isUnsupportedAuthTokenClass(a.getClass());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		var s = request.getSession(false);
		var ctx = SecurityContextHolder.getContext();
		var savedAuth = getSavedToken(s);
		var originalAuth = ctx.getAuthentication();
		if (nonNull(savedAuth)) {
			ctx.setAuthentication(savedAuth);
		} else {
			var a = localAuthProvider.updateAuthentication(request, ctx);
			if (nonNull(a)) {
				saveToken(s, a);
			}
		}
		try {
			chain.doFilter(request, response);
		} finally {
			ctx.setAuthentication(originalAuth);
		}
	}

	private static Authentication getSavedToken(HttpSession session) {
		if (nonNull(session)) {
			var o = session.getAttribute(TOKEN);
			if (o instanceof Token) {
				var t = (Token) o;
				if (t.isValid(session)) {
					return t.getAuth();
				}
			}
			if (nonNull(o)) {
				session.removeAttribute(TOKEN);
				log.debug("removed stale special auth token from session");
			}
		}
		return null;
	}

	private static void saveToken(HttpSession session, Authentication auth) {
		if (nonNull(session)) {
			session.setAttribute(TOKEN, new Token(session, auth));
			log.debug("added special auth token to session");
		}
	}

	/**
	 * Ensure that any authentication token that was bound into the session is
	 * removed.
	 *
	 * @param request
	 *            the HTTP request; used for looking up the session
	 */
	public static void clearToken(HttpServletRequest request) {
		var session = request.getSession(false);
		if (nonNull(session)) {
			session.removeAttribute(TOKEN);
			log.debug("removed special auth token from session");
		}
	}
}
