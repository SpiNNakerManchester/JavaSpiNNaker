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

import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.GRANT_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.GRANT_READER;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.GRANT_USER;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_ADMIN;

import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Transacted;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.LocalAuthenticationProvider;
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
	private PasswordEncoder passwordEncoder;

	@Value("${spalloc.addDummyUser:true}")
	private boolean addDummyUser;

	/**
	 * Trivial default allocation quota. 100 board-seconds is next to nothing.
	 */
	@Value("${spalloc.defaultQuota:100}")
	private long defaultQuota;

	/** Maximum number of login failures for account to get a lock. */
	@Value("${spalloc.maxLoginFailures:3}")
	private int maxLoginFailures;

	@Value("${spalloc.accountLockDuration:PT24H}")
	private Duration lockInterval;

	private static final String DUMMY_USER = "user1";

	private static final String DUMMY_PASSWORD = "user1Pass";

	/** Run the unlock task every minute. */
	private static final long INTER_UNLOCK_DELAY = 60000;

	@PostConstruct
	private void initUserIfNecessary() throws SQLException {
		if (addDummyUser) {
			createUser(DUMMY_USER, DUMMY_PASSWORD, TrustLevel.ADMIN,
					defaultQuota);
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
				Update addUser = update(conn, ADD_USER_WITH_DEFAULTS);
				Update addQuota = update(conn, ADD_QUOTA_FOR_ALL_MACHINES)) {
			return transaction(conn, () -> {
				for (int userId : addUser.keys(username,
						passwordEncoder.encode(password), trustLevel)) {
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

	/**
	 * Describes basic information about a user that they'd use to change their
	 * password.
	 *
	 * @author Donal Fellows
	 */
	public static class User {
		private final int userId;

		private final String username;

		private String oldPassword;

		private String newPassword;

		private String newPassword2;

		User(int userId, String username) {
			this.userId = userId;
			this.username = username;
			this.oldPassword = "";
			this.newPassword = "";
			this.newPassword2 = "";
		}

		/**
		 * @return the user id
		 */
		public final int getUserId() {
			return userId;
		}

		/**
		 * @return the username
		 */
		@NotBlank(message = "username must be set")
		public final String getUsername() {
			return username;
		}

		/**
		 * @return the old password
		 */
		@NotBlank(message = "old password must be supplied")
		public String getOldPassword() {
			return oldPassword;
		}

		public void setOldPassword(String password) {
			this.oldPassword = password;
		}

		/**
		 * @return the first copy of the new password
		 */
		@NotBlank(message = "new password must be supplied")
		public String getNewPassword() {
			return newPassword;
		}

		public void setNewPassword(String newPassword) {
			this.newPassword = newPassword;
		}

		/**
		 * @return the second copy of the new password
		 */
		@NotBlank(message = "second copy of new password must be supplied")
		public String getNewPassword2() {
			return newPassword2;
		}

		public void setNewPassword2(String newPassword2) {
			this.newPassword2 = newPassword2;
		}

		@AssertFalse(message = "old and new passwords must be different")
		boolean isNewPasswordSameAsOld() {
			return newPassword.equals(oldPassword);
		}

		@AssertTrue(
				message = "second copy of new password must be same as first")
		boolean isNewPasswordMatched() {
			return newPassword.equals(newPassword2);
		}
	}

	@Override
	public User getUserForPrincipal(Principal principal)
			throws AuthenticationException {
		try (Connection c = db.getConnection();
				Query q = query(c, "SELECT user_id, user_name FROM user_info "
						+ "WHERE user_name = :username "
						+ "AND encrypted_password IS NOT NULL LIMIT 1")) {
			Row row = q.call1(principal.getName()).orElseThrow(
					// OpenID-authenticated user; go away
					() -> new AuthenticationServiceException(
							"user is managed externally; "
									+ "cannot manage password here"));
			return new User(row.getInt("user_id"), row.getString("user_name"));
		} catch (SQLException e) {
			throw new InternalAuthenticationServiceException(
					"database access problem", e);
		}
	}

	@Override
	public User updateUserOfPrincipal(Principal principal, User user) {
		try (Connection c = db.getConnection();
				Query q = query(c,
						"SELECT user_id, user_name, encrypted_password "
								+ "FROM user_info "
								+ "WHERE user_id = :user_id "
								+ "AND user_name = :user_name "
								+ "AND encrypted_password IS NOT NULL LIMIT 1");
				Update u = update(c,
						"UPDATE user_info SET "
								+ "encrypted_password = :encrypted_password "
								+ "WHERE user_id = :user_id")) {
			return DatabaseEngine.transaction(c, () -> {
				Row row = q.call1(user.userId, principal.getName()).orElseThrow(
						// OpenID-authenticated user; go away
						() -> new AuthenticationServiceException(
								"user is managed externally; "
										+ "cannot manage password here"));
				User baseUser = new User(row.getInt("user_id"),
						row.getString("user_name"));
				if (!passwordEncoder.matches(user.oldPassword,
						row.getString("password"))) {
					throw new BadCredentialsException("bad password");
				}
				if (!user.newPassword.equals(user.newPassword2)) {
					throw new BadCredentialsException("bad password");
				}
				if (u.call(passwordEncoder.encode(user.newPassword),
						row.getInt("user_id")) != 1) {
					throw new InternalAuthenticationServiceException(
							"failed to update database");
				}
				return baseUser;
			});
		} catch (SQLException e) {
			throw new InternalAuthenticationServiceException(
					"database access problem", e);
		}
	}

	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		log.info("authenticating {}", authentication.toString());
		// We ALWAYS trim the username; extraneous whitespace is bogus
		String name = authentication.getName().trim();
		if (name.isEmpty()) {
			// Won't touch the DB if the username is empty
			throw new UsernameNotFoundException("empty user name?");
		}
		String password = authentication.getCredentials().toString();
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

	/**
	 * Database connection and queries used for authentication and authorization
	 * purposes.
	 */
	private final class AuthQueries implements AutoCloseable {
		private final Connection conn;

		private final Query getUserBlocked;

		private final Query userAuthorities;

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
			loginSuccess = update(conn, MARK_LOGIN_SUCCESS);
			loginFailure = query(conn, MARK_LOGIN_FAILURE);
		}

		void transact(Transacted code) throws SQLException {
			transaction(conn, code);
		}

		@Override
		public void close() throws SQLException {
			loginFailure.close();
			loginSuccess.close();
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
		 * @throws AuthenticationException
		 *             If there is no such user.
		 * @throws SQLException
		 *             If DB access fails.
		 */
		Row getUser(String username)
				throws AuthenticationException, SQLException {
			return getUserBlocked.call1(username)
					.orElseThrow(() -> new UsernameNotFoundException(
							"no such user: " + username));
		}

		/**
		 * Get the extended auth-related user data.
		 *
		 * @param userId
		 *            Who are we fetching for?
		 * @return The DB row describing them. Includes {@code password} and
		 *         {@code trust_level}.
		 * @throws AuthenticationException
		 *             If there is no such user.
		 * @throws SQLException
		 *             If DB access fails.
		 */
		Row getUserAuthorities(int userId)
				throws AuthenticationException, SQLException {
			/*
			 * I believe the BadCredentialsException can never be thrown; caller
			 * checks the password, we just got the userId from the DB, and
			 * we're in a transaction so the world won't change under our feet.
			 */
			return userAuthorities.call1(userId).orElseThrow(
					() -> new BadCredentialsException("bad password"));
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
	private void authenticateAgainstDB(String username, String password,
			List<GrantedAuthority> authorities,
			LocalAuthProviderImpl.AuthQueries queries) throws SQLException {
		Row userInfo = queries.getUser(username);
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
			if (!passwordEncoder.matches(password,
					authInfo.getString("password"))) {
				throw new BadCredentialsException("bad password");
			}
			TrustLevel trust =
					authInfo.getEnum("trust_level", TrustLevel.class);
			switch (trust) {
			case ADMIN:
				authorities.add(() -> GRANT_ADMIN);
				// fallthrough
			case USER:
				authorities.add(() -> GRANT_USER);
				// fallthrough
			case READER:
				authorities.add(() -> GRANT_READER);
				// fallthrough
			default:
				// Do nothing; no grants of authority made
			}
			queries.noteLoginSuccessForUser(userId);
			log.info("login success for {} at level {}", username, trust);
		} catch (AuthenticationException e) {
			queries.noteLoginFailureForUser(userId, username);
			log.info("login failure for {}", username, e);
			throw e;
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}

	@Scheduled(fixedDelay = INTER_UNLOCK_DELAY)
	void unlockLockedUsers() throws SQLException {
		log.debug("running user unlock task");
		try (Connection conn = db.getConnection();
				Query unlock = query(conn, UNLOCK_LOCKED_USERS)) {
			for (Row row : unlock.call(lockInterval)) {
				log.info("automatically unlocked user {}",
						row.getString("user_name"));
			}
		}
	}
}
