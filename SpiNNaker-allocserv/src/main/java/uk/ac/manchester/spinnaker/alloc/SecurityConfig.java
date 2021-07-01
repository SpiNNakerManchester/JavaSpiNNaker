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
package uk.ac.manchester.spinnaker.alloc;

import static java.util.stream.Collectors.toSet;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * The security configuration of the service.
 * <p>
 * <strong>Note:</strong> role expressions ({@link #IS_USER} and
 * {@link #IS_ADMIN}) must be applied (with {@code @}{@link PreAuthorize}) to
 * <em>interfaces</em> of classes (or methods of those interfaces) that are
 * Spring Beans in order for the security interception to be applied correctly.
 * This is the <em>only</em> combination that is known to work reliably.
 *
 * @author Donal Fellows
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	private static final Logger log = getLogger(SecurityConfig.class);

	static {
		// Workaround for checkstyle being stupid
		PreAuthorize.class.getClass();
	}

	// TODO application security model
	// https://github.com/SpiNNakerManchester/JavaSpiNNaker/issues/342

	/** How to assert that a user must be an admin. */
	public static final String IS_ADMIN = "hasRole('ADMIN')";

	/** How to assert that a user must be able to read summaries. */
	public static final String IS_READER = "hasRole('READER')";

	/**
	 * How to assert that a user must be able to make jobs and read job details
	 * in depth.
	 */
	public static final String IS_USER = "hasRole('USER')";

	/**
	 * The authority used to grant a user permission to create jobs, manipulate
	 * them, and read their details. Note that many features are locked to the
	 * owner of the job or admins. Users should also have {@link #GRANT_READER}.
	 */
	protected static final String GRANT_USER = "ROLE_USER";

	/**
	 * The authority used to grant a user permission to get general machine
	 * information and summaries of jobs. Without this, only the service root
	 * (and the parts required for logging in) will be visible.
	 */
	protected static final String GRANT_READER = "ROLE_READER";

	/**
	 * The authority used to grant a user permission to use administrative
	 * actions. Admins should also have {@link #GRANT_USER} and
	 * {@link #GRANT_READER}.
	 */
	protected static final String GRANT_ADMIN = "ROLE_ADMIN";

	/**
	 * The HTTP basic authentication realm.
	 */
	private static final String REALM = "SpallocService";

	@Autowired
	private BasicAuthEntryPoint authenticationEntryPoint;

	@Autowired
	private LocalAuthenticationProvider localAuthProvider;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth)
			throws Exception {
		// FIXME Need to add OpenID support
		auth.authenticationProvider(localAuthProvider);
	}

	/**
	 * Just how trusted is a user?
	 */
	public enum TrustLevel {
		/** Grants no real permissions at all. */
		BASIC,
		/**
		 * Grants read-only permissions in addition to {@link #BASIC}.
		 *
		 * @see SecurityConfig#GRANT_READER
		 */
		READER,
		/**
		 * Grants job creation and management permissions in addition to
		 * {@link #READER}.
		 *
		 * @see SecurityConfig#GRANT_USER
		 */
		USER,
		/**
		 * Grants service administration permissions in addition to
		 * {@link #USER}.
		 *
		 * @see SecurityConfig#GRANT_ADMIN
		 */
		ADMIN
	}

	/**
	 * Locally-defined authentication providers include the capability to create
	 * users.
	 *
	 * @author Donal Fellows
	 */
	public interface LocalAuthenticationProvider
			extends AuthenticationProvider {
		/**
		 * Create a user. Only admins can create users.
		 *
		 * @param username
		 *            The user name to use.
		 * @param password
		 *            The <em>unencoded</em> password to use.
		 * @param trustLevel
		 *            How much is the user trusted.
		 * @param quota
		 *            The user's quota, in board-seconds.
		 * @return True if the user was created, false if the user already
		 *         existed.
		 * @throws SQLException
		 *             If anything goes wrong (including trying to create an
		 *             existing user).
		 */
		@PreAuthorize(IS_ADMIN)
		boolean createUser(String username, String password,
				TrustLevel trustLevel, long quota) throws SQLException;

		// TODO what other operations should there be?
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		/*
		 * The /info part reveals admin details; you need ROLE_ADMIN to view it.
		 * For anything else, as long as you are authenticated we're happy. SOME
		 * calls have additional requirements; those are annotated
		 * with @PreAuthorize and a suitable auth expression.
		 */
		http.authorizeRequests()//
				.antMatchers("/info*").hasRole("ADMIN") //
				.antMatchers("/info/**").hasRole("ADMIN") //
				.anyRequest().authenticated().and().httpBasic()
				.authenticationEntryPoint(authenticationEntryPoint);
		// FIXME add support for other styles of login, etc
	}

	/**
	 * Implements basic auth.
	 */
	@Component
	static class BasicAuthEntryPoint extends BasicAuthenticationEntryPoint {
		@Override
		public void commence(HttpServletRequest request,
				HttpServletResponse response, AuthenticationException authEx)
				throws IOException {
			log.info("issuing request for log in to {}",
					request.getRemoteAddr());
			response.addHeader("WWW-Authenticate",
					"Basic realm=" + getRealmName());
			response.setStatus(SC_UNAUTHORIZED);
			PrintWriter writer = response.getWriter();
			writer.println("log in required");
		}

		@Override
		public void afterPropertiesSet() {
			setRealmName(REALM);
			super.afterPropertiesSet();
		}
	}

	/**
	 * Make access denied (from a {@code @}{@link PreAuthorize} check) not fill
	 * the log with huge stack traces.
	 */
	@Component
	@Provider
	static class SpringAccessDeniedExceptionExceptionMapper
			implements ExceptionMapper<AccessDeniedException> {
		@Context
		private UriInfo ui;

		@Context
		private HttpServletRequest req;

		@Override
		public Response toResponse(AccessDeniedException exception) {
			// Actually produce useful logging; the default is ghastly!
			UsernamePasswordAuthenticationToken who =
					(UsernamePasswordAuthenticationToken) req
							.getUserPrincipal();
			log.warn("access denied: {} : {} {}", ui.getAbsolutePath(),
					who.getName(),
					who.getAuthorities().stream()
							.map(GrantedAuthority::getAuthority)
							.collect(toSet()));
			// But the user gets a bland response
			return status(FORBIDDEN).entity("computer says no").build();
		}
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
