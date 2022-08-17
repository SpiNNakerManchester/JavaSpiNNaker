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

import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_SUPPORT;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.springframework.context.annotation.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Make access denied (from a {@code @}{@link PreAuthorize} check) not fill the
 * log with huge stack traces.
 */
@Component
@Provider
@Role(ROLE_SUPPORT)
@UsedInJavadocOnly(PreAuthorize.class)
class AccessDeniedExceptionMapper
		implements ExceptionMapper<AccessDeniedException> {
	private static final Logger log =
			getLogger(AccessDeniedExceptionMapper.class);

	private static final String BLAND_AUTH_MSG = "computer says no";

	@Context
	private UriInfo ui;

	@Context
	private HttpServletRequest req;

	@Override
	public Response toResponse(AccessDeniedException exception) {
		// Actually produce useful logging; the default is ghastly!
		Principal p = req.getUserPrincipal();
		if (p instanceof AbstractAuthenticationToken) {
			AbstractAuthenticationToken who = (AbstractAuthenticationToken) p;
			log.warn("access denied: {} : {} {}", ui.getAbsolutePath(),
					who.getName(),
					who.getAuthorities().stream()
							.map(GrantedAuthority::getAuthority)
							.collect(toSet()));
		} else if (p instanceof OAuth2AuthenticatedPrincipal) {
			OAuth2AuthenticatedPrincipal who = (OAuth2AuthenticatedPrincipal) p;
			log.warn("access denied: {} : {} {}", ui.getAbsolutePath(),
					who.getName(),
					who.getAuthorities().stream()
							.map(GrantedAuthority::getAuthority)
							.collect(toSet()));
		} else {
			log.warn("access denied: {} : {}", ui.getAbsolutePath(), p);
		}
		// But the user gets a bland response
		return status(FORBIDDEN).entity(BLAND_AUTH_MSG).build();
	}
}
