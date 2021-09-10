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
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.rowsAsList;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;

import java.security.Principal;
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
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Connection;
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
	 */
	public List<UserRecord> listUsers() {
		try (Connection c = db.getConnection();
				Query q = query(c, LIST_ALL_USERS)) {
			return transaction(c,
					() -> rowsAsList(q.call(), UserControl::sketchUser));
		}
	}

	private static UserRecord sketchUser(Row row) {
		UserRecord userSketch = new UserRecord();
		userSketch.setUserId(row.getInt("user_id"));
		userSketch.setUserName(row.getString("user_name"));
		return userSketch;
	}

	private static final class CreateSQL implements AutoCloseable {
		private final Update createUser;

		private final Update makeQuotas;

		private final Update makeDefaultQuotas;

		private final Query getUserDetails;

		CreateSQL(Connection c) {
			createUser = update(c, CREATE_USER);
			makeQuotas = update(c, CREATE_QUOTA);
			makeDefaultQuotas = update(c, CREATE_QUOTAS_FROM_DEFAULTS);
			getUserDetails = query(c, GET_USER_DETAILS);
		}

		@Override
		public void close() {
			createUser.close();
			makeQuotas.close();
			makeDefaultQuotas.close();
			getUserDetails.close();
		}
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
		try (Connection c = db.getConnection();
				CreateSQL sql = new CreateSQL(c)) {
			return transaction(c, () -> createUser(user, c, sql));
		}
	}

	private Optional<UserRecord> createUser(UserRecord user, Connection c,
			CreateSQL sql) {
		return sql.createUser
				.key(user.getUserName(), user.getPassword(),
						user.getTrustLevel(), !user.isEnabled())
				.flatMap(userId -> {
					if (isNull(user.getQuota())) {
						sql.makeDefaultQuotas.call(userId);
					} else {
						for (Entry<String, Long> quotaInfo : user.getQuota()
								.entrySet()) {
							sql.makeQuotas.call(userId, quotaInfo.getValue(),
									quotaInfo.getKey());
						}
					}
					return sql.getUserDetails.call1(userId)
							.map(row -> getUser(c, row));
				});
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
		try (Connection c = db.getConnection();
				Query getUserDetails = query(c, GET_USER_DETAILS)) {
			return transaction(c,
					() -> getUserDetails.call1(id).map(row -> getUser(c, row)));
		}
	}

	private static UserRecord getUser(Connection c, Row row) {
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
				getQuotas.call(user.getUserId()).forEach(qrow -> {
					Number quotaInfo = (Number) qrow.getObject("quota");
					Long quota =
							isNull(quotaInfo) ? null : quotaInfo.longValue();
					quotas.put(qrow.getString("machine_name"), quota);
				});
			}
			user.setQuota(quotas);
		} finally {
			// I mean it!
			user.setPassword(null);
		}
		return user;
	}

	private static final class UpdateSQL implements AutoCloseable {
		private final Update setUserName;

		private final Update setUserPass;

		private final Update setUserDisabled;

		private final Update setUserLocked;

		private final Update setUserTrust;

		private final Update setUserQuota;

		private final Query getUserDetails;

		UpdateSQL(Connection c) {
			setUserName = update(c, SET_USER_NAME);
			setUserPass = update(c, SET_USER_PASS);
			setUserDisabled = update(c, SET_USER_DISABLED);
			setUserLocked = update(c, SET_USER_LOCKED);
			setUserTrust = update(c, SET_USER_TRUST);
			setUserQuota = update(c, SET_USER_QUOTA);
			getUserDetails = query(c, GET_USER_DETAILS);
		}

		@Override
		public void close() {
			setUserName.close();
			setUserPass.close();
			setUserDisabled.close();
			setUserLocked.close();
			setUserTrust.close();
			setUserQuota.close();
			getUserDetails.close();
		}
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
		try (Connection c = db.getConnection();
				UpdateSQL sql = new UpdateSQL(c)) {
			return transaction(c,
					() -> updateUser(id, user, adminUser, c, sql));
		}
	}

	// Use this for looking up the current user, who should exist!
	private static int getCurrentUserId(Connection c, String userName) {
		try (Query userCheck = query(c, GET_USER_ID)) {
			return userCheck.call1(userName)
					.orElseThrow(() -> new RuntimeException(
							"current user has unexpectedly vanshed"))
					.getInt("user_id");
		}
	}

	private Optional<UserRecord> updateUser(int id, UserRecord user,
			String adminUser, Connection c, UpdateSQL sql) {
		int adminId = getCurrentUserId(c, adminUser);

		if (nonNull(user.getUserName())) {
			sql.setUserName.call(user.getUserName(), id);
		}

		if (nonNull(user.getPassword())) {
			sql.setUserPass.call(user.getPassword(), id);
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
			for (Entry<String, Long> quota : user.getQuota().entrySet()) {
				sql.setUserQuota.call(quota.getValue(), id, quota.getKey());
			}
		}

		return sql.getUserDetails.call1(id).map(row -> getUser(c, row));
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
		try (Connection c = db.getConnection();
				Query getUserName = query(c, GET_USER_DETAILS);
				Update deleteUser = update(c, DELETE_USER)) {
			return transaction(c, () -> {
				if (getCurrentUserId(c, adminUser) == id) {
					// May not delete yourself!
					return Optional.empty();
				}
				return getUserName.call1(id).flatMap(row -> {
					String userName = row.getString("user_name");
					return Optional.ofNullable(
							deleteUser.call(id) == 1 ? userName : null);
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
		try (Connection c = db.getConnection();
				Query q = query(c, GET_LOCAL_USER_DETAILS)) {
			return transaction(c, () -> q.call1(principal.getName())
					.map(row -> new PasswordChangeRecord(
							row.getInt("user_id"), row.getString("user_name")))
					.orElseThrow(
							// OpenID-authenticated user; go away
							() -> new AuthenticationServiceException(
									"user is managed externally; "
											+ "cannot manage password here")));
		}
	}

	private static final class UpdatePassSQL implements AutoCloseable {
		private Query getPasswordedUser;

		private Query isPasswordMatching;

		private Update setPassword;

		UpdatePassSQL(Connection c) {
			getPasswordedUser = query(c, GET_LOCAL_USER_DETAILS);
			isPasswordMatching = query(c, IS_USER_PASS_MATCHED);
			setPassword = update(c, SET_USER_PASS);
		}

		@Override
		public void close() {
			getPasswordedUser.close();
			isPasswordMatching.close();
			setPassword.close();
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
		try (Connection c = db.getConnection();
				UpdatePassSQL sql = new UpdatePassSQL(c)) {
			return transaction(c,
					() -> updateUserOfPrincipal(principal, user, sql));
		}
	}

	private PasswordChangeRecord updateUserOfPrincipal(Principal principal,
			PasswordChangeRecord user, UpdatePassSQL sql) {
		Row row = sql.getPasswordedUser.call1(principal.getName()).orElseThrow(
				// OpenID-authenticated user; go away
				() -> new AuthenticationServiceException(
						"user is managed externally; cannot "
								+ "change password here"));
		PasswordChangeRecord baseUser = new PasswordChangeRecord(
				row.getInt("user_id"), row.getString("user_name"));
		if (!sql.isPasswordMatching
				.call1(user.getOldPassword(), baseUser.getUserId()).get()
				.getBoolean("matches")) {
			throw new BadCredentialsException("bad password");
		}
		// Validate change; this should never fail but...
		if (!user.isNewPasswordMatched()) {
			throw new BadCredentialsException("bad password");
		}
		if (sql.setPassword.call(user.getNewPassword(),
				baseUser.getUserId()) != 1) {
			throw new InternalAuthenticationServiceException(
					"failed to update database");
		}
		return baseUser;
	}
}
