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
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

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
@EnableWebSecurity
@Role(ROLE_APPLICATION)
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
		OpenIDProperties p = props.getOpenid();
		X509TrustManager tm = trustManager(loadTrustStore(p));
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
	private AppAuthTransformationFilter authApplicationFilter;

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

	private String oidcPath(String suffix) {
		return urlMaker.systemUrl("perform_oidc/" + suffix);
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
						oidcPath("**"),
						urlMaker.systemUrl("error"),
						urlMaker.systemUrl("resources/*"))
				.permitAll()
				// Everything else requires post-login
				.anyRequest().authenticated();
		if (properties.isBasic()) {
			http.httpBasic().authenticationEntryPoint(authenticationEntryPoint);
		}
		String loginUrl = urlMaker.systemUrl("login.html");
		String rootPage = urlMaker.systemUrl("");
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
					.failureUrl(loginUrl + "?error=true");
			http.oauth2Client();
			http.oauth2ResourceServer()
					.authenticationEntryPoint(authenticationEntryPoint).jwt();
			http.addFilterAfter(authApplicationFilter,
					BasicAuthenticationFilter.class);
		}
		if (properties.isLocalForm()) {
			http.formLogin().loginPage(loginUrl)
					.loginProcessingUrl(urlMaker.systemUrl("perform_login"))
					.defaultSuccessUrl(rootPage, true)
					.failureUrl(loginUrl + "?error=true")
					.failureHandler(authenticationFailureHandler);
		}
		/*
		 * Logging out is common code, but pretty pointless for Basic Auth as
		 * browsers will just log straight back in again. Still, it is
		 * meaningful.
		 */
		http.logout().logoutUrl(urlMaker.systemUrl("perform_logout"))
				.addLogoutHandler((req, resp, auth) -> clearToken(req))
				.deleteCookies(SESSION_COOKIE).invalidateHttpSession(true)
				.logoutSuccessUrl(loginUrl);
	}

	GrantedAuthoritiesMapper userAuthoritiesMapper() {
		return authorities -> {
			Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
			authorities.forEach(authority -> {
				/*
				 * Check for OidcUserAuthority because Spring Security 5.2
				 * returns each scope as a GrantedAuthority, which we don't care
				 * about.
				 */
				if (authority instanceof OidcUserAuthority) {
					localAuthProvider.mapAuthorities(
							(OidcUserAuthority) authority, mappedAuthorities);
				} else {
					mappedAuthorities.add(authority);
				}
			});
			return mappedAuthorities;
		};
	}

	@Bean
	@Role(ROLE_SUPPORT)
	LogoutHandler logoutHandler() {
		SecurityContextLogoutHandler sclh = new SecurityContextLogoutHandler();
		sclh.setClearAuthentication(true);
		sclh.setInvalidateHttpSession(true);
		return sclh;
	}
}
