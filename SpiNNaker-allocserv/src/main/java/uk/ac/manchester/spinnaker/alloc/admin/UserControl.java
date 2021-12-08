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
package uk.ac.manchester.spinnaker.alloc.admin;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.PasswordServices;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.model.PasswordChangeRecord;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;

/**
 * User administration controller.
 *
 * @author Donal Fellows
 */
@Service
public class UserControl extends DatabaseAwareBean {
	@Autowired
	private PasswordServices passServices;

	private abstract class AccessQuotaSQL extends AbstractSQL {
		private final Query getQuotas = conn.query(GET_QUOTA_DETAILS);

		private final Query userCheck = conn.query(GET_USER_ID);

		@Override
		public void close() {
			getQuotas.close();
			userCheck.close();
			super.close();
		}
	}

	private final class CreateSQL extends AccessQuotaSQL {
		private final Update createUser = conn.update(CREATE_USER);

		private final Update makeQuotas = conn.update(CREATE_QUOTA);

		private final Update makeDefaultQuotas =
				conn.update(CREATE_QUOTAS_FROM_DEFAULTS);

		private final Query getUserDetails = conn.query(GET_USER_DETAILS);

		@Override
		public void close() {
			createUser.close();
			makeQuotas.close();
			makeDefaultQuotas.close();
			getUserDetails.close();
			super.close();
		}
	}

	private final class UpdateAllSQL extends AccessQuotaSQL {
		private final Update setUserName = conn.update(SET_USER_NAME);

		private final Update setUserPass = conn.update(SET_USER_PASS);

		private final Update setUserDisabled = conn.update(SET_USER_DISABLED);

		private final Update setUserLocked = conn.update(SET_USER_LOCKED);

		private final Update setUserTrust = conn.update(SET_USER_TRUST);

		private final Update setUserQuota = conn.update(SET_USER_QUOTA);

		private final Query getUserDetails = conn.query(GET_USER_DETAILS);

		@Override
		public void close() {
			setUserName.close();
			setUserPass.close();
			setUserDisabled.close();
			setUserLocked.close();
			setUserTrust.close();
			setUserQuota.close();
			getUserDetails.close();
			super.close();
		}
	}

	private final class UpdatePassSQL extends AccessQuotaSQL {
		private Query getPasswordedUser = conn.query(GET_LOCAL_USER_DETAILS);

		private Update setPassword = conn.update(SET_USER_PASS);

		@Override
		public void close() {
			getPasswordedUser.close();
			setPassword.close();
			super.close();
		}
	}

	private final class DeleteUserSQL extends AccessQuotaSQL {
		private final Query getUserName = conn.query(GET_USER_DETAILS);

		private final Update deleteUser = conn.update(DELETE_USER);

		@Override
		public void close() {
			getUserName.close();
			deleteUser.close();
			super.close();
		}
	}

	/**
	 * List the users in the database.
	 *
	 * @return List of users. Only {@link UserRecord#userId} and
	 *         {@link UserRecord#userName} fields are inflated.
	 */
	public List<UserRecord> listUsers() {
		try (Connection c = getConnection();
				Query q = c.query(LIST_ALL_USERS)) {
			return c.transaction(false,
					() -> q.call().map(UserControl::sketchUser).toList());
		}
	}

	private static UserRecord sketchUser(Row row) {
		UserRecord userSketch = new UserRecord();
		userSketch.setUserId(row.getInt("user_id"));
		userSketch.setUserName(row.getString("user_name"));
		return userSketch;
	}

	/**
	 * Create a user.
	 *
	 * @param user
	 *            The description of the user to create.
	 * @return A description of the created user, or {@link Optional#empty()} if
	 *         the user exists already.
	 */
	public Optional<UserRecord> createUser(UserRecord user) {
		// This is a slow operation; don't hold a database transaction
		String encPass = passServices.encodePassword(user.getPassword());
		try (CreateSQL sql = new CreateSQL()) {
			return sql.transaction(() -> createUser(user, encPass, sql));
		}
	}

	private Optional<UserRecord> createUser(UserRecord user, String encPass,
			CreateSQL sql) {
		return sql.createUser
				.key(user.getUserName(), encPass, user.getTrustLevel(),
						!user.isEnabled())
				.flatMap(userId -> {
					if (isNull(user.getQuota())) {
						sql.makeDefaultQuotas.call(userId);
					} else {
						user.getQuota()
								.forEach((machineName, quota) -> sql.makeQuotas
										.call(userId, quota, machineName));
					}
					return sql.getUserDetails.call1(userId)
							.map(row -> getUser(sql, row));
				});
	}

	private final class GetUserSQL extends AccessQuotaSQL {
		Query getUserDetails = conn.query(GET_USER_DETAILS);

		@Override
		public void close() {
			getUserDetails.close();
			super.close();
		}
	}

	/**
	 * Get a description of a user.
	 *
	 * @param id
	 *            The ID of the user.
	 * @return A description of the user, or {@link Optional#empty()} if the
	 *         user doesn't exist.
	 */
	public Optional<UserRecord> getUser(int id) {
		try (GetUserSQL sql = new GetUserSQL()) {
			return sql.transaction(() -> sql.getUserDetails.call1(id)
					.map(row -> getUser(sql, row)));
		}
	}

	private static UserRecord getUser(AccessQuotaSQL sql, Row row) {
		UserRecord user = new UserRecord();
		try {
			user.setUserId(row.getInt("user_id"));
			user.setUserName(row.getString("user_name"));
			user.setHasPassword(row.getBoolean("has_password"));
			user.setTrustLevel(row.getEnum("trust_level", TrustLevel.class));
			user.setEnabled(!row.getBoolean("disabled"));
			user.setLocked(row.getBoolean("locked"));
			user.setLastSuccessfulLogin(
					row.getInstant("last_successful_login_timestamp"));
			user.setLastFailedLogin(row.getInstant("last_fail_timestamp"));
			HashMap<String, Long> quotas = new HashMap<>();
			sql.getQuotas.call(user.getUserId())
					.forEach(qrow -> quotas.put(qrow.getString("machine_name"),
							qrow.getLong("quota")));
			user.setQuota(quotas);
		} finally {
			// I mean it!
			user.setPassword(null);
		}
		return user;
	}

	/**
	 * Updates a user.
	 *
	 * @param id
	 *            The ID of the user
	 * @param user
	 *            The description of what to update.
	 * @param adminUser
	 *            The <em>name</em> of the current user doing this call. Used to
	 *            prohibit any admin from doing major damage to themselves.
	 * @return The updated user
	 */
	public Optional<UserRecord> updateUser(int id, UserRecord user,
			String adminUser) {
		// Encode the password outside of any transaction; this is a slow op!
		String encPass = passServices.encodePassword(user.getPassword());
		try (UpdateAllSQL sql = new UpdateAllSQL()) {
			return sql.transaction(
					() -> updateUser(id, user, adminUser, encPass, sql));
		}
	}

	// Use this for looking up the current user, who should exist!
	private static int getCurrentUserId(AccessQuotaSQL sql, String userName) {
		return sql.userCheck.call1(userName).map(integer("user_id"))
				.orElseThrow(() -> new RuntimeException(
						"current user has unexpectedly vanshed"));
	}

	private Optional<UserRecord> updateUser(int id, UserRecord user,
			String adminUser, String encPass, UpdateAllSQL sql) {
		int adminId = getCurrentUserId(sql, adminUser);

		if (nonNull(user.getUserName())) {
			sql.setUserName.call(user.getUserName(), id);
		}

		if (nonNull(user.getPassword())) {
			sql.setUserPass.call(encPass, id);
		} else if (user.isExternallyAuthenticated()) {
			// Forces external authentication
			sql.setUserPass.call(null, id);
		} else {
			// Weren't told to set the password
			assert true;
		}

		if (nonNull(user.isEnabled()) && adminId != id) {
			// Admins can't change their own disable state
			sql.setUserDisabled.call(!user.isEnabled(), id);
		}
		if (nonNull(user.isLocked()) && !user.isLocked() && adminId != id) {
			// Admins can't change their own locked state
			sql.setUserLocked.call(user.isLocked(), id);
		}

		if (nonNull(user.getTrustLevel()) && adminId != id) {
			// Admins can't change their own trust level
			sql.setUserTrust.call(user.getTrustLevel(), id);
		}

		if (nonNull(user.getQuota())) {
			user.getQuota().forEach((machineName, quota) -> sql.setUserQuota
					.call(quota, id, machineName));
		}

		return sql.getUserDetails.call1(id).map(row -> getUser(sql, row));
	}

	/**
	 * Deletes a user.
	 *
	 * @param id
	 *            The ID of the user to delete.
	 * @param adminUser
	 *            The <em>name</em> of the current user doing this call. Used to
	 *            prohibit anyone from deleting themselves.
	 * @return The name of the deleted user if things succeeded, or
	 *         {@link Optional#empty()} on failure.
	 */
	public Optional<String> deleteUser(int id, String adminUser) {
		try (DeleteUserSQL sql = new DeleteUserSQL()) {
			return sql.transaction(() -> {
				if (getCurrentUserId(sql, adminUser) == id) {
					// May not delete yourself!
					return Optional.empty();
				}
				return sql.getUserName.call1(id).map(row -> {
					// Order matters! Get the name before the delete
					String userName = row.getString("user_name");
					return sql.deleteUser.call(id) == 1 ? userName : null;
				});
			});
		}
	}

	/**
	 * Get a model for updating the local password of the current user.
	 *
	 * @param principal
	 *            The current user
	 * @return User model object. Password fields are unfilled.
	 * @throws AuthenticationException
	 *             If the user cannot change their password here for some
	 *             reason.
	 */
	public PasswordChangeRecord getUserForPrincipal(Principal principal)
			throws AuthenticationException {
		try (Connection c = getConnection();
				Query q = c.query(GET_LOCAL_USER_DETAILS)) {
			return c.transaction(false, () -> q.call1(principal.getName())
					.map(row -> new PasswordChangeRecord(
							row.getInt("user_id"), row.getString("user_name")))
					.orElseThrow(
							// OpenID-authenticated user; go away
							() -> new AuthenticationServiceException(
									"user is managed externally; "
											+ "cannot manage password here")));
		}
	}

	/**
	 * Update the local password of the current user based on a filled out model
	 * previously provided.
	 *
	 * @param principal
	 *            The current user
	 * @param user
	 *            Valid user model object with password fields filled.
	 * @return Replacement user model object. Password fields are unfilled.
	 * @throws AuthenticationException
	 *             If the user cannot change their password here for some
	 *             reason.
	 */
	public PasswordChangeRecord updateUserOfPrincipal(Principal principal,
			PasswordChangeRecord user) throws AuthenticationException {
		try (UpdatePassSQL sql = new UpdatePassSQL()) {
			return updateUserOfPrincipal(principal, user, sql);
		}
	}

	private static final class GetUserResult {
		final PasswordChangeRecord baseUser;

		final String oldEncPass;

		GetUserResult(Row row) {
			baseUser = new PasswordChangeRecord(row.getInt("user_id"),
					row.getString("user_name"));
			oldEncPass = row.getString("encrypted_password");
		}
	}

	private PasswordChangeRecord updateUserOfPrincipal(Principal principal,
			PasswordChangeRecord user, UpdatePassSQL sql) {
		GetUserResult result = sql
				.transaction(() -> sql.getPasswordedUser
						.call1(principal.getName()).map(GetUserResult::new))
				.orElseThrow(
						// OpenID-authenticated user; go away
						() -> new AuthenticationServiceException(
								"user is managed externally; cannot "
										+ "change password here"));
		if (!passServices.matchPassword(user.getOldPassword(),
				result.oldEncPass)) {
			throw new BadCredentialsException("bad password");
		}
		// Validate change; this should never fail but...
		if (!user.isNewPasswordMatched()) {
			throw new BadCredentialsException("bad password");
		}
		String newEncPass = passServices.encodePassword(user.getNewPassword());
		return sql.transaction(() -> {
			if (sql.setPassword.call(newEncPass,
					result.baseUser.getUserId()) != 1) {
				throw new InternalAuthenticationServiceException(
						"failed to update database");
			}
			return result.baseUser;
		});
	}
}
