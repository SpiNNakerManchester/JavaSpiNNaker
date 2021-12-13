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
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel.USER;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfigUtils.installInjectableTrustStoreAsDefault;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfigUtils.trustManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
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
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.ServiceConfig.URLPathMaker;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AuthProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.OpenIDProperties;

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

	private static final String SESSION_COOKIE = "JSESSIONID";

	// ------------------------------------------------------------------------
	// What follows is UGLY stuff to make Java open HTTPS right
	private static X509TrustManager customTm;

	// Static because it has to be done very early.
	static {
		try {
			installInjectableTrustStoreAsDefault(() -> customTm);
			log.info("custom SSL trust injection point installed");
		} catch (Exception e) {
			throw new RuntimeException("failed to set up SSL trust", e);
		}
	}

	/**
	 * Builds a custom trust manager to plug into the Java runtime. This is so
	 * that we can access resources managed by Keycloak, which is necessary
	 * because Java doesn't trust its certificate by default (for messy
	 * reasons).
	 *
	 * @param props
	 *            Configuration properties
	 * @return the custom trust manager, <em>already injected</em>
	 * @throws IOException
	 *             If the trust store can't be loaded because of I/O
	 * @throws GeneralSecurityException
	 *             If there is a security problem with the trust store
	 * @see <a href="https://stackoverflow.com/a/24561444/301832">Stack
	 *      Overflow</a>
	 */
	@Bean
	static X509TrustManager customTrustManager(AuthProperties props)
			throws IOException, GeneralSecurityException {
		OpenIDProperties p = props.getOpenid();

		KeyStore myTrustStore = KeyStore.getInstance(p.getTruststoreType());
		try (InputStream myCerts = p.getTruststorePath().getInputStream()) {
			myTrustStore.load(myCerts, p.getTruststorePassword().toCharArray());
		}

		X509TrustManager tm = trustManager(myTrustStore);
		customTm = tm;
		log.info("set trust store from {}", p.getTruststorePath().getURI());
		return tm;
	}

	// ------------------------------------------------------------------------

	@Autowired
	private BasicAuthEntryPoint authenticationEntryPoint;

	@Autowired
	private LocalAuthenticationProvider localAuthProvider;

	@Autowired
	private AuthenticationFailureHandler authenticationFailureHandler;

	@Autowired
	private AuthProperties properties;

	@Autowired
	private URLPathMaker urlMaker;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth)
			throws Exception {
		auth.authenticationProvider(localAuthProvider);
	}

	/**
	 * Just how trusted is a user?
	 */
	public enum TrustLevel {
		/** Grants no real permissions at all. */
		BASIC(),
		/**
		 * Grants read-only permissions in addition to {@link #BASIC}.
		 *
		 * @see SecurityConfig#GRANT_READER
		 */
		READER(GRANT_READER),
		/**
		 * Grants job creation and management permissions in addition to
		 * {@link #READER}.
		 *
		 * @see SecurityConfig#GRANT_USER
		 */
		USER(GRANT_READER, GRANT_USER),
		/**
		 * Grants service administration permissions in addition to
		 * {@link #USER}.
		 *
		 * @see SecurityConfig#GRANT_ADMIN
		 */
		ADMIN(GRANT_READER, GRANT_USER, GRANT_ADMIN);

		private List<String> grants;

		TrustLevel(String... grants) {
			this.grants = asList(grants);
		}

		Stream<GrantedAuthority> getGrants() {
			return grants.stream().map(SimpleGrantedAuthority::new);
		}
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
				.antMatchers(urlMaker.serviceUrl("info*"),
						urlMaker.serviceUrl("info/**"))
				.hasRole("ADMIN")
				// Login process and static resources are available to all
				.antMatchers(urlMaker.systemUrl("login*"),
						urlMaker.systemUrl("perform_*"),
						urlMaker.systemUrl("perform_oidc/**"),
						urlMaker.systemUrl("error"),
						urlMaker.systemUrl("resources/*"))
				.permitAll()
				// Everything else requires post-login
				.anyRequest().authenticated();
		if (properties.isBasic()) {
			http.httpBasic().authenticationEntryPoint(authenticationEntryPoint);
		}
		String loginUrl = urlMaker.systemUrl("login.html");
		if (properties.getOpenid().isEnable()) {
			/*
			 * We're both, so we can have logins AND tokens. The logins are for
			 * using the HTML UI, and the tokens are for using from SpiNNaker
			 * tools (especially within the collabratory and the Jupyter
			 * notebook).
			 */
			http.oauth2Login().loginPage(urlMaker.systemUrl("login.html"))
					.loginProcessingUrl(
							urlMaker.systemUrl("perform_oidc/login/code/*"))
					.authorizationEndpoint(c -> {
						c.baseUri(urlMaker.systemUrl("perform_oidc/auth"));
					}).defaultSuccessUrl(urlMaker.systemUrl(""), true)
					.failureUrl(loginUrl + "?error=true");
			http.oauth2Client();
			http.oauth2ResourceServer(oauth -> oauth.jwt());
		}
		if (properties.isLocalForm()) {
			http.formLogin().loginPage(loginUrl)
					.loginProcessingUrl(urlMaker.systemUrl("perform_login"))
					.defaultSuccessUrl(urlMaker.systemUrl(""), true)
					.failureUrl(loginUrl + "?error=true")
					.failureHandler(authenticationFailureHandler);
		}
		/*
		 * Logging out is common code, but pretty pointless for Basic Auth as
		 * browsers will just log straight back in again. Still, it is
		 * meaningful.
		 */
		http.logout().logoutUrl(urlMaker.systemUrl("perform_logout"))
				.deleteCookies(SESSION_COOKIE).invalidateHttpSession(true)
				.logoutSuccessUrl(loginUrl);
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
	static class AccessDeniedExceptionMapper
			implements ExceptionMapper<AccessDeniedException> {
		@Context
		private UriInfo ui;

		@Context
		private HttpServletRequest req;

		@Override
		public Response toResponse(AccessDeniedException exception) {
			// Actually produce useful logging; the default is ghastly!
			Principal p = req.getUserPrincipal();
			if (p instanceof AbstractAuthenticationToken) {
				AbstractAuthenticationToken who =
						(AbstractAuthenticationToken) p;
				log.warn("access denied: {} : {} {}", ui.getAbsolutePath(),
						who.getName(),
						who.getAuthorities().stream()
								.map(GrantedAuthority::getAuthority)
								.collect(toSet()));
			} else if (p instanceof OAuth2AuthenticatedPrincipal) {
				OAuth2AuthenticatedPrincipal who =
						(OAuth2AuthenticatedPrincipal) p;
				log.warn("access denied: {} : {} {}", ui.getAbsolutePath(),
						who.getName(),
						who.getAuthorities().stream()
								.map(GrantedAuthority::getAuthority)
								.collect(toSet()));
			}
			// But the user gets a bland response
			return status(FORBIDDEN).entity(BLAND_AUTH_MSG).build();
		}
	}

	@Component
	static class MyAuthenticationFailureHandler
			implements AuthenticationFailureHandler {
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
	 * Misc services related to password handling.
	 *
	 * @author Donal Fellows
	 */
	@Component
	public static class PasswordServices {
		private static final int BCRYPT_STRENGTH = 10;

		private static final int PASSWORD_LENGTH = 16;

		private PasswordEncoder passwordEncoder;

		private SecureRandom rng;

		@PostConstruct
		private void init() {
			rng = new SecureRandom();
			passwordEncoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH, rng);
		}

		/**
		 * Generate a random password.
		 *
		 * @return A password consisting of 16 random ASCII printable
		 *         characters.
		 */
		public final String generatePassword() {
			StringBuilder sb = new StringBuilder();
			rng.ints(PASSWORD_LENGTH, '\u0021', '\u007f')
					.forEachOrdered(c -> sb.append((char) c));
			return sb.toString();
		}

		/**
		 * Encode a password with bcrypt. <em>This is a slow operation! Do not
		 * hold a database transaction open when calling this.</em>
		 *
		 * @param password
		 *            The password to encode. May be {@code null}.
		 * @return The encoded password. Will be {@code null} if the input is
		 *         {@code null}.
		 */
		public final String encodePassword(String password) {
			return isNull(password) ? null : passwordEncoder.encode(password);
		}

		/**
		 * Check using bcrypt if a password matches its encoded form retrieved
		 * from the database. <em>This is a slow operation! Do not hold a
		 * database transaction open when calling this.</em>
		 *
		 * @param password
		 *            The password to check. If {@code null}, the encoded form
		 *            must also be {@code null} for a match to be true.
		 * @param encodedPassword
		 *            The encoded form from the database.
		 * @return True if the password matches, false otherwise.
		 */
		public final boolean matchPassword(String password,
				String encodedPassword) {
			if (isNull(password)) {
				return isNull(encodedPassword);
			}
			return nonNull(encodedPassword)
					&& passwordEncoder.matches(password, encodedPassword);
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
	@Component
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

		private List<GrantedAuthority> authorities = new ArrayList<>();

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
					authorities.add(new SimpleGrantedAuthority(role));
				}
			}
			admin = isAdmin(authorities);
			name = context.getUserPrincipal().getName();
		}

		private static boolean isAdmin(List<GrantedAuthority> auths) {
			return auths.stream().map(GrantedAuthority::getAuthority)
					.anyMatch(GRANT_ADMIN::equals);
		}

		/**
		 * Build a permit.
		 *
		 * @param context
		 *            The originating security context.
		 */
		public Permit(SecurityContext context) {
			authorities = new ArrayList<>(
					context.getAuthentication().getAuthorities());
			admin = isAdmin(authorities);
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
			USER.getGrants().forEach(authorities::add);
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
		 * Push our special temporary authentication object for the duration of
		 * the inner code. Used to satisfy Spring method security.
		 *
		 * @param <T>
		 *            The type of the result
		 * @param inContext
		 *            The inner code to run with an authentication object
		 *            applied.
		 * @return Whatever the inner code returns
		 */
		public <T> T authorize(Supplier<T> inContext) {
			/**
			 * A temporarily-installable authentication token. Allows access to
			 * secured APIs in asynchronous worker threads, provided they
			 * provide a {@link Permit} (obtained from a service thread) to show
			 * that they may do so.
			 */
			@SuppressWarnings("serial")
			class TempAuth implements Authentication {
				// The permit already proves we're authenticated
				private boolean auth = true;

				@Override
				public String getName() {
					return name;
				}

				@Override
				public Collection<? extends GrantedAuthority> getAuthorities() {
					return unmodifiableCollection(authorities);
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
				public void setAuthenticated(boolean isAuthenticated) {
					if (!isAuthenticated) {
						auth = false;
					}
				}

				private void writeObject(ObjectOutputStream out)
						throws NotSerializableException {
					throw new NotSerializableException(
							"not actually serializable");
				}
			}

			SecurityContext c = SecurityContextHolder.getContext();
			Authentication old = c.getAuthentication();
			c.setAuthentication(new TempAuth());
			try {
				return inContext.get();
			} finally {
				c.setAuthentication(old);
			}
		}
	}
}

/** Support utility methods. */
abstract class SecurityConfigUtils {
	private SecurityConfigUtils() {
	}

	/**
	 * Load a trust store into a trust manager.
	 *
	 * @param truststore
	 *            The trust store to load, or {@code null} to use the default.
	 * @return The configured trust manager.
	 * @throws GeneralSecurityException
	 *             If things go wrong.
	 * @see <a href="https://stackoverflow.com/a/24561444/301832">Stack
	 *      Overflow</a>
	 */
	static X509TrustManager trustManager(KeyStore truststore)
			throws GeneralSecurityException {
		TrustManagerFactory tmf =
				TrustManagerFactory.getInstance(getDefaultAlgorithm());
		tmf.init(truststore);
		for (TrustManager tm : tmf.getTrustManagers()) {
			if (tm instanceof X509TrustManager) {
				return (X509TrustManager) tm;
			}
		}
		return null;
	}

	/**
	 * Set up the default SSL context so that we can inject our trust store for
	 * verifying servers.
	 *
	 * @param injector
	 *            How to get the value to inject.
	 * @throws GeneralSecurityException
	 *             If the SSL context can't be built.
	 * @see <a href="https://stackoverflow.com/a/24561444/301832">Stack
	 *      Overflow</a>
	 */
	static void installInjectableTrustStoreAsDefault(
			Supplier<X509TrustManager> injector)
			throws GeneralSecurityException {
		X509TrustManager defaultTm = trustManager(null);
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[] {
			new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return defaultTm.getAcceptedIssuers();
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
					X509TrustManager customTm = injector.get();
					if (customTm != null) {
						try {
							customTm.checkServerTrusted(chain, authType);
							// If we got here, we passed!
							return;
						} catch (CertificateException e) {
						}
					}
					defaultTm.checkServerTrusted(chain, authType);
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
					defaultTm.checkClientTrusted(chain, authType);
				}
			}
		}, null);
		SSLContext.setDefault(sslContext);
	}
}
