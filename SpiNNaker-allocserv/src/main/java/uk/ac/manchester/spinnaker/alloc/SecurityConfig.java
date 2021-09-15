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

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.ServletException;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.sqlite.Function;

import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.ArgumentCount;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Deterministic;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AuthProperties;

/**
 * The security and administration configuration of the service.
 * <p>
 * <strong>Note:</strong> role expressions ({@link #IS_USER} and
 * {@link #IS_ADMIN}) must be applied (with {@code @}{@link PreAuthorize}) to
 * <em>interfaces</em> of classes (or methods of those interfaces) that are
 * Spring Beans in order for the security interception to be applied correctly.
 * This is the <em>only</em> combination that is known to work reliably.
 *
 * @author Donal Fellows
 */
@EnableWebMvc
@EnableWebSecurity
@Import(SecurityConfig.MvcConfig.class)
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	private static final Logger log = getLogger(SecurityConfig.class);

	// TODO application security model (in progress)
	// https://github.com/SpiNNakerManchester/JavaSpiNNaker/issues/342

	/** How to assert that a user must be an admin. */
	public static final String IS_ADMIN = "hasRole('ADMIN')";

	/** How to assert that a user must be able to read summaries. */
	public static final String IS_READER = "hasRole('READER')";

	/** How to filter out job details that a given user may see (or not). */
	public static final String MAY_SEE_JOB_DETAILS = "#permit.admin or "
			+ " #permit.name == filterObject.owner.orElse(null)";

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

	/** The HTTP basic authentication realm. */
	private static final String REALM = "SpallocService";

	/** The name of the Spring MVC error view. */
	public static final String MVC_ERROR = "erroroccurred";

	@Autowired
	private BasicAuthEntryPoint authenticationEntryPoint;

	@Autowired
	private LocalAuthenticationProvider localAuthProvider;

	@Autowired
	private AuthenticationFailureHandler authenticationFailureHandler;

	@Autowired
	private AuthProperties properties;

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
		 */
		@PreAuthorize(IS_ADMIN)
		boolean createUser(String username, String password,
				TrustLevel trustLevel, long quota);

		/**
		 * Unlock any locked users whose lock period has expired.
		 */
		void unlockLockedUsers();

		// TODO what other operations should there be?
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		/*
		 * The /info part reveals admin details; you need ROLE_ADMIN to view it.
		 * Everything to do with logging in MUST NOT require being logged in.
		 * For anything else, as long as you are authenticated we're happy. SOME
		 * calls have additional requirements; those are annotated
		 * with @PreAuthorize and a suitable auth expression.
		 */
		http.authorizeRequests()
				// General metadata pages require ADMIN access
				.antMatchers("/info*", "/info/**").hasRole("ADMIN")
				// Login process and static resources are available to all
				.antMatchers("/system/login*", "/system/perform_*",
						"/system/error", "/system/resources/*")
				.permitAll()
				// Everything else requires post-login
				.anyRequest().authenticated();
		if (properties.isBasic()) {
			http.httpBasic().authenticationEntryPoint(authenticationEntryPoint);
		}
		if (properties.isLocalForm()) {
			http.formLogin().loginPage("/system/login.html")
					.loginProcessingUrl("/system/perform_login")
					.defaultSuccessUrl("/system/", true)
					.failureUrl("/system/login.html?error=true")
					.failureHandler(authenticationFailureHandler);
		}
		/*
		 * Logging out is common code, but pretty pointless for Basic Auth as
		 * browsers will just log straight back in again. Still, it is
		 * meaningful.
		 */
		http.logout().logoutUrl("/system/perform_logout")
				.deleteCookies("JSESSIONID").invalidateHttpSession(true)
				.logoutSuccessUrl("/system/login.html");
		// FIXME add support for HBP/EBRAINS OpenID Connect
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

	private static final String BLAND_AUTH_MSG = "computer says no";

	/**
	 * Contains a single basic role grant.
	 */
	static class SimpleGrantedAuthority implements GrantedAuthority {
		private static final long serialVersionUID = -668405047103939708L;

		private String role;

		SimpleGrantedAuthority(String role) {
			this.role = role;
		}

		@Override
		public String getAuthority() {
			return role;
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
			return status(FORBIDDEN).entity(BLAND_AUTH_MSG).build();
		}
	}

	/**
	 * SQL function ({@code encode_password}) that takes one argument,
	 * presumably an unencoded password, and returns the encoded form of that
	 * password.
	 * <p>
	 * Note that this is one of the very few beans that ever need to hold a
	 * {@link PasswordEncoder}; everyone else should use the SQL functions.
	 *
	 * @author Donal Fellows
	 */
	@ArgumentCount(1)
	@Component("encode_password")
	static class EncodePasswordFunction extends Function {
		@Autowired
		private PasswordEncoder passwordEncoder;

		// Note that bcrypt is *not* deterministic during encoding

		@Override
		protected void xFunc() throws SQLException {
			log.debug("encode_password() called");
			String pass = value_text(0);
			// Important: encodes NULL as NULL
			if (nonNull(pass)) {
				pass = passwordEncoder.encode(pass);
			}
			result(pass);
		}
	}

	/**
	 * SQL function ({@code match_password}) that takes two arguments,
	 * presumably an unencoded password and the encoded form stored in the DB,
	 * and returns a boolean (0 for false, 1 for true) saying whether the
	 * unencoded password matches the encoded one.
	 * <p>
	 * Does not actually decode the encoded password.
	 * <p>
	 * Note that this is one of the very few beans that ever need to hold a
	 * {@link PasswordEncoder}; everyone else should use the SQL functions.
	 *
	 * @author Donal Fellows
	 */
	@ArgumentCount(2)
	@Deterministic
	@Component("match_password")
	static class MatchPasswordFunction extends Function {
		@Autowired
		private PasswordEncoder passwordEncoder;

		@Override
		protected void xFunc() throws SQLException {
			log.debug("match_password() called");
			// If either argument is NULL, the result is false
			String raw = value_text(0);
			String encoded = value_text(1);
			boolean m = nonNull(raw) && nonNull(encoded)
					&& passwordEncoder.matches(raw, encoded);
			result(m ? 1 : 0);
		}
	}

	@Component
	static class MyAuthenticationFailureHandler
			implements AuthenticationFailureHandler {
		@Autowired private AuthProperties properties;

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

	@Bean
	LogoutHandler logoutHandler() {
		SecurityContextLogoutHandler sclh = new SecurityContextLogoutHandler();
		sclh.setClearAuthentication(true);
		sclh.setInvalidateHttpSession(true);
		return sclh;
	}

	@Bean
	ViewResolver jspViewResolver() {
		InternalResourceViewResolver bean = new InternalResourceViewResolver();
		bean.setPrefix("/WEB-INF/views/");
		bean.setSuffix(".jsp");
		return bean;
	}

	/**
	 * Sets up the login page mapping.
	 */
	@Configuration
	static class MvcConfig implements WebMvcConfigurer {
		@Override
		public void addViewControllers(ViewControllerRegistry registry) {
			registry.addViewController("/login.html");
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/resources/**").addResourceLocations(
					"classpath:/META-INF/public-web-resources/");
		}
	}

	/**
	 * Because Spring otherwise can't apply {@link PostFilter} to
	 * {@link Stream}.
	 *
	 * @author Donal Fellows
	 * @see <a href="https://stackoverflow.com/q/66107075/301832">Stack
	 *      Overflow</a>
	 */
	@Service
	static class AnyTypeMethodSecurityExpressionHandler
			extends DefaultMethodSecurityExpressionHandler
			implements MethodSecurityExpressionHandler {
		@Override
		public Object filter(Object target, Expression expr,
				EvaluationContext ctx) {
			if (isNull(target)) {
				// We can handle this case here!
				return null;
			}
			if (target instanceof Collection || target.getClass().isArray()
					|| target instanceof Map || target instanceof Stream) {
				return super.filter(target, expr, ctx);
			}
			if (target instanceof Optional) {
				return filterOptional((Optional<?>) target, expr, ctx);
			} else {
				return filterOptional(Optional.of(target), expr, ctx)
						.orElse(null);
			}
		}

		@SuppressWarnings("unchecked")
		private <T> Optional<T> filterOptional(Optional<T> target,
				Expression expr, EvaluationContext ctx) {
			// Java 8 language profile makes this a little messy
			List<T> a = new ArrayList<T>();
			target.ifPresent(a::add);
			return ((Stream<T>) super.filter(a.stream(), expr, ctx))
					.findFirst();
		}
	}

	/**
	 * Encodes what a user is permitted to do. Abstracts over several types of
	 * security context.
	 */
	public static final class Permit {
		/** Is the user an admin? */
		public final boolean admin;

		/** What is the name of the user? */
		public final String name;

		private List<String> authorities = new ArrayList<>();

		private static final String[] STDAUTH = {
			GRANT_ADMIN, GRANT_READER, GRANT_USER
		};

		/**
		 * Build a permit.
		 *
		 * @param context
		 *            The originating security context.
		 */
		public Permit(javax.ws.rs.core.SecurityContext context) {
			for (String role : STDAUTH) {
				if (context.isUserInRole(role)) {
					authorities.add(role);
				}
			}
			admin = authorities.contains(GRANT_ADMIN);
			name = context.getUserPrincipal().getName();
		}

		/**
		 * Build a permit.
		 *
		 * @param context
		 *            The originating security context.
		 */
		public Permit(SecurityContext context) {
			for (GrantedAuthority ga : context.getAuthentication()
					.getAuthorities()) {
				authorities.add(ga.getAuthority());
			}
			admin = authorities.contains(GRANT_ADMIN);
			name = context.getAuthentication().getName();
		}

		/**
		 * Build a permit for a service user. The service user can create jobs
		 * and read job details, but cannot do much with jobs owned by other
		 * users. <em>Only used by the legacy interface.</em>
		 *
		 * @param serviceUser
		 *            The user name. Must exist in order to be actually used.
		 */
		public Permit(String serviceUser) {
			authorities.add(GRANT_READER);
			authorities.add(GRANT_USER);
			admin = false;
			name = serviceUser;
		}

		/**
		 * Can something owned by a given user can be shown to the user that
		 * this permit is for?
		 *
		 * @param owner
		 *            The owner of the object.
		 * @return True exactly if the object (or subset of properties) may be
		 *         shown.
		 */
		public boolean unveilFor(String owner) {
			return admin || owner.equals(name);
		}

		/**
		 * Mark the current thread as having permission to access objects. Used
		 * to elevate the capabilities of an asynchronous worker thread.
		 *
		 * @return A handle that will de-authorize the thread when closed.
		 */
		public QuietCloseable authorizeCurrentThread() {
			SecurityContext c = SecurityContextHolder.getContext();
			c.setAuthentication(new TempAuth());
			return () -> {
				c.setAuthentication(null);
			};
		}

		/**
		 * An auto-closeable that guarantees that it doesn't throw when closed.
		 */
		public interface QuietCloseable extends AutoCloseable {
			@Override
			void close();
		}

		/**
		 * A temporarily-installable authentication token. Allows access to
		 * secured APIs in asynchronous worker threads, provided they provide a
		 * {@link Permit} (obtained from a service thread) to show that they may
		 * do so.
		 *
		 */
		@SuppressWarnings("serial")
		private final class TempAuth implements Authentication {
			// The permit already proves we're authenticated
			private boolean auth = true;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				return authorities.stream().map(SimpleGrantedAuthority::new)
						.collect(toList());
			}

			@Override
			public Object getCredentials() {
				// You can never get the credentials from this
				return null;
			}

			@Override
			public Permit getDetails() {
				return Permit.this;
			}

			@Override
			public String getPrincipal() {
				return name;
			}

			@Override
			public boolean isAuthenticated() {
				return auth;
			}

			@Override
			public void setAuthenticated(boolean isAuthenticated)
					throws IllegalArgumentException {
				if (!isAuthenticated) {
					auth = false;
				}
			}
		}
	}
}
