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

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_APPLICATION;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_SUPPORT;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;
import static org.springframework.util.StreamUtils.copyToByteArray;
import static uk.ac.manchester.spinnaker.alloc.security.AppAuthTransformationFilter.clearToken;
import static uk.ac.manchester.spinnaker.alloc.security.Utils.installInjectableTrustStoreAsDefault;
import static uk.ac.manchester.spinnaker.alloc.security.Utils.loadTrustStore;
import static uk.ac.manchester.spinnaker.alloc.security.Utils.trustManager;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.LogFactory;
import org.hobsoft.spring.resttemplatelogger.LogFormatter;
import org.hobsoft.spring.resttemplatelogger.LoggingCustomizer;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import jakarta.servlet.DispatcherType;
import uk.ac.manchester.spinnaker.alloc.ServiceConfig.URLPathMaker;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AuthProperties;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

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
@EnableWebSecurity
@Role(ROLE_APPLICATION)
@EnableMethodSecurity(prePostEnabled = true)
@UsedInJavadocOnly(PreAuthorize.class)
public class SecurityConfig {
	private static final Logger log = getLogger(SecurityConfig.class);

	/** How to assert that a user must be an admin. */
	public static final String IS_ADMIN = "hasRole('ADMIN')";

	/** How to assert that a user must be an admin. */
	public static final String IS_NMPI_EXEC = "hasRole('NMPI_EXEC')";

	/** How to assert that a user must be able to read summaries. */
	public static final String IS_READER = "hasRole('READER')";

	/** How to filter out job details that a given user may see (or not). */
	public static final String MAY_SEE_JOB_DETAILS = "#permit.admin or "
			+ " #permit.nmpiexec or "
			+ " #permit.name == filterObject.owner.orElse(null)";

	private static final ParameterizedTypeReference<
			Map<String, Object>> PARAMETERIZED_RESPONSE_TYPE =
					new ParameterizedTypeReference<>() {
					};

	/**
	 * How to assert that a user must be able to make jobs and read job details
	 * in depth.
	 */
	public static final String IS_USER = "hasRole('USER')";

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
	@Role(ROLE_SUPPORT)
	static X509TrustManager customTrustManager(AuthProperties props)
			throws IOException, GeneralSecurityException {
		var p = props.getOpenid();
		var tm = trustManager(loadTrustStore(p));
		customTm = tm;
		log.info("set trust store from {}", p.getTruststorePath().getURI());
		return tm;
	}

	// ------------------------------------------------------------------------

	@Autowired
	private BasicAuthEntryPoint authenticationEntryPoint;

	@Autowired
	private LocalAuthenticationProvider<?> localAuthProvider;

	@Autowired
	private AppAuthTransformationFilter authApplicationFilter;

	@Autowired
	private AuthenticationFailureHandler authenticationFailureHandler;

	@Autowired
	private AuthProperties properties;

	@Autowired
	private URLPathMaker urlMaker;

	/**
	 * Configure things we plug into.
	 *
	 * @param auth
	 *            The authentication manager builder to configure.
	 */
	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) {
		auth.authenticationProvider(localAuthProvider);
	}

	private String oidcPath(String suffix) {
		return urlMaker.systemUrl("perform_oidc/" + suffix);
	}

	/**
	 * Set up access control policies where they're not done by method security.
	 * The {@code /info} part reveals admin details; you need {@code ROLE_ADMIN}
	 * to view it. Everything to do with logging in <strong>must not</strong>
	 * require being logged in. For anything else, as long as you are
	 * authenticated we're happy. <em>Some</em> calls have additional
	 * requirements; those are annotated with {@link PreAuthorize @PreAuthorize}
	 * and a suitable auth expression.
	 *
	 * @param http
	 *            Where the configuration is applied to.
	 * @param introspector
	 *           The introspector used to build request matchers.
	 * @throws Exception
	 *             If anything goes wrong with setting up.
	 */
	private void defineAccessPolicy(HttpSecurity http,
			HandlerMappingIntrospector introspector) throws Exception {
		http.authorizeHttpRequests((authorize) -> authorize
				// Allow forwarded requests
				.dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
				// Login process and static resources are available to all
				.requestMatchers(antMatcher(urlMaker.systemUrl("login*")),
						antMatcher(urlMaker.systemUrl("perform_*")),
						antMatcher(oidcPath("**")),
						antMatcher(urlMaker.systemUrl("error")),
						antMatcher(urlMaker.systemUrl("resources/*")),
						antMatcher(urlMaker.serviceUrl("openapi.json")),
						antMatcher(urlMaker.serviceUrl("swagger*")),
						antMatcher(urlMaker.serviceUrl("index.css")))
				.permitAll()
				// Everything else requires post-login
				.anyRequest().authenticated());
	}

	/**
	 * How we handle the mechanics of login with the REST API.
	 *
	 * @param http
	 *            Where the configuration is applied to.
	 * @throws Exception
	 *             If anything goes wrong with setting up. Not expected.
	 */
	private void defineAPILoginRules(HttpSecurity http) throws Exception {
		if (properties.isBasic()) {
			http.httpBasic((authorize) -> authorize
					.authenticationEntryPoint(authenticationEntryPoint));
		}
		if (properties.getOpenid().isEnable()) {
			http.oauth2ResourceServer((authorize) -> authorize
					.authenticationEntryPoint(authenticationEntryPoint)
					.opaqueToken(oauth2 -> oauth2
						.introspector(new UserInfoOpaqueTokenIntrospector())));
		}
	}

	/**
	 * How we handle the mechanics of login within the web UI.
	 *
	 * @param http
	 *            Where the configuration is applied to.
	 * @throws Exception
	 *             If anything goes wrong with setting up. Not expected.
	 */
	private void defineWebUILoginRules(HttpSecurity http) throws Exception {
		var loginUrl = urlMaker.systemUrl("login.html");
		var rootPage = urlMaker.systemUrl("");
		if (properties.getOpenid().isEnable()) {
			/*
			 * We're both, so we can have logins AND tokens. The logins are for
			 * using the HTML UI, and the tokens are for using from SpiNNaker
			 * tools (especially within the collabratory and the Jupyter
			 * notebook).
			 */
			http.oauth2Login(oauth2 -> oauth2.loginPage(loginUrl)
					.loginProcessingUrl(oidcPath("login/code/*"))
					.authorizationEndpoint(
							auth -> auth.baseUri(oidcPath("auth")))
					.defaultSuccessUrl(rootPage, true)
					.failureUrl(loginUrl + "?error=true")
					.userInfoEndpoint(auth -> auth
						.userAuthoritiesMapper(userAuthoritiesMapper()))
					.permitAll());
			http.oauth2Client(Customizer.withDefaults());
		}
		if (properties.isLocalForm()) {
			http.formLogin(auth -> auth.loginPage(loginUrl)
					.loginProcessingUrl(urlMaker.systemUrl("perform_login"))
					.defaultSuccessUrl(rootPage, true)
					.failureUrl(loginUrl + "?error=true")
					.failureHandler(authenticationFailureHandler)
					.permitAll());
		}
	}

	/**
	 * Logging out is common code between the UI and the API, but pretty
	 * pointless for Basic Auth as browsers will just log straight back in
	 * again. Still, it is meaningful (it invalidates the session).
	 *
	 * @param http
	 *            Where the configuration is applied to.
	 * @throws Exception
	 *             If anything goes wrong with setting up. Not expected.
	 */
	private void defineLogoutRules(HttpSecurity http) throws Exception {
		var loginUrl = urlMaker.systemUrl("login.html");
		http.logout(cust -> cust.logoutUrl(urlMaker.systemUrl("perform_logout"))
				.addLogoutHandler((req, resp, auth) -> clearToken(req))
				.deleteCookies(SESSION_COOKIE).invalidateHttpSession(true)
				.logoutSuccessUrl(loginUrl));
	}

	/**
	 * Define our main security controls.
	 *
	 * @param http
	 *            Used to build the filter chain.
	 * @param introspector
	 *            The introspector used to build request matchers.
	 * @return The filter chain that implements the controls.
	 * @throws Exception
	 *             If anything goes wrong with setting up. Not expected.
	 */
	@Bean
	@Role(ROLE_SUPPORT)
	public SecurityFilterChain securityFilter(HttpSecurity http,
			HandlerMappingIntrospector introspector)
			throws Exception {
		defineAccessPolicy(http, introspector);
		defineAPILoginRules(http);
		defineWebUILoginRules(http);
		defineLogoutRules(http);
		http.addFilterAfter(authApplicationFilter,
				BasicAuthenticationFilter.class);
		return http.build();
	}

	private final class UserInfoOpaqueTokenIntrospector
			implements OpaqueTokenIntrospector {
		private final OpaqueTokenIntrospector delegate;

		private final String userInfoUri;

		private UserInfoOpaqueTokenIntrospector() {
			var p = properties.getOpenid();

			delegate = new SpringOpaqueTokenIntrospector(p.getIntrospection(),
					p.getId(), p.getSecret());
			userInfoUri = p.getUserinfo();
		}

		@Override
		public OAuth2AuthenticatedPrincipal introspect(String token) {
			var authorized = delegate.introspect(token);
			Instant issuedAt = authorized.getAttribute("issued-at");
			Instant expiresAt = authorized.getAttribute("expires-at");

			var userAttributes = userinfo(token);
			var authorities = new LinkedHashSet<GrantedAuthority>();
			var auth = new OidcUserAuthority(
					new OidcIdToken(token, issuedAt, expiresAt, userAttributes),
					new OidcUserInfo(userAttributes));
			localAuthProvider.mapAuthorities(auth, authorities);
			return new DefaultOAuth2User(authorities, userAttributes,
					"preferred_username");
		}

		private Map<String, Object> userinfo(String token) {
			var headers = new HttpHeaders();
			headers.setAccept(List.of(APPLICATION_JSON));
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			var request = new RequestEntity<>(ACCESS_TOKEN + "=" + token,
					headers, POST, URI.create(userInfoUri));

			var restLog = LogFactory.getLog(LoggingCustomizer.class);
			var restTemplate = new RestTemplateBuilder().customizers(
					new LoggingCustomizer(restLog, new Formatter())).build();
			restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
			var response =
					restTemplate.exchange(request, PARAMETERIZED_RESPONSE_TYPE);

			return response.getBody();
		}
	}

	private final class Formatter implements LogFormatter {

		@Override
		public String formatResponse(ClientHttpResponse response)
				throws IOException {
			return String.format("Response:\n    Headers: %s\n    Body: %s",
					response.getHeaders(),
					new String(copyToByteArray(response.getBody())));
		}

		@Override
		public String formatRequest(HttpRequest request, byte[] body) {
			return String.format(
					"%s Request to %s:\n"
					+ "    Headers: %s\n"
					+ "    Body: %s",
					request.getMethod(), request.getURI(), request.getHeaders(),
					new String(body));
		}
	}

	/**
	 * @return A converter that handles the initial extraction of collabratories
	 *         and organisations from the info we have available when a user
	 *         logs in explicitly in the web UI.
	 * @see LocalAuthProviderImpl#mapAuthorities(OidcUserAuthority, Collection)
	 */
	@Bean("hbp.collab-and-org.user-converter.shim")
	@Role(ROLE_SUPPORT)
	GrantedAuthoritiesMapper userAuthoritiesMapper() {
		var baseMapper = new SimpleAuthorityMapper();
		return authorities -> {
			var mappedAuthorities = baseMapper.mapAuthorities(authorities);
			authorities.forEach(authority -> {
				/*
				 * Check for OidcUserAuthority because Spring Security 5.2
				 * returns each scope as a GrantedAuthority, which we don't care
				 * about.
				 */
				if (authority instanceof OidcUserAuthority) {
					localAuthProvider.mapAuthorities(
							(OidcUserAuthority) authority, mappedAuthorities);
				}
				mappedAuthorities.add(authority);
			});
			return mappedAuthorities;
		};
	}

	@Bean
	@Role(ROLE_SUPPORT)
	LogoutHandler logoutHandler() {
		var handler = new SecurityContextLogoutHandler();
		handler.setClearAuthentication(true);
		handler.setInvalidateHttpSession(true);
		return handler;
	}
}
