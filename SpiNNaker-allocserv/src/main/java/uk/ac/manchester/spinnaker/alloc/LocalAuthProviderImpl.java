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

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.GRANT_READER;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.GRANT_USER;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel.ADMIN;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel.USER;
import static uk.ac.manchester.spinnaker.alloc.db.Row.bool;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.isBusy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.LocalAuthenticationProvider;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.PasswordServices;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.SimpleGrantedAuthority;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
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

	private AuthenticationProvider tokenProvider;

	private AuthenticationProvider loginProvider;

	@PostConstruct
	void initOpenIdAuthProviders() {
		DefaultOAuth2UserService userService = new DefaultOAuth2UserService();
		DefaultAuthorizationCodeTokenResponseClient client =
				new DefaultAuthorizationCodeTokenResponseClient();
		tokenProvider =
				new OAuth2AuthorizationCodeAuthenticationProvider(client);
		loginProvider =
				new OAuth2LoginAuthenticationProvider(client, userService);
	}

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
					trustLevel, quota, createUser, addQuota));
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
	 * @return Whether the user was successfully created.
	 */
	private Boolean createUser(String username, String encPass,
			TrustLevel trustLevel, long quota, Update createUser,
			Update addQuota) {
		return createUser.key(username, encPass, trustLevel, false)
				.map(userId -> {
					addQuota.call(userId, quota);
					log.info(
							"added user {} with trust level {} and "
									+ "quota {} board-seconds",
							username, trustLevel, quota);
					return userId;
				}).isPresent();
	}

	/** Marks an authentication that we've already taken a decision about. */
	private interface AlreadyDoneMarker extends Authentication {
	}

	@Override
	public Authentication authenticate(Authentication auth)
			throws AuthenticationException {
		log.info("requesting auth decision about {}", auth);
		if (auth instanceof AlreadyDoneMarker) {
			// It's ours; don't repeat the auth
			return auth;
		}

		try {
			if (auth instanceof UsernamePasswordAuthenticationToken) {
				return authenticateDirect(
						(UsernamePasswordAuthenticationToken) auth);
			} else if (auth instanceof OAuth2LoginAuthenticationToken) {
				return authenticateOpenId(
						(OAuth2LoginAuthenticationToken) auth);
			} else if (auth//
					instanceof OAuth2AuthorizationCodeAuthenticationToken) {
				return authenticateOpenId(
						(OAuth2AuthorizationCodeAuthenticationToken) auth);
			} else if (auth instanceof OAuth2AuthenticationToken) {
				return authenticateOpenId((OAuth2AuthenticationToken) auth);
			} else {
				return null;
			}
		} catch (DataAccessException e) {
			throw new InternalAuthenticationServiceException(
					"database access problem", e);
		}
	}

	private static final Class<?>[] SUPPORTED_CLASSES = {
		UsernamePasswordAuthenticationToken.class,
		OAuth2AuthorizationCodeAuthenticationToken.class,
		OAuth2LoginAuthenticationToken.class,
		OAuth2AuthenticationToken.class,
		OpenIDDerivedAuthenticationToken.class
	};

	@Override
	public final boolean supports(Class<?> cls) {
		for (Class<?> c : SUPPORTED_CLASSES) {
			if (c.isAssignableFrom(cls)) {
				return true;
			}
		}
		log.info("asked about supporting {}", cls);
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

	private Authentication
			authenticateOpenId(OAuth2LoginAuthenticationToken auth) {
		log.info("authenticating OpenID Login {}", auth);
		OAuth2LoginAuthenticationToken authToken =
				(OAuth2LoginAuthenticationToken) loginProvider
						.authenticate(auth);
		if (isNull(authToken)) {
			return null;
		}
		log.debug("got principal: {}", authToken.getPrincipal());
		return authorizeOpenId(authToken.getPrincipal());
	}

	private Authentication authenticateOpenId(
			OAuth2AuthorizationCodeAuthenticationToken auth) {
		// TODO is this code ever called?
		log.info("authenticating OpenID Token {}", auth);
		OAuth2AuthorizationCodeAuthenticationToken authToken =
				(OAuth2AuthorizationCodeAuthenticationToken) tokenProvider
						.authenticate(auth);
		if (isNull(authToken)) {
			return null;
		}
		log.info("got principal {} with parameters {}",
				authToken.getPrincipal(), authToken.getAdditionalParameters());
		// FIXME how to get username from auth code token?
		authToken.getAdditionalParameters().get("preferred_username");
		return authorizeOpenId(authProps.getOpenid().getUsernamePrefix()
				+ authToken.getPrincipal(), null);
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
		log.info("authenticating OpenID Login {}", auth);
		log.debug("got principal: {}", auth.getPrincipal());
		return authorizeOpenId(auth.getPrincipal());
	}

	private OpenIDDerivedAuthenticationToken authorizeOpenId(OAuth2User user) {
		return authorizeOpenId(authProps.getOpenid().getUsernamePrefix()
				+ user.getAttribute("preferred_username"), user);
	}

	private OpenIDDerivedAuthenticationToken authorizeOpenId(String name,
			OAuth2User user) {
		if (isNull(name)
				|| name.equals(authProps.getOpenid().getUsernamePrefix())) {
			// No actual name there?
			log.warn("failed to handle OpenID user with no real user name");
			return null;
		}
		try (AuthQueries queries = new AuthQueries()) {
			if (!queries.transaction(//
					() -> authOpenIDAgainstDB(name, queries))) {
				return null;
			}
		}
		// Users from OpenID always have the same permissions
		return new OpenIDDerivedAuthenticationToken(name, user);
	}

	/**
	 * An object that can say something about what user it was derived from.
	 */
	public interface OpenIDUserAware {
		/**
		 * Get the underlying OpenID user information.
		 *
		 * @return The user info, if known.
		 */
		Optional<OAuth2User> getOpenIdUser();
	}

	private static final class OpenIDDerivedAuthenticationToken
			extends AbstractAuthenticationToken
			implements OpenIDUserAware, AlreadyDoneMarker {
		private static final long serialVersionUID = 970898019896708267L;

		private final String who;

		private final OAuth2User user;

		private OpenIDDerivedAuthenticationToken(String who, OAuth2User user) {
			super(asList(new SimpleGrantedAuthority(GRANT_READER),
					new SimpleGrantedAuthority(GRANT_USER)));
			this.who = who;
			this.user = user;
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

		@Override
		public Optional<OAuth2User> getOpenIdUser() {
			return Optional.ofNullable(user);
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
	 * Check if an OpenID user can use the service.
	 *
	 * @param username
	 *            The username, already obtained and verified.
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
	private boolean authOpenIDAgainstDB(String username, AuthQueries queries) {
		Optional<Row> r = queries.getUser(username);
		if (!r.isPresent()) {
			/*
			 * No such user; need to inflate one now. If we successfully make
			 * the user, they're also immediately authorised.
			 */
			return createUser(username, null, USER,
					quotaProps.getDefaultQuota(), queries.createUser,
					queries.addQuota);
		}
		Row userInfo = r.get();

		int userId = userInfo.getInt("user_id");
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
