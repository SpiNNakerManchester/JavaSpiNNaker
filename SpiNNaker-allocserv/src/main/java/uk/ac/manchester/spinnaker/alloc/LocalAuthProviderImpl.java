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
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_ADMIN;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
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
			createUser(DUMMY_USER, DUMMY_PASSWORD, TrustLevel.USER,
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

	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		// We ALWAYS trim the username; extraneous whitespace is bogus
		String name = authentication.getName().trim();
		if (name.isEmpty()) {
			// Won't touch the DB if the username is empty
			throw new UsernameNotFoundException("empty user name?");
		}
		String password = authentication.getCredentials().toString();
		List<GrantedAuthority> authorities = new ArrayList<>();

		try {
			authenticateAgainstDB(name, password, authorities);
			return new UsernamePasswordAuthenticationToken(name, password,
					authorities);
		} catch (SQLException e) {
			throw new InternalAuthenticationServiceException(
					"database access problem", e);
		}
	}

	private static class AuthQueries implements AutoCloseable {
		final Query getUserBlocked;

		final Query userAuthorities;

		final Update loginSuccess;

		final Query loginFailure;

		AuthQueries(Connection conn) throws SQLException {
			getUserBlocked = query(conn, IS_USER_LOCKED);
			userAuthorities = query(conn, GET_USER_AUTHORITIES);
			loginSuccess = update(conn, MARK_LOGIN_SUCCESS);
			loginFailure = query(conn, MARK_LOGIN_FAILURE);
		}

		@Override
		public void close() throws SQLException {
			loginFailure.close();
			loginSuccess.close();
			userAuthorities.close();
			getUserBlocked.close();
		}
	}

	private void authenticateAgainstDB(String name, String password,
			List<GrantedAuthority> authorities)
			throws SQLException, AuthenticationException {
		try (Connection conn = db.getConnection();
				LocalAuthProviderImpl.AuthQueries queries =
						new AuthQueries(conn)) {
			transaction(conn, () -> authenticateAgainstDB(name, password,
					authorities, queries));
		}
	}

	private void authenticateAgainstDB(String name, String password,
			List<GrantedAuthority> authorities,
			LocalAuthProviderImpl.AuthQueries queries) throws SQLException {
		Row userInfo = queries.getUserBlocked.call1(name).orElseThrow(
				() -> new UsernameNotFoundException("no such user: " + name));
		int userId = userInfo.getInt("user_id");
		if (userInfo.getBoolean("disabled")) {
			throw new DisabledException("account is disabled");
		}
		try {
			if (userInfo.getBoolean("locked")) {
				// Note that this extends the lock!
				throw new LockedException("account is locked");
			}
			Row authInfo = queries.userAuthorities.call1(userId).orElseThrow(
					// TODO is this the right exception?
					() -> new BadCredentialsException("bad password"));
			if (!passwordEncoder.matches(password,
					authInfo.getString("password"))) {
				throw new BadCredentialsException("bad password");
			}
			TrustLevel trust =
					authInfo.getEnum("trust_level", TrustLevel.class);
			switch (trust) {
			case ADMIN:
				authorities.add(() -> SecurityConfig.GRANT_ADMIN);
				// fallthrough
			case USER:
				authorities.add(() -> SecurityConfig.GRANT_USER);
				// fallthrough
			case READER:
				authorities.add(() -> SecurityConfig.GRANT_READER);
				// fallthrough
			case BASIC:
				// Do nothing; no grants of authority made
			}
			queries.loginSuccess.call(userId);
		} catch (AuthenticationException e) {
			for (Row row : queries.loginFailure.call(maxLoginFailures,
					userId)) {
				if (row.getBoolean("locked")) {
					log.warn("automatically locking user {} for {}", name,
							lockInterval);
				}
			}
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
