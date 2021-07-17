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

import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;

import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.model.PasswordChangeRecord;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;

/**
 * User administration controller.
 *
 * @author Donal Fellows
 */
@Component
public class UserControl extends SQLQueries {
	@Autowired
	private DatabaseEngine db;

	/**
	 * List the users in the database.
	 *
	 * @return List of users. Only {@link UserRecord#userId} and
	 *         {@link UserRecord#userName} fields are inflated.
	 * @throws SQLException
	 *             If DB access fails.
	 */
	public List<UserRecord> listUsers() throws SQLException {
		List<UserRecord> result = new ArrayList<>();
		try (Connection c = db.getConnection();
				Query q = query(c, LIST_ALL_USERS)) {
			transaction(c, () -> {
				for (Row row : q.call()) {
					UserRecord userSketch = new UserRecord();
					userSketch.setUserId(row.getInt("user_id"));
					userSketch.setUserName(row.getString("user_name"));
					result.add(userSketch);
				}
			});
		}
		return result;
	}

	/**
	 * Create a user.
	 *
	 * @param user
	 *            The description of the user to create.
	 * @return A description of the created user, or {@link Optional#empty()} if
	 *         the user exists already.
	 * @throws SQLException
	 *             If DB access fails.
	 */
	public Optional<UserRecord> createUser(UserRecord user)
			throws SQLException {
		try (Connection c = db.getConnection();
				Update createUser = update(c, CREATE_USER);
				Update makeQuotas = update(c, CREATE_QUOTA);
				Query getUserDetails = query(c, GET_USER_DETAILS)) {
			return transaction(c, () -> {
				Optional<Integer> key =
						createUser.key(user.getUserName(), user.getPassword(),
								user.getTrustLevel(), !user.isEnabled());
				if (!key.isPresent()) {
					return Optional.empty();
				}
				int userId = key.get();
				for (Entry<String, Long> quotaInfo : user.getQuota()
						.entrySet()) {
					makeQuotas.call(userId, quotaInfo.getValue(),
							quotaInfo.getKey());
				}
				for (Row row : getUserDetails.call(userId)) {
					return Optional.of(getUser(c, row));
				}
				return Optional.empty();
			});
		}
	}

	/**
	 * Get a description of a user.
	 *
	 * @param id
	 *            The ID of the user.
	 * @return A description of the user, or {@link Optional#empty()} if the
	 *         user doesn't exist.
	 * @throws SQLException
	 *             If DB access fails.
	 */
	public Optional<UserRecord> getUser(int id) throws SQLException {
		try (Connection c = db.getConnection();
				Query getUserDetails = query(c, GET_USER_DETAILS)) {
			return transaction(c, () -> {
				for (Row row : getUserDetails.call(id)) {
					return Optional.of(getUser(c, row));
				}
				return Optional.empty();
			});
		}
	}

	private UserRecord getUser(Connection c, Row row) throws SQLException {
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
			try (Query getQuotas = query(c, GET_QUOTA_DETAILS)) {
				for (Row qrow : getQuotas.call(user.getUserId())) {
					Number quotaInfo = (Number) qrow.getObject("quota");
					Long quota =
							quotaInfo == null ? null : quotaInfo.longValue();
					quotas.put(qrow.getString("machine_name"), quota);
				}
			}
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
	 * @throws SQLException
	 *             If DB access goes wrong
	 */
	public Optional<UserRecord> updateUser(int id, UserRecord user,
			String adminUser) throws SQLException {
		try (Connection c = db.getConnection();
				Query userCheck = query(c, GET_USER_ID);
				Update setUserName = update(c, SET_USER_NAME);
				Update setUserPass = update(c, SET_USER_PASS);
				Update setUserDisabled = update(c, SET_USER_DISABLED);
				Update setUserLocked = update(c, SET_USER_LOCKED);
				Update setUserTrust = update(c, SET_USER_TRUST);
				Update setUserQuota = update(c, SET_USER_QUOTA);
				Query getUserDetails = query(c, GET_USER_DETAILS)) {
			return transaction(c, () -> {
				int adminId =
						userCheck.call1(adminUser).get().getInt("user_id");
				if (user.getUserName() != null) {
					setUserName.call(user.getUserName(), id);
				}
				if (user.getPassword() != null) {
					setUserPass.call(user.getPassword(), id);
				} else if (user.isExternallyAuthenticated()) {
					// Forces external authentication
					setUserPass.call(null, id);
				} else {
					// Weren't told to set the password
					assert true;
				}
				if (user.isEnabled() != null && adminId != id) {
					// Admins can't change their own disable state
					setUserDisabled.call(!user.isEnabled(), id);
				}
				if (user.isLocked() != null && !user.isLocked()
						&& adminId != id) {
					// Admins can't change their own locked state
					setUserLocked.call(user.isLocked(), id);
				}
				if (user.getTrustLevel() != null && adminId != id) {
					// Admins can't change their own trust level
					setUserTrust.call(user.getTrustLevel(), id);
				}
				if (user.getQuota() != null) {
					for (Entry<String, Long> quota : user.getQuota()
							.entrySet()) {
						setUserQuota.call(quota.getValue(), id, quota.getKey());
					}
				}
				for (Row row : getUserDetails.call(id)) {
					return Optional.of(getUser(c, row));
				}
				return Optional.empty();
			});
		}
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
	 * @throws SQLException
	 *             If DB access goes wrong
	 */
	public Optional<String> deleteUser(int id, String adminUser)
			throws SQLException {
		try (Connection c = db.getConnection();
				Query userCheck = query(c, GET_USER_ID);
				Query getUserName = query(c, GET_USER_DETAILS);
				Update deleteUser = update(c, DELETE_USER)) {
			return transaction(c, () -> {
				if (userCheck.call1(adminUser).get().getInt("user_id") == id) {
					// May not delete yourself!
					return Optional.empty();
				}
				String userName =
						getUserName.call1(id).get().getString("user_name");
				if (deleteUser.call(id) == 1) {
					return Optional.of(userName);
				}
				return Optional.empty();
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
	 * @throws SQLException
	 *             If DB access fails.
	 */
	public PasswordChangeRecord getUserForPrincipal(Principal principal)
			throws AuthenticationException, SQLException {
		try (Connection c = db.getConnection();
				Query q = query(c, GET_LOCAL_USER_DETAILS)) {
			return transaction(c, () -> {
				Row row = q.call1(principal.getName()).orElseThrow(
						// OpenID-authenticated user; go away
						() -> new AuthenticationServiceException(
								"user is managed externally; "
										+ "cannot manage password here"));
				return new PasswordChangeRecord(row.getInt("user_id"),
						row.getString("user_name"));
			});
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
	 * @throws SQLException
	 *             If DB access fails.
	 */
	public PasswordChangeRecord updateUserOfPrincipal(Principal principal,
			PasswordChangeRecord user)
			throws AuthenticationException, SQLException {
		try (Connection c = db.getConnection();
				Query getPasswordedUser = query(c, GET_LOCAL_USER_DETAILS);
				Query isPasswordMatching = query(c, IS_USER_PASS_MATCHED);
				Update setPassword = update(c, SET_USER_PASS)) {
			return transaction(c, () -> {
				Row row = getPasswordedUser.call1(principal.getName())
						.orElseThrow(
								// OpenID-authenticated user; go away
								() -> new AuthenticationServiceException(
										"user is managed externally; cannot "
												+ "change password here"));
				PasswordChangeRecord baseUser = new PasswordChangeRecord(
						row.getInt("user_id"), row.getString("user_name"));
				if (!isPasswordMatching.call1(user.getOldPassword(),
						baseUser.getUserId()).get().getBoolean("matches")) {
					throw new BadCredentialsException("bad password");
				}
				// Validate change; this should never fail but...
				if (!user.isNewPasswordMatched()) {
					throw new BadCredentialsException("bad password");
				}
				if (setPassword.call(user.getNewPassword(),
						baseUser.getUserId()) != 1) {
					throw new InternalAuthenticationServiceException(
							"failed to update database");
				}
				return baseUser;
			});
		}
	}
}
