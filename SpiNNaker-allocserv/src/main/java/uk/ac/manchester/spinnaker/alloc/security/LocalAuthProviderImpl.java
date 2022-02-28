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

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.db.Row.bool;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.isBusy;
import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_READER;
import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_USER;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.security.TrustLevel.ADMIN;
import static uk.ac.manchester.spinnaker.alloc.security.TrustLevel.USER;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AuthProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.QuotaProperties;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.Row;

/**
 * Does authentication against users defined entirely in the database. This
 * includes keeping the users' (encrypted) password in the database. This is
 * primarily focused on the {@code user_info} database table.
 *
 * @see AuthProperties Configuration properties
 * @author Donal Fellows
 */
@Service
public class LocalAuthProviderImpl extends DatabaseAwareBean
		implements LocalAuthenticationProvider {
	private static final Logger log = getLogger(LocalAuthProviderImpl.class);

	@Autowired
	private ServiceMasterControl control;

	@Autowired
	private AuthProperties authProps;

	@Autowired
	private QuotaProperties quotaProps;

	@Autowired
	private PasswordServices passServices;

	private static final String DUMMY_USER = "user1";

	private static final String DUMMY_PASSWORD = "user1Pass";

	@PostConstruct
	private void initUserIfNecessary() {
		if (authProps.isAddDummyUser()) {
			String pass = DUMMY_PASSWORD;
			boolean poorPassword = true;
			if (authProps.isDummyRandomPass()) {
				pass = passServices.generatePassword();
				poorPassword = false;
			}
			if (createUser(DUMMY_USER, pass, ADMIN,
					quotaProps.getDefaultQuota())) {
				if (authProps.isDummyRandomPass()) {
					log.info("admin user {} has password: {}", DUMMY_USER,
							pass);
				}
				if (poorPassword) {
					log.warn("user {} has default password!", DUMMY_USER);
				}
			}
		}
	}

	@Override
	@PreAuthorize(IS_ADMIN)
	public boolean createUser(String username, String password,
			TrustLevel trustLevel, long quota) {
		String name = username.trim();
		if (name.isEmpty()) {
			// Won't touch the DB if the username is empty
			throw new UsernameNotFoundException("empty user name?");
		}
		String encPass = passServices.encodePassword(password);
		try (Connection conn = getConnection();
				Update createUser = conn.update(CREATE_USER);
				Update addQuota = conn.update(ADD_QUOTA_FOR_ALL_MACHINES)) {
			return conn.transaction(() -> createUser(username, encPass,
					trustLevel, quota, createUser, addQuota).isPresent());
		}
	}

	/**
	 * Create a user. A transaction <em>must</em> be held open when calling
	 * this.
	 *
	 * @param username
	 *            The username to create
	 * @param encPass
	 *            The already-encoded password to use; {@code null} for a user
	 *            that needs to authenticate by another mechanism.
	 * @param trustLevel
	 *            What level of permissions to grant
	 * @param quota
	 *            How much quota to allocate
	 * @param createUser
	 *            SQL statement
	 * @param addQuota
	 *            SQL statement
	 * @return The user ID if the user was successfully created.
	 */
	private Optional<Integer> createUser(String username, String encPass,
			TrustLevel trustLevel, long quota, Update createUser,
			Update addQuota) {
		Optional<Integer> userId =
				createUser.key(username, encPass, trustLevel, false);
		userId.ifPresent(id -> {
			addQuota.call(userId, quota);
			log.info(
					"added user {} with trust level {} and "
							+ "quota {} board-seconds",
					username, trustLevel, quota);
		});
		return userId;
	}

	/** Marks an authentication that we've already taken a decision about. */
	private interface AlreadyDoneMarker extends Authentication {
	}

	@Override
	public Authentication authenticate(Authentication auth)
			throws AuthenticationException {
		if (isNull(auth)) {
			return null;
		}
		log.debug("requesting auth decision about {}", auth);
		if (auth instanceof AlreadyDoneMarker) {
			// It's ours; don't repeat the auth
			return auth;
		}

		try {
			if (auth instanceof UsernamePasswordAuthenticationToken) {
				return authenticateDirect(
						(UsernamePasswordAuthenticationToken) auth);
			} else if (auth instanceof OAuth2AuthenticationToken) {
				return authenticateOpenId((OAuth2AuthenticationToken) auth);
			} else if (auth instanceof JwtAuthenticationToken) {
				return authenticateOpenId((JwtAuthenticationToken) auth);
			} else {
				return null;
			}
		} catch (DataAccessException e) {
			throw new InternalAuthenticationServiceException(
					"database access problem", e);
		}
	}

	@Override
	public Authentication updateAuthentication(SecurityContext ctx) {
		Authentication current = ctx.getAuthentication();
		if (nonNull(current)) {
			if (supports(current.getClass())) {
				Authentication updated = authenticate(current);
				if (nonNull(updated) && updated != current) {
					log.debug("filter updated security from {} to {}", current,
							updated);
					ctx.setAuthentication(updated);
					return updated;
				}
			} else if (!isUnsupportedAuthTokenClass(current.getClass())) {
				log.warn("unexpected authentication type {} (token: {})",
						current.getClass(), current);
			}
		}
		return null;
	}

	/** The classes that we know what to do about. */
	private static final Class<?>[] SUPPORTED_AUTH_TOKEN_CLASSES = {
		UsernamePasswordAuthenticationToken.class,
		OAuth2AuthenticationToken.class, JwtAuthenticationToken.class,
		AlreadyDoneMarker.class
	};

	/** The classes that we <em>know</em> we don't ever want to handle. */
	private static final Class<?>[] UNSUPPORTED_AUTH_TOKEN_CLASSES = {
		AnonymousAuthenticationToken.class, RememberMeAuthenticationToken.class,
		RunAsUserToken.class, TestingAuthenticationToken.class
	};

	@Override
	public boolean supports(Class<?> cls) {
		for (Class<?> c : SUPPORTED_AUTH_TOKEN_CLASSES) {
			if (c.isAssignableFrom(cls)) {
				return true;
			}
		}
		if (!isUnsupportedAuthTokenClass(cls)) {
			log.warn("asked about supporting {}", cls);
		}
		return false;
	}

	/**
	 * Check if we're talking about a class that we know we don't ever want to
	 * handle.
	 *
	 * @param cls
	 *            The class to check.
	 * @return {@code true} if we never want to take a full auth decision about
	 *         it.
	 */
	static boolean isUnsupportedAuthTokenClass(Class<?> cls) {
		for (Class<?> c : UNSUPPORTED_AUTH_TOKEN_CLASSES) {
			if (c.isAssignableFrom(cls)) {
				return true;
			}
		}
		return false;
	}

	private static class PerformedUsernamePasswordAuthenticationToken extends
			UsernamePasswordAuthenticationToken implements AlreadyDoneMarker {
		private static final long serialVersionUID = -3164620207079316329L;

		PerformedUsernamePasswordAuthenticationToken(String name,
				String password, List<GrantedAuthority> authorities) {
			super(name, password, authorities);
		}
	}

	private UsernamePasswordAuthenticationToken
			authenticateDirect(UsernamePasswordAuthenticationToken auth)
					throws AuthenticationException {
		log.info("authenticating Local Login {}", auth.toString());
		// We ALWAYS trim the username; extraneous whitespace is bogus
		String name = auth.getName().trim();
		if (name.isEmpty()) {
			// Won't touch the DB if the username is empty
			throw new UsernameNotFoundException("empty user name?");
		}
		String password = auth.getCredentials().toString();
		List<GrantedAuthority> authorities = new ArrayList<>();

		try (AuthQueries queries = new AuthQueries()) {
			if (!authLocalAgainstDB(name, password, authorities, queries)) {
				return null;
			}
		}
		return new PerformedUsernamePasswordAuthenticationToken(name, password,
				authorities);
	}

	/**
	 * Convert the authentication from the OpenID service into one we internally
	 * understand.
	 *
	 * @param auth
	 *            The OpenID auth token
	 * @return The internal auth token
	 */
	private Authentication authenticateOpenId(OAuth2AuthenticationToken auth) {
		log.debug("authenticating OpenID {}", auth);
		OAuth2User user = auth.getPrincipal();
		log.info("CHECK attributes: {}", user.getAttributes());
		log.info("POSSIBLE AUTHORITIES: {}", auth.getAuthorities());
		return authorizeOpenId(
				authProps.getOpenid().getUsernamePrefix()
						+ user.getAttribute("preferred_username"),
				user, null, auth.getAuthorities());
	}

	/**
	 * Convert the JWT from the OpenID service into one we internally
	 * understand.
	 *
	 * @param auth
	 *            The OpenID auth token
	 * @return The internal auth token
	 */
	private Authentication authenticateOpenId(JwtAuthenticationToken auth) {
		log.debug("authenticating OpenID {}", auth);
		log.info("CHECK token claims: {}", auth.getToken().getClaims());
		log.info("POSSIBLE AUTHORITIES: {}", auth.getAuthorities());
		return authorizeOpenId(
				authProps.getOpenid().getUsernamePrefix() + auth.getToken()
						.getClaimAsString("preferred_username"),
				null, auth.getToken(), auth.getAuthorities());
	}

	private OpenIDDerivedAuthenticationToken authorizeOpenId(String name,
			OAuth2User user, Jwt bearerToken,
			Collection<GrantedAuthority> authorities) {
		if (isNull(name)
				|| name.equals(authProps.getOpenid().getUsernamePrefix())) {
			// No actual name there?
			log.warn("failed to handle OpenID user with no real user name");
			return null;
		}
		try (AuthQueries queries = new AuthQueries()) {
			if (!queries.transaction(//
					() -> authOpenIDAgainstDB(name, authorities, queries))) {
				return null;
			}
		}
		// Users from OpenID always have the same permissions
		return new OpenIDDerivedAuthenticationToken(name, user, bearerToken);
	}

	/**
	 * An object that can say something about what user it was derived from.
	 * Note that you never have both the user information and the token at the
	 * same time, but they contain fairly similar contents.
	 */
	public interface OpenIDUserAware {
		/**
		 * Get the underlying OpenID user information.
		 *
		 * @return The user info, if known. Pay attention to the attributes.
		 */
		Optional<OAuth2User> getOpenIdUser();

		/**
		 * Get the underlying OpenID user token.
		 *
		 * @return The user's bearer token. Pay attention to the claims.
		 */
		Optional<Jwt> getOpenIdToken();

		/**
		 * Get a claim/attribute that is a string.
		 *
		 * @param claimName
		 *            The name of the claim.
		 * @return The string.
		 * @throws IllegalStateException
		 *             If neither {@link #getOpenIdUser()} nor
		 *             {@link #getOpenIdToken()} provide anything we can get
		 *             claims from.
		 */
		default String getStringClaim(String claimName) {
			return getOpenIdUser()
					.map(u -> Objects.toString(u.getAttribute(claimName)))
					.orElseGet(() -> getOpenIdToken()
							.map(t -> t.getClaimAsString(claimName))
							.orElseThrow(() -> new IllegalStateException(
									"no user or token to supply claim")));
		}

		/**
		 * @return The preferred OpenID user name. <em>We don't use this
		 *         directly</em> as it may clash with other user names.
		 * @throws IllegalStateException
		 *             If the object was made without either a user or a token.
		 */
		default String getOpenIdUserName() {
			return getStringClaim("preferred_username");
		}

		/**
		 * @return The real name of the OpenID user.
		 * @throws IllegalStateException
		 *             If the object was made without either a user or a token.
		 */
		default String getOpenIdName() {
			return getStringClaim("name");
		}

		/**
		 * @return The verified email address of the OpenID user, if available.
		 */
		default Optional<String> getOpenIdEmail() {
			Optional<String> email =
					getOpenIdUser().map(uu -> uu.getAttributes())
							.filter(uu -> uu.containsKey("email_verified"))
							.filter(uu -> (boolean) uu.get("email_verified"))
							.map(uu -> uu.get("email").toString());
			if (email.isPresent()) {
				return email;
			}
			return getOpenIdToken().filter(t -> t.getClaim("email_verified"))
					.map(t -> t.getClaimAsString("email"));
		}
	}

	private static final class OpenIDDerivedAuthenticationToken
			extends AbstractAuthenticationToken
			implements OpenIDUserAware, AlreadyDoneMarker {
		private static final long serialVersionUID = 970898019896708267L;

		private final String who;

		private final OAuth2User user;

		private final Jwt token;

		private OpenIDDerivedAuthenticationToken(String who, OAuth2User user,
				Jwt token) {
			super(asList(new SimpleGrantedAuthority(GRANT_READER),
					new SimpleGrantedAuthority(GRANT_USER)));
			this.who = who;
			this.user = user;
			this.token = token;
		}

		@Override
		public Object getCredentials() {
			// We never have credentials available
			return null;
		}

		@Override
		public Object getPrincipal() {
			return who;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @return The user information.
		 */
		@Override
		public Optional<OAuth2User> getOpenIdUser() {
			return Optional.ofNullable(user);
		}

		/**
		 * {@inheritDoc}
		 *
		 * @return The user's bearer token.
		 */
		@Override
		public Optional<Jwt> getOpenIdToken() {
			return Optional.ofNullable(token);
		}
	}

	/**
	 * Database connection and queries used for authentication and authorization
	 * purposes.
	 */
	private final class AuthQueries extends AbstractSQL {
		private final Query getUserBlocked = conn.query(IS_USER_LOCKED);

		private final Query userAuthorities = conn.query(GET_USER_AUTHORITIES);

		private final Update loginSuccess = conn.update(MARK_LOGIN_SUCCESS);

		private final Query loginFailure = conn.query(MARK_LOGIN_FAILURE);

		private final Update createUser = conn.update(CREATE_USER);

		private final Update addQuota = conn.update(ADD_QUOTA_FOR_ALL_MACHINES);

		/**
		 * Make an instance.
		 */
		AuthQueries() {
		}

		@Override
		public void close() {
			addQuota.close();
			createUser.close();
			loginFailure.close();
			loginSuccess.close();
			userAuthorities.close();
			getUserBlocked.close();
			super.close();
		}

		/**
		 * Get a description of basic auth-related user data.
		 *
		 * @param username
		 *            Who are we fetching for?
		 * @return The DB row describing them. Includes {@code user_id},
		 *         {@code disabled} and {@code locked}.
		 */
		Optional<Row> getUser(String username) {
			return getUserBlocked.call1(username);
		}

		/**
		 * Get the extended auth-related user data.
		 *
		 * @param userId
		 *            Who are we fetching for?
		 * @return The DB row describing them. Includes {@code has_password} and
		 *         {@code trust_level}.
		 */
		Row getUserAuthorities(int userId) {
			/*
			 * I believe the NoSuchElementException can never be thrown; caller
			 * checks the password, we just got the userId from the DB, and
			 * we're in a transaction so the world won't change under our feet.
			 */
			return userAuthorities.call1(userId).get();
		}

		/**
		 * Tells the database that the user login worked. This is necessary
		 * because the DB can't perform the password check itself.
		 *
		 * @param userId
		 *            Who logged in?
		 */
		void noteLoginSuccessForUser(int userId) {
			loginSuccess.call(userId);
		}

		/**
		 * Tells the database that the user login failed. This is necessary
		 * because the DB can't perform the password check itself.
		 *
		 * @param userId
		 *            Who logged in?
		 * @param username
		 *            Who logged in? For the logging message when the account
		 *            gets a temporary lock applied.
		 */
		void noteLoginFailureForUser(int userId, String username) {
			if (loginFailure.call1(authProps.getMaxLoginFailures(), userId)
					.map(bool("locked")).orElse(false)) {
				log.warn("automatically locking user {} for {}", username,
						authProps.getAccountLockDuration());
			}
		}
	}

	private static String makeCollabAuthorityName(String collabClaimed) {
		return "COLLAB_" + collabClaimed;
	}

	private static String makeOrgAuthorityName(String collabClaimed) {
		return "ORG_" + collabClaimed;
	}

	class CollabratoryAuthority extends SimpleGrantedAuthority {
		private static final long serialVersionUID = 4964366746649162092L;

		private final String collabratory;

		CollabratoryAuthority(String collabClaimed) {
			super(makeCollabAuthorityName(collabClaimed));
			collabratory = collabClaimed;
		}

		String getCollabratory() {
			return collabratory;
		}
	}

	class OrganisationAuthority extends SimpleGrantedAuthority {
		private static final long serialVersionUID = 8260068770503054502L;

		private final String organisation;

		OrganisationAuthority(String orgClaimed) {
			super(makeOrgAuthorityName(orgClaimed));
			organisation = orgClaimed;
		}

		String getOrganisation() {
			return organisation;
		}
	}

	private boolean collabToAuthority(String source, List<String> claim,
			Collection<GrantedAuthority> results) {
		if (isNull(claim)) {
			return false;
		}
		for (String collab : claim) {
			log.info("CLAIMED MEMBERSHIP OF COLLAB from {}: {}", source,
					collab);
			results.add(new CollabratoryAuthority(collab));
		}
		return true;
	}

	private boolean orgToAuthority(String source, List<String> claim,
			Collection<GrantedAuthority> results) {
		if (isNull(claim)) {
			return false;
		}
		for (String org : claim) {
			log.info("CLAIMED MEMBERSHIP OF ORGANIZATION from {}: {}", source,
					org);
			results.add(new OrganisationAuthority(org));
		}
		return true;
	}

	private static final String USERINFO =
			"https://iam.ebrains.eu/auth/realms/hbp/protocol/"
					+ "openid-connect/userinfo";

	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;

	private void fetchRawUserInfo(OAuth2AccessToken token) {
		try {
			URLConnection conn = new URL(USERINFO).openConnection();
			conn.addRequestProperty("Authorization",
					"Bearer " + token.getTokenValue());
			try (Reader is = new InputStreamReader(conn.getInputStream())) {
				log.info("raw response: {}", IOUtils.toString(is));
			}
		} catch (Exception e) {
			log.error("failure to direct-fetch user info", e);
		}
	}

	@Override
	public void mapAuthorities(OidcUserAuthority user,
			Collection<GrantedAuthority> results) {
		Authentication auth =
				SecurityContextHolder.getContext().getAuthentication();
		// https://stackoverflow.com/a/62921030/301832
		OAuth2AuthorizedClient client = authorizedClientService
				.loadAuthorizedClient("spinnaker-spalloc", auth.getName());
		if (client == null) {
			log.warn("null client");
		} else {
			fetchRawUserInfo(client.getAccessToken());
		}
		if (!collabToAuthority("userInfo",
				user.getUserInfo().getClaimAsStringList("team"), results)
				// Note: not a shortcut AND; always call both sides
				& !collabToAuthority("idToken",
						user.getIdToken().getClaimAsStringList("team"),
						results)) {
			log.info("no team in authority");
		}
		if (!collabToAuthority("userInfo",
				user.getUserInfo().getClaimAsStringList("group"), results)
				// Note: not a shortcut AND; always call both sides
				& !collabToAuthority("idToken",
						user.getIdToken().getClaimAsStringList("group"),
						results)) {
			log.info("no group in authority");
		}
		if (!orgToAuthority("userInfo",
				user.getUserInfo().getClaimAsStringList("unit"), results)
				// Note: not a shortcut AND; always call both sides
				& !orgToAuthority("idToken",
						user.getIdToken().getClaimAsStringList("unit"),
						results)) {
			log.info("no unit in authority");
		}
		results.add(new SimpleGrantedAuthority(GRANT_READER));
		results.add(new SimpleGrantedAuthority(GRANT_USER));
	}

	@Override
	public void mapAuthorities(Jwt token,
			Collection<GrantedAuthority> results) {
		if (!collabToAuthority("token", token.getClaimAsStringList("team"),
				results)) {
			log.info("no team in token");
		}
		if (!orgToAuthority("token", token.getClaimAsStringList("unit"),
				results)) {
			log.info("no unit in token");
		}
		results.add(new SimpleGrantedAuthority(GRANT_READER));
		results.add(new SimpleGrantedAuthority(GRANT_USER));
	}

	private static final class LocalAuthResult {
		final int userId;

		final TrustLevel trustLevel;

		final String passInfo;

		/**
		 * Auth succeeded.
		 *
		 * @param u
		 *            The user ID
		 * @param t
		 *            The trust level.
		 * @param ep
		 *            The <em>encoded</em> password.
		 */
		LocalAuthResult(int u, TrustLevel t, String ep) {
			userId = u;
			trustLevel = requireNonNull(t);
			passInfo = requireNonNull(ep);
		}
	}

	/**
	 * Check if a user can log in, and determine what permissions they have.
	 *
	 * @param username
	 *            The username.
	 * @param password
	 *            The <em>unencrypted</em> password.
	 * @param authorities
	 *            Filled out with permissions.
	 * @param queries
	 *            How to access the database.
	 * @return Whether the user is known. If {@code false}, the database cannot
	 *         authenticate the user and some other auth method should be tried.
	 * @throws UsernameNotFoundException
	 *             If no account exists.
	 * @throws DisabledException
	 *             If the account is administratively disabled.
	 * @throws LockedException
	 *             If the account is temporarily locked.
	 * @throws BadCredentialsException
	 *             If the password is invalid.
	 */
	private boolean authLocalAgainstDB(String username, String password,
			List<GrantedAuthority> authorities, AuthQueries queries) {
		Optional<LocalAuthResult> lookup =
				queries.transaction(() -> lookUpUserDetails(username, queries));
		return lookup.map(details -> {
			checkPassword(username, password, details, queries);
			// Succeeded; finalize into external form
			return queries.transaction(() -> {
				queries.noteLoginSuccessForUser(details.userId);
				// Convert tiered trust level to grant form
				details.trustLevel.getGrants().forEach(authorities::add);
				log.info("login success for {} at level {}", username,
						details.trustLevel);
				return true;
			});
		}).orElse(false);
	}

	/**
	 * Look up a local user. Does all checks <em>except</em> the password check;
	 * that's slow (because bcrypt) so we do it outside the transaction.
	 *
	 * @param username
	 *            The user name we're looking up.
	 * @param queries
	 *            How to access the database.
	 * @return Whether we know the user, and if so, what do we know about them.
	 * @throws AuthenticationException
	 *             If the user is known and definitely can't log in for some
	 *             reason.
	 */
	private static Optional<LocalAuthResult> lookUpUserDetails(String username,
			AuthQueries queries) {
		Optional<Row> r = queries.getUser(username);
		if (!r.isPresent()) {
			// No such user
			return Optional.empty();
		}
		Row userInfo = r.get();
		int userId = userInfo.getInt("user_id");
		if (userInfo.getBoolean("disabled")) {
			log.info("login failure for {}: account is disabled", username);
			throw new DisabledException("account is disabled");
		}
		try {
			if (userInfo.getBoolean("locked")) {
				// Note that this extends the lock!
				throw new LockedException("account is locked");
			}
			Row authInfo = queries.getUserAuthorities(userId);
			String encPass = authInfo.getString("encrypted_password");
			if (isNull(encPass)) {
				/*
				 * We know this user, but they can't use this authentication
				 * method. They'll probably have to use OpenID.
				 */
				return Optional.empty();
			}
			TrustLevel trust =
					authInfo.getEnum("trust_level", TrustLevel.class);
			return Optional.of(new LocalAuthResult(userId, trust, encPass));
		} catch (AuthenticationException e) {
			queries.noteLoginFailureForUser(userId, username);
			log.info("login failure for {}", username, e);
			throw e;
		}
	}

	/**
	 * This is slow (100ms!) so we make sure we aren't holding a transaction
	 * open while this step is ongoing.
	 *
	 * @param username
	 *            The username (now validated as existing).
	 * @param password
	 *            The user-provided password that we're checking.
	 * @param queries
	 *            How to access the DB.
	 * @param details
	 *            The results of looking up the user
	 * @return
	 */
	private LocalAuthResult checkPassword(String username, String password,
			LocalAuthResult details, AuthQueries queries) {
		if (!passServices.matchPassword(password, details.passInfo)) {
			queries.transaction(() -> {
				queries.noteLoginFailureForUser(details.userId, username);
				log.info("login failure for {}: bad password", username);
				throw new BadCredentialsException("bad password");
			});
		}
		return details;
	}

	/**
	 * Check if an OpenID user can use the service. A transaction <em>must</em>
	 * be held open when calling this.
	 *
	 * @param username
	 *            The username, already obtained and verified.
	 * @param authorities
	 *            The claimed authorities derived from the HBP OpenID service.
	 *            Interesting because they include claims about organisation and
	 *            collabratory membership.
	 * @param queries
	 *            How to access the database.
	 * @return Whether the user is allowed to use the service. If {@code false},
	 *         the database cannot authorize the user and some other auth method
	 *         should be tried.
	 * @throws DisabledException
	 *             If the account is administratively disabled.
	 * @throws LockedException
	 *             If the account is temporarily locked.
	 */
	private boolean authOpenIDAgainstDB(String username,
			Collection<GrantedAuthority> authorities, AuthQueries queries) {
		List<String> collabs = new ArrayList<>();
		List<String> orgs = new ArrayList<>();
		authorities.forEach(ga -> {
			if (ga instanceof CollabratoryAuthority) {
				CollabratoryAuthority collab = (CollabratoryAuthority) ga;
				inflateCollabratoryGroup(collab.getCollabratory(), queries);
				collabs.add(collab.getCollabratory());
			} else if (ga instanceof OrganisationAuthority) {
				OrganisationAuthority org = (OrganisationAuthority) ga;
				inflateOrganisationGroup(org.getOrganisation(), queries);
				orgs.add(org.getOrganisation());
			}
		});
		Optional<Row> r = queries.getUser(username);
		if (!r.isPresent()) {
			/*
			 * No such user; need to inflate one now. If we successfully make
			 * the user, they're also immediately authorised.
			 */
			Optional<Integer> createdUser = createUser(username, null, USER,
					quotaProps.getDefaultQuota(), queries.createUser,
					queries.addQuota);
			createdUser.ifPresent(
					id -> synchOrgsAndCollabs(id, orgs, collabs, queries));
			return createdUser.isPresent();
		}
		Row userInfo = r.get();

		int userId = userInfo.getInt("user_id");
		synchOrgsAndCollabs(userId, orgs, collabs, queries);
		if (userInfo.getBoolean("disabled")) {
			throw new DisabledException("account is disabled");
		}
		try {
			if (userInfo.getBoolean("locked")) {
				// Note that this extends the lock!
				throw new LockedException("account is locked");
			}
			Row authInfo = queries.getUserAuthorities(userId);
			if (nonNull(authInfo.getString("encrypted_password"))) {
				/*
				 * We know this user, but they can't use this authentication
				 * method. They'll probably have to use username+password.
				 */
				return false;
			}
			queries.noteLoginSuccessForUser(userId);
			log.info("login success for {}", username);
			return true;
		} catch (AuthenticationException e) {
			queries.noteLoginFailureForUser(userId, username);
			log.info("login failure for {}", username, e);
			throw e;
		}
	}

	private void inflateCollabratoryGroup(String collab, AuthQueries queries) {
		// TODO create a collab (with default props) if it doesn't exist
		log.info("would create collabratory '{}' here", collab);
	}

	private void inflateOrganisationGroup(String org, AuthQueries queries) {
		// TODO create an org (with default props) if it doesn't exist
		log.info("would create organisation '{}' here", org);
	}

	private void synchOrgsAndCollabs(int userId, List<String> orgs,
			List<String> collabs, AuthQueries queries) {
		// TODO make this user's orgs and collabs be exactly these
		log.info("would set user {} to have orgs = {} and collabs = {}", userId,
				orgs, collabs);
	}

	@Override
	@Scheduled(fixedDelayString = "#{authProperties.unlockPeriod}")
	public void unlockLockedUsers() {
		try {
			if (!control.isPaused()) {
				log.debug("running user unlock task");
				unlock();
			}
		} catch (DataAccessException e) {
			if (isBusy(e)) {
				log.info("database is busy; "
						+ "will try user unlock processing later");
				return;
			}
			throw e;
		}
	}

	void unlock() {
		try (Connection conn = getConnection();
				Query unlock = conn.query(UNLOCK_LOCKED_USERS)) {
			conn.transaction(() -> {
				unlock.call(authProps.getAccountLockDuration())
						.map(string("user_name")).forEach(user -> log
								.info("automatically unlocked user {}", user));
			});
		}
	}
}
