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
import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteErrorCode.SQLITE_BUSY;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.GRANT_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.GRANT_READER;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.GRANT_USER;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel.USER;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.stereotype.Component;
import org.sqlite.SQLiteException;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.TransactedWithResult;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.LocalAuthenticationProvider;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.SimpleGrantedAuthority;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;

/**
 * Does authentication against users defined entirely in the database. This
 * includes keeping the users' (encrypted) password in the database. This is
 * primarily focused on the {@code user_info} database table.
 * <p>
 * Key configuration properties:
 * <ul>
 * <li>{@code spalloc.addDummyUser} &mdash; Turn off in production!
 * <li>{@code spalloc.defaultQuota} &mdash; Default number of board-seconds to
 * allocate.
 * <li>{@code spalloc.maxLoginFailures} &mdash; How many failures before account
 * lock-out?
 * <li>{@code spalloc.accountLockDuration} &mdash; Length of account lock-out.
 * </ul>
 *
 * @author Donal Fellows
 */
@Component
public class LocalAuthProviderImpl extends SQLQueries
		implements LocalAuthenticationProvider {
	private static final Logger log = getLogger(LocalAuthProviderImpl.class);

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private ServiceMasterControl control;

	@Value("${spalloc.auth.add-dummy-user:true}")
	private boolean addDummyUser;

	@Value("${spalloc.auth.dummy-random-pass:false}")
	private boolean dummyRandomPass;

	/**
	 * Trivial default allocation quota. 100 board-seconds is next to nothing.
	 */
	@Value("${spalloc.quota.default:100}")
	private long defaultQuota;

	/** Maximum number of login failures for account to get a lock. */
	@Value("${spalloc.auth.max-login-failures:3}")
	private int maxLoginFailures;

	@Value("${spalloc.auth.account-lock-duration:PT24H}")
	private Duration lockInterval;

	private static final String DUMMY_USER = "user1";

	private static final String DUMMY_PASSWORD = "user1Pass";

	private Random rng = null;

	private static final int PASSWORD_LENGTH = 16;

	/**
	 * Generate a random password.
	 *
	 * @return A password consisting of 16 random ASCII printable characters.
	 */
	private String generatePassword() {
		synchronized (this) {
			if (rng == null) {
				rng = new SecureRandom();
			}
		}
		StringBuilder sb = new StringBuilder();
		rng.ints(PASSWORD_LENGTH, '\u0021', '\u007f')
				.forEachOrdered(c -> sb.append((char) c));
		return sb.toString();
	}

	@PostConstruct
	private void initUserIfNecessary() throws SQLException {
		if (addDummyUser) {
			String pass = DUMMY_PASSWORD;
			if (dummyRandomPass) {
				pass = generatePassword();
			}
			if (createUser(DUMMY_USER, pass, TrustLevel.ADMIN, defaultQuota)) {
				if (dummyRandomPass) {
					log.info("admin user {} has password: {}", DUMMY_USER,
							pass);
				}
			}
		}
	}

	@Override
	@PreAuthorize(IS_ADMIN)
	public boolean createUser(String username, String password,
			TrustLevel trustLevel, long quota) throws SQLException {
		String name = username.trim();
		if (name.isEmpty()) {
			// Won't touch the DB if the username is empty
			throw new UsernameNotFoundException("empty user name?");
		}
		try (Connection conn = db.getConnection();
				Update createUser = update(conn, CREATE_USER);
				Update addQuota = update(conn, ADD_QUOTA_FOR_ALL_MACHINES)) {
			return transaction(conn, () -> {
				for (int userId : createUser.keys(username, password,
						trustLevel, false)) {
					addQuota.call(userId, quota);
					log.info(
							"added user {} with trust level {} "
									+ "and quota {} board-seconds",
							username, trustLevel, quota);
					return true;
				}
				return false;
			});
		}
	}

	@Override
	public Authentication authenticate(Authentication auth)
			throws AuthenticationException {
		if (auth instanceof UsernamePasswordAuthenticationToken) {
			return authenticateDirect(
					(UsernamePasswordAuthenticationToken) auth);
		} else if (auth instanceof OAuth2LoginAuthenticationToken) {
			return authenticateOpenId((OAuth2LoginAuthenticationToken) auth);
		} else {
			return authenticateOpenId(
					(OAuth2AuthorizationCodeAuthenticationToken) auth);
		}
	}

	@Override
	public boolean supports(Class<?> cls) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(cls)
				|| OAuth2AuthorizationCodeAuthenticationToken.class
						.isAssignableFrom(cls)
				|| OAuth2LoginAuthenticationToken.class.isAssignableFrom(cls);
	}

	private UsernamePasswordAuthenticationToken
			authenticateDirect(UsernamePasswordAuthenticationToken auth)
					throws AuthenticationException {
		log.info("authenticating {}", auth.toString());
		// We ALWAYS trim the username; extraneous whitespace is bogus
		String name = auth.getName().trim();
		if (name.isEmpty()) {
			// Won't touch the DB if the username is empty
			throw new UsernameNotFoundException("empty user name?");
		}
		String password = auth.getCredentials().toString();
		List<GrantedAuthority> authorities = new ArrayList<>();

		try {
			try (AuthQueries queries = new AuthQueries()) {
				queries.transact(() -> authenticateAgainstDB(name, password,
						authorities, queries));
			}
			return new UsernamePasswordAuthenticationToken(name, password,
					authorities);
		} catch (SQLException e) {
			throw new InternalAuthenticationServiceException(
					"database access problem", e);
		}
	}

	private Authentication
			authenticateOpenId(OAuth2LoginAuthenticationToken auth)
					throws AuthenticationException {
		// FIXME how to get username from login token?
		return authenticateOpenId(auth.getPrincipal()
				.getAttribute("preferred_username").toString());
	}

	private Authentication
			authenticateOpenId(OAuth2AuthorizationCodeAuthenticationToken auth)
					throws AuthenticationException {
		// FIXME how to get username from auth code token?
		return authenticateOpenId(auth.getPrincipal().toString());
	}

	private AuthenticationToken authenticateOpenId(String name) {
		String prefixedName = "openid." + name;
		try (AuthQueries queries = new AuthQueries()) {
			queries.transact(
					() -> authorizeOpenIDAgainstDB(prefixedName, queries));
			return new AuthenticationToken(prefixedName);
		} catch (SQLException e) {
			throw new InternalAuthenticationServiceException(
					"database access problem", e);
		}
	}

	private static final class AuthenticationToken
			extends AbstractAuthenticationToken {
		private static final long serialVersionUID = 1L;

		private final String who;

		private AuthenticationToken(String who) {
			super(asList(new SimpleGrantedAuthority(GRANT_USER)));
			this.who = who;
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
	}

	/**
	 * Database connection and queries used for authentication and authorization
	 * purposes.
	 */
	private final class AuthQueries implements AutoCloseable {
		private final Connection conn;

		private final Query getUserBlocked;

		private final Query userAuthorities;

		private final Query isUserPassMatched;

		private final Update loginSuccess;

		private final Query loginFailure;

		/**
		 * Make an instance.
		 *
		 * @throws SQLException
		 *             If DB access fails (e.g., due to schema mismatches).
		 */
		AuthQueries() throws SQLException {
			conn = db.getConnection();
			getUserBlocked = query(conn, IS_USER_LOCKED);
			userAuthorities = query(conn, GET_USER_AUTHORITIES);
			isUserPassMatched = query(conn, IS_USER_PASS_MATCHED);
			loginSuccess = update(conn, MARK_LOGIN_SUCCESS);
			loginFailure = query(conn, MARK_LOGIN_FAILURE);
		}

		<T> T transact(TransactedWithResult<T> code) throws SQLException {
			return transaction(conn, code);
		}

		@Override
		public void close() throws SQLException {
			loginFailure.close();
			loginSuccess.close();
			isUserPassMatched.close();
			userAuthorities.close();
			getUserBlocked.close();
			conn.close();
		}

		/**
		 * Get a description of basic auth-related user data.
		 *
		 * @param username
		 *            Who are we fetching for?
		 * @return The DB row describing them. Includes {@code user_id},
		 *         {@code disabled} and {@code locked}.
		 * @throws SQLException
		 *             If DB access fails.
		 */
		Optional<Row> getUser(String username) throws SQLException {
			return getUserBlocked.call1(username);
		}

		/**
		 * Get the extended auth-related user data.
		 *
		 * @param userId
		 *            Who are we fetching for?
		 * @return The DB row describing them. Includes {@code has_password} and
		 *         {@code trust_level}.
		 * @throws SQLException
		 *             If DB access fails.
		 */
		Row getUserAuthorities(int userId) throws SQLException {
			/*
			 * I believe the NoSuchElementException can never be thrown; caller
			 * checks the password, we just got the userId from the DB, and
			 * we're in a transaction so the world won't change under our feet.
			 */
			return userAuthorities.call1(userId).get();
		}

		boolean isUserPassMatched(int userId, String password)
				throws SQLException {
			return isUserPassMatched.call1(password, userId)
					.orElseThrow(
							() -> new BadCredentialsException("bad password"))
					.getBoolean("matches");
		}

		/**
		 * Tells the database that the user login worked. This is necessary
		 * because the DB can't perform the password check itself.
		 *
		 * @param userId
		 *            Who logged in?
		 * @throws SQLException
		 *             If DB access fails.
		 */
		void noteLoginSuccessForUser(int userId) throws SQLException {
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
		 * @throws SQLException
		 *             If DB access fails.
		 */
		void noteLoginFailureForUser(int userId, String username)
				throws SQLException {
			for (Row row : loginFailure.call(maxLoginFailures, userId)) {
				if (row.getBoolean("locked")) {
					log.warn("automatically locking user {} for {}", username,
							lockInterval);
				}
			}
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
	 * @throws SQLException
	 *             If DB access fails.
	 * @throws UsernameNotFoundException
	 *             If no account exists.
	 * @throws DisabledException
	 *             If the account is administratively disabled.
	 * @throws LockedException
	 *             If the account is temporarily locked.
	 * @throws BadCredentialsException
	 *             If the password is invalid.
	 */
	private boolean authenticateAgainstDB(String username, String password,
			List<GrantedAuthority> authorities,
			LocalAuthProviderImpl.AuthQueries queries) throws SQLException {
		Optional<Row> r = queries.getUser(username);
		if (!r.isPresent()) {
			// No such user
			return false;
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
			if (!authInfo.getBoolean("has_password")) {
				/*
				 * We know this user, but they can't use this authentication
				 * method. They'll probably have to use OIDC.
				 */
				return false;
			}
			if (!queries.isUserPassMatched(userId, password)) {
				throw new BadCredentialsException("bad password");
			}
			TrustLevel trust =
					authInfo.getEnum("trust_level", TrustLevel.class);
			switch (trust) {
			case ADMIN:
				authorities.add(new SimpleGrantedAuthority(GRANT_ADMIN));
				// fallthrough
			case USER:
				authorities.add(new SimpleGrantedAuthority(GRANT_USER));
				// fallthrough
			case READER:
				authorities.add(new SimpleGrantedAuthority(GRANT_READER));
				// fallthrough
			default:
				// Do nothing; no grants of authority made
			}
			queries.noteLoginSuccessForUser(userId);
			log.info("login success for {} at level {}", username, trust);
			return true;
		} catch (AuthenticationException e) {
			queries.noteLoginFailureForUser(userId, username);
			log.info("login failure for {}", username, e);
			throw e;
		}
	}

	private boolean authorizeOpenIDAgainstDB(String username,
			LocalAuthProviderImpl.AuthQueries queries) throws SQLException {
		Optional<Row> r = queries.getUser(username);
		if (!r.isPresent()) {
			// No such user; need to inflate one
			createUser(username, null, USER, defaultQuota);
			// Must exist and be allowed; just made it!
			return true;
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
			if (authInfo.getBoolean("has_password")) {
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
	@Scheduled(fixedDelayString = "${spalloc.auth.unlock-period:PT60S}")
	public void unlockLockedUsers() throws SQLException {
		try {
			if (!control.isPaused()) {
				log.debug("running user unlock task");
				unlock();
			}
		} catch (SQLiteException e) {
			if (e.getResultCode().equals(SQLITE_BUSY)) {
				log.info("database is busy; "
						+ "will try user unlock processing later");
				return;
			}
			throw e;
		}
	}

	void unlock() throws SQLException {
		try (Connection conn = db.getConnection();
				Query unlock = query(conn, UNLOCK_LOCKED_USERS)) {
			for (Row row : unlock.call(lockInterval)) {
				log.info("automatically unlocked user {}",
						row.getString("user_name"));
			}
		}
	}
}
