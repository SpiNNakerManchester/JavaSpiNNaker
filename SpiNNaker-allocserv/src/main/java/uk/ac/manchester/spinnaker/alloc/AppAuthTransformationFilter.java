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
package uk.ac.manchester.spinnaker.alloc;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.alloc.LocalAuthProviderImpl.isUnsupportedAuthTokenClass;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.LocalAuthenticationProvider;

/**
 * A filter to apply authentication transformation as supplied by the
 * {@link LocalAuthenticationProvider}. Relies on the session ID being updated
 * to ensure that stale login information is not retained.
 *
 * @see LocalAuthenticationProvider#updateAuthentication(SecurityContext)
 */
@Component
public class AppAuthTransformationFilter extends OncePerRequestFilter {
	/**
	 * The name of session fixation tokens supported by this class.
	 */
	private static final String TOKEN =
			AppAuthTransformationFilter.class.getName();

	@Autowired
	private LocalAuthenticationProvider localAuthProvider;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request)
			throws ServletException {
		Authentication a =
				SecurityContextHolder.getContext().getAuthentication();
		return isNull(a) || isUnsupportedAuthTokenClass(a.getClass());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpSession s = request.getSession(false);
		SecurityContext ctx = SecurityContextHolder.getContext();
		Authentication savedAuth = getSavedToken(s);
		Authentication originalAuth = ctx.getAuthentication();
		if (nonNull(savedAuth)) {
			ctx.setAuthentication(savedAuth);
		} else {
			Authentication a = localAuthProvider.updateAuthentication(ctx);
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
			Object o = session.getAttribute(TOKEN);
			if (o instanceof Token) {
				Token t = (Token) o;
				if (t.isValid(session)) {
					return t.getAuth();
				}
			}
		}
		return null;
	}

	private static void saveToken(HttpSession session, Authentication auth) {
		if (nonNull(session)) {
			session.setAttribute(TOKEN, new Token(session, auth));
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
		HttpSession session = request.getSession(false);
		if (nonNull(session)) {
			session.removeAttribute(TOKEN);
		}
	}
}
