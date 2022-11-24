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

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_APPLICATION;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_SUPPORT;
import static uk.ac.manchester.spinnaker.alloc.security.AppAuthTransformationFilter.clearToken;
import static uk.ac.manchester.spinnaker.alloc.security.Utils.installInjectableTrustStoreAsDefault;
import static uk.ac.manchester.spinnaker.alloc.security.Utils.loadTrustStore;
import static uk.ac.manchester.spinnaker.alloc.security.Utils.trustManager;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
@EnableGlobalMethodSecurity(prePostEnabled = true)
@UsedInJavadocOnly(PreAuthorize.class)
public class SecurityConfig {
	private static final Logger log = getLogger(SecurityConfig.class);

	/** How to assert that a user must be an admin. */
	public static final String IS_ADMIN = "hasRole('ADMIN')";

	/** How to assert that a user must be able to read summaries. */
	public static final String IS_READER = "hasRole('READER')";

	/** How to filter out job details that a given user may see (or not). */
	public static final String MAY_SEE_JOB_DETAILS = "#permit.admin or "
			+ " #permit.name == filterObject.owner.orElse(null)";

    private final ParameterizedTypeReference<Map<String, Object>>
    		PARAMETERIZED_RESPONSE_TYPE =
    			new ParameterizedTypeReference<Map<String, Object>>() {};

	private static final MediaType DEFAULT_CONTENT_TYPE = MediaType
			.valueOf(MediaType.APPLICATION_FORM_URLENCODED_VALUE +
					";charset=UTF-8");
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

	@Value("${spring.security.oauth2.resourceserver.opaquetoken.introspection-uri}")
	private String introspectionUri;

	@Value("${spring.security.oauth2.resourceserver.opaquetoken.userinfo-uri}")
	private String userInfoUri;

	@Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}")
	private String clientId;

	@Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}")
	private String clientSecret;

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
	 * @throws Exception
	 *             If anything goes wrong with setting up.
	 */
	private void defineAccessPolicy(HttpSecurity http) throws Exception {
		http.authorizeRequests()
				// General metadata pages require ADMIN access
				.antMatchers(urlMaker.serviceUrl("info*"),
						urlMaker.serviceUrl("info/**"))
				.hasRole("ADMIN")
				// Login process and static resources are available to all
				.antMatchers(urlMaker.systemUrl("login*"),
						urlMaker.systemUrl("perform_*"), oidcPath("**"),
						urlMaker.systemUrl("error"),
						urlMaker.systemUrl("resources/*"))
				.permitAll()
				// Everything else requires post-login
				.anyRequest().authenticated();
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
			http.httpBasic().authenticationEntryPoint(authenticationEntryPoint);
		}
		if (properties.getOpenid().isEnable()) {
			http.oauth2ResourceServer()
					.authenticationEntryPoint(authenticationEntryPoint)
					.opaqueToken().introspector(new UserInfoOpaqueTokenIntrospector());
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
			http.oauth2Login().loginPage(loginUrl)
					.loginProcessingUrl(oidcPath("login/code/*"))
					.authorizationEndpoint().baseUri(oidcPath("auth")).and()
					.defaultSuccessUrl(rootPage, true)
					.failureUrl(loginUrl + "?error=true").userInfoEndpoint()
					.userAuthoritiesMapper(userAuthoritiesMapper());
			http.oauth2Client();
		}
		if (properties.isLocalForm()) {
			http.formLogin().loginPage(loginUrl)
					.loginProcessingUrl(urlMaker.systemUrl("perform_login"))
					.defaultSuccessUrl(rootPage, true)
					.failureUrl(loginUrl + "?error=true")
					.failureHandler(authenticationFailureHandler);
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
		http.logout().logoutUrl(urlMaker.systemUrl("perform_logout"))
				.addLogoutHandler((req, resp, auth) -> clearToken(req))
				.deleteCookies(SESSION_COOKIE).invalidateHttpSession(true)
				.logoutSuccessUrl(loginUrl);
	}

	/**
	 * Define our main security controls.
	 *
	 * @param http
	 *            Used to build the filter chain.
	 * @return The filter chain that implements the controls.
	 * @throws Exception
	 *             If anything goes wrong with setting up. Not expected.
	 */
	@Bean
	@Role(ROLE_SUPPORT)
	public SecurityFilterChain securityFilter(HttpSecurity http)
			throws Exception {
		defineAccessPolicy(http);
		defineAPILoginRules(http);
		defineWebUILoginRules(http);
		defineLogoutRules(http);
		http.addFilterAfter(authApplicationFilter,
				BasicAuthenticationFilter.class);
		return http.build();
	}


	private final class UserInfoOpaqueTokenIntrospector
			implements OpaqueTokenIntrospector {
	    private final OpaqueTokenIntrospector delegate =
	    		new SpringOpaqueTokenIntrospector(
	    				introspectionUri, clientId, clientSecret);


	    @Override
	    public OAuth2AuthenticatedPrincipal introspect(String token) {
	        OAuth2AuthenticatedPrincipal authorized = this.delegate.introspect(
	        		token);
	        Instant issuedAt = authorized.getAttribute("issued-at");
	        Instant expiresAt = authorized.getAttribute("expires-at");
	        OAuth2AccessToken reqtoken = new OAuth2AccessToken(TokenType.BEARER,
	        		token, issuedAt, expiresAt);

	        RestTemplate restTemplate = new RestTemplate();
			restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
	        HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(
					MediaType.APPLICATION_JSON));
			URI uri = UriComponentsBuilder.fromUriString(userInfoUri).build()
					.toUri();
			headers.setContentType(DEFAULT_CONTENT_TYPE);
			MultiValueMap<String, String> formParameters =
					new LinkedMultiValueMap<>();
			formParameters.add(OAuth2ParameterNames.ACCESS_TOKEN,
					reqtoken.getTokenValue());
			RequestEntity<?> request = new RequestEntity<>(formParameters,
					headers, HttpMethod.POST, uri);
			ResponseEntity<Map<String, Object>> response =
					restTemplate.exchange(request, PARAMETERIZED_RESPONSE_TYPE);

			Map<String, Object> userAttributes = response.getBody();
			Collection<GrantedAuthority> authorities = new LinkedHashSet<>();
			OidcUserAuthority auth = new OidcUserAuthority(
					 new OidcIdToken(token, issuedAt, expiresAt, userAttributes),
					 new OidcUserInfo(userAttributes));
			localAuthProvider.mapAuthorities(auth, authorities);
			return new DefaultOAuth2User(authorities, userAttributes,
					"preferred_username");
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
