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

import static java.util.Objects.requireNonNull;
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

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.User;

/**
 * User administration controller.
 *
 * @author Donal Fellows
 */
@Component
public class UserControl extends SQLQueries {
	@Autowired
	private DatabaseEngine db;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * List the users in the database.
	 *
	 * @return List of users. Only {@link User#userId} and {@link User#userName}
	 *         fields are inflated.
	 * @throws SQLException
	 *             If DB access fails.
	 */
	public List<User> listUsers() throws SQLException {
		List<User> result = new ArrayList<>();
		try (Connection c = db.getConnection();
				Query q = query(c, LIST_ALL_USERS)) {
			transaction(c, () -> {
				for (Row row : q.call()) {
					User userSketch = new User();
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
	public Optional<User> createUser(User user) throws SQLException {
		try (Connection c = db.getConnection();
				Update makeUser = update(c, CREATE_USER);
				Update makeQuotas = update(c, CREATE_QUOTA);
				Query getUserDetails = query(c, GET_USER_DETAILS)) {
			String pass = user.getPassword() == null ? null
					: passwordEncoder.encode(user.getPassword());
			return transaction(c, () -> {
				Optional<Integer> key = makeUser.key(user.getUserName(), pass,
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
	public Optional<User> getUser(int id) throws SQLException {
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

	private User getUser(Connection c, Row row) throws SQLException {
		User user = new User();
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
	public Optional<User> updateUser(int id, User user, String adminUser)
			throws SQLException {
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
					setUserPass.call(passwordEncoder.encode(user.getPassword()),
							id);
				} else if (user.isExternallyAuthenticated()) {
					// Forces external authentication
					setUserPass.call(null, id);
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
	 * Describes basic information about a user that they'd use to change their
	 * password.
	 *
	 * @author Donal Fellows
	 */
	public static class UserPassChangeModel {
		private int userId;

		private String username;

		private String oldPassword;

		private String newPassword;

		private String newPassword2;

		public UserPassChangeModel() {
			this.userId = -1;
			this.username = "";
			this.oldPassword = "";
			this.newPassword = "";
			this.newPassword2 = "";
		}

		UserPassChangeModel(int userId, String username) {
			this();
			this.userId = userId;
			this.username = requireNonNull(username);
		}

		/**
		 * @return the user id
		 */
		public final int getUserId() {
			return userId;
		}

		public void setUserId(int userId) {
			this.userId = userId;
		}

		/**
		 * @return the username
		 */
		public final String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
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
	public UserPassChangeModel getUserForPrincipal(Principal principal)
			throws AuthenticationException, SQLException {
		try (Connection c = db.getConnection();
				Query q = query(c, "SELECT user_id, user_name FROM user_info "
						+ "WHERE user_name = :username "
						+ "AND encrypted_password IS NOT NULL LIMIT 1")) {
			Row row = q.call1(principal.getName()).orElseThrow(
					// OpenID-authenticated user; go away
					() -> new AuthenticationServiceException(
							"user is managed externally; "
									+ "cannot manage password here"));
			return new UserPassChangeModel(row.getInt("user_id"),
					row.getString("user_name"));
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
	public UserPassChangeModel updateUserOfPrincipal(Principal principal,
			UserPassChangeModel user)
			throws AuthenticationException, SQLException {
		try (Connection c = db.getConnection();
				Query getPassword = query(c,
						"SELECT user_id, user_name, encrypted_password "
								+ "FROM user_info "
								+ "WHERE user_name = :user_name "
								+ "AND encrypted_password IS NOT NULL LIMIT 1");
				Update setPassword = update(c, SET_USER_PASS)) {
			return transaction(c, () -> {
				Row row = getPassword.call1(principal.getName()).orElseThrow(
						// OpenID-authenticated user; go away
						() -> new AuthenticationServiceException(
								"user is managed externally; "
										+ "cannot change password here"));
				UserPassChangeModel baseUser = new UserPassChangeModel(
						row.getInt("user_id"), row.getString("user_name"));
				if (!passwordEncoder.matches(user.oldPassword,
						row.getString("encrypted_password"))) {
					throw new BadCredentialsException("bad password");
				}
				// Validate change; this should never fail but...
				if (!user.newPassword.equals(user.newPassword2)) {
					throw new BadCredentialsException("bad password");
				}
				if (setPassword.call(passwordEncoder.encode(user.newPassword),
						baseUser.userId) != 1) {
					throw new InternalAuthenticationServiceException(
							"failed to update database");
				}
				return baseUser;
			});
		}
	}
}
