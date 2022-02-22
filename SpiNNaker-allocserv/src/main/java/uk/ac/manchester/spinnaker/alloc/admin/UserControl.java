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

import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.MemberRecord;
import uk.ac.manchester.spinnaker.alloc.model.PasswordChangeRecord;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.security.PasswordServices;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * User and group administration DAO.
 *
 * @author Donal Fellows
 */
@Service
public class UserControl extends DatabaseAwareBean {
	@Autowired
	private PasswordServices passServices;

	private final class AllUsersSQL extends AbstractSQL {
		private final Query allUsers = conn.query(LIST_ALL_USERS);

		@Override
		public void close() {
			allUsers.close();
			super.close();
		}

		MappableIterable<Row> allUsers() {
			return allUsers.call();
		}
	}

	private abstract class UserCheckSQL extends AbstractSQL {
		private final Query userCheck = conn.query(GET_USER_ID);

		private final Query getUserDetails = conn.query(GET_USER_DETAILS);

		@Override
		public void close() {
			userCheck.close();
			getUserDetails.close();
			super.close();
		}

		Optional<Row> getUser(int id) {
			return getUserDetails.call1(id);
		}
	}

	private final class CreateSQL extends UserCheckSQL {
		private final Update createUser = conn.update(CREATE_USER);

		@Override
		public void close() {
			createUser.close();
			super.close();
		}

		Optional<Integer> createUser(String name, String encPass,
				TrustLevel trustLevel, boolean disabled) {
			return createUser.key(name, encPass, trustLevel, disabled);
		}
	}

	private final class UpdateAllSQL extends UserCheckSQL {
		private final Update setUserName = conn.update(SET_USER_NAME);

		private final Update setUserPass = conn.update(SET_USER_PASS);

		private final Update setUserDisabled = conn.update(SET_USER_DISABLED);

		private final Update setUserLocked = conn.update(SET_USER_LOCKED);

		private final Update setUserTrust = conn.update(SET_USER_TRUST);

		@Override
		public void close() {
			setUserName.close();
			setUserPass.close();
			setUserDisabled.close();
			setUserLocked.close();
			setUserTrust.close();
			super.close();
		}
	}

	private final class UpdatePassSQL extends UserCheckSQL {
		private Query getPasswordedUser = conn.query(GET_LOCAL_USER_DETAILS);

		private Update setPassword = conn.update(SET_USER_PASS);

		@Override
		public void close() {
			getPasswordedUser.close();
			setPassword.close();
			super.close();
		}
	}

	private final class DeleteUserSQL extends UserCheckSQL {
		private final Query getUserName = conn.query(GET_USER_DETAILS);

		private final Update deleteUser = conn.update(DELETE_USER);

		@Override
		public void close() {
			getUserName.close();
			deleteUser.close();
			super.close();
		}
	}

	private final class GroupsSQL extends AbstractSQL {
		private final Query listGroups = conn.query(LIST_ALL_GROUPS);

		private final Query getUsers = conn.query(GET_USERS_OF_GROUP);

		private final Query getGroupId = conn.query(GET_GROUP_BY_ID);

		private final Query getGroupName = conn.query(GET_GROUP_BY_NAME);

		private final Update insertGroup = conn.update(CREATE_GROUP);

		private final Query deleteGroup = conn.query(DELETE_GROUP);

		@Override
		public void close() {
			listGroups.close();
			getUsers.close();
			getGroupId.close();
			getGroupName.close();
			insertGroup.close();
			deleteGroup.close();
			super.close();
		}

		MappableIterable<Row> listGroups() {
			return listGroups.call();
		}

		Optional<Row> getGroupId(int id) {
			return getGroupId.call1(id);
		}

		Optional<Row> getGroupName(String name) {
			return getGroupName.call1(name);
		}

		Optional<Integer> insertGroup(String name, Optional<Long> quota,
				boolean internal) {
			return insertGroup.key(name, quota, internal);
		}

		Optional<Row> deleteGroup(int id) {
			return deleteGroup.call1(id);
		}

		GroupRecord asGroupRecord(Row row) {
			GroupRecord group = new GroupRecord();
			group.setGroupId(row.getInteger("group_id"));
			group.setGroupName(row.getString("group_name"));
			group.setQuota(row.getLong("quota"));
			group.setInternal(row.getBoolean("is_internal"));
			return group;
		}

		GroupRecord populateMemberships(GroupRecord group,
				Function<MemberRecord, URI> urlGen) {
			if (nonNull(urlGen)) {
				UserControl uc = UserControl.this;
				group.setMembers(getUsers.call(group.getGroupId())
						.map(uc::member).toMap(TreeMap::new,
								MemberRecord::getUserName, urlGen));
			}
			return group;
		}
	}

	private MemberRecord member(Row row) {
		MemberRecord m = new MemberRecord();
		m.setId(row.getInt("membership_id"));
		m.setGroupId(row.getInt("group_id"));
		m.setGroupName(row.getString("group_name"));
		m.setUserId(row.getInt("user_id"));
		m.setUserName(row.getString("user_name"));
		return m;
	}

	/**
	 * List the users in the database.
	 *
	 * @return List of users. Only {@link UserRecord#userId} and
	 *         {@link UserRecord#userName} fields are inflated.
	 */
	public List<UserRecord> listUsers() {
		try (AllUsersSQL sql = new AllUsersSQL()) {
			return sql.transaction(false,
					() -> sql.allUsers().map(UserControl::sketchUser).toList());
		}
	}

	/**
	 * List the users in the database.
	 *
	 * @param uriMapper
	 *            How to construct a URL for the user.
	 * @return Map of users to URLs.
	 */
	public Map<String, URI> listUsers(Function<UserRecord, URI> uriMapper) {
		try (AllUsersSQL sql = new AllUsersSQL()) {
			return sql.transaction(false,
					() -> sql.allUsers().map(UserControl::sketchUser).toMap(
							TreeMap::new, UserRecord::getUserName, uriMapper));
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
		return sql.createUser(user.getUserName(), encPass, user.getTrustLevel(),
				!user.isEnabled()).flatMap(userId -> {
					// TODO inflate the groups
					return sql.getUser(userId).map(row -> getUser(sql, row));
				});
	}

	private final class GetUserSQL extends UserCheckSQL {
		Query getUserDetails = conn.query(GET_USER_DETAILS);

		Query getUserDetailsByName = conn.query(GET_USER_DETAILS_BY_NAME);

		@Override
		public void close() {
			getUserDetails.close();
			getUserDetailsByName.close();
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

	/**
	 * Get a description of a user.
	 *
	 * @param user
	 *            The name of the user.
	 * @return A description of the user, or {@link Optional#empty()} if the
	 *         user doesn't exist.
	 */
	public Optional<UserRecord> getUser(String user) {
		try (GetUserSQL sql = new GetUserSQL()) {
			return sql.transaction(() -> sql.getUserDetailsByName.call1(user)
					.map(row -> getUser(sql, row)));
		}
	}

	private static UserRecord getUser(UserCheckSQL sql, Row row) {
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
	private static int getCurrentUserId(UserCheckSQL sql, String userName) {
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

		return sql.getUser(id).map(row -> getUser(sql, row));
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

	private static PasswordChangeRecord passChange(Row row) {
		return new PasswordChangeRecord(row.getInt("user_id"),
				row.getString("user_name"));
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
					.map(UserControl::passChange).orElseThrow(
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
			baseUser = passChange(row);
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

	/**
	 * List the groups in the database. Does not include membership data.
	 *
	 * @return List of groups.
	 */
	public List<GroupRecord> listGroups() {
		try (GroupsSQL sql = new GroupsSQL()) {
			return sql.transaction(false,
					() -> sql.listGroups().map(sql::asGroupRecord).toList());
		}
	}

	/**
	 * List the groups in the database.
	 *
	 * @param uriMapper
	 *            How to construct a URL for the group.
	 * @return Map of group names to URLs.
	 */
	public Map<String, URI> listGroups(Function<GroupRecord, URI> uriMapper) {
		try (GroupsSQL sql = new GroupsSQL()) {
			return sql.transaction(false,
					() -> sql.listGroups().map(sql::asGroupRecord).toMap(
							TreeMap::new, GroupRecord::getGroupName,
							uriMapper));
		}
	}

	/**
	 * Get a description of a group. Includes group membership data.
	 *
	 * @param id
	 *            The ID of the group.
	 * @param urlGen
	 *            How to construct the URL for a group membership. If
	 *            {@code null}, the memberships will be omitted.
	 * @return A description of the group, or {@link Optional#empty()} if the
	 *         group doesn't exist.
	 */
	public Optional<GroupRecord> getGroup(int id,
			Function<MemberRecord, URI> urlGen) {
		try (GroupsSQL sql = new GroupsSQL()) {
			return sql.transaction(false,
					() -> sql.getGroupId(id).map(sql::asGroupRecord)
							.map(g -> sql.populateMemberships(g, urlGen)));
		}
	}

	/**
	 * Get a description of a group. Includes group membership data.
	 *
	 * @param name
	 *            The name of the group.
	 * @param urlGen
	 *            How to construct the URL for a group membership. If
	 *            {@code null}, the memberships will be omitted.
	 * @return A description of the group, or {@link Optional#empty()} if the
	 *         group doesn't exist.
	 */
	public Optional<GroupRecord> getGroup(String name,
			Function<MemberRecord, URI> urlGen) {
		try (GroupsSQL sql = new GroupsSQL()) {
			return sql.transaction(false,
					() -> sql.getGroupName(name).map(sql::asGroupRecord)
							.map(g -> sql.populateMemberships(g, urlGen)));
		}
	}

	/**
	 * Create a group from a supplied group.
	 *
	 * @param groupTemplate
	 *            Description of what the group should look like. Only the
	 *            {@code groupName} and the {@code quota} properties are used.
	 * @param internal
	 *            Whether this should be an internal group; internal groups hold
	 *            internal users, external groups hold external users.
	 * @return The full group description, assuming all went well.
	 */
	public Optional<GroupRecord> createGroup(GroupRecord groupTemplate,
			boolean internal) {
		try (GroupsSQL sql = new GroupsSQL()) {
			return sql.transaction(() -> sql
					.insertGroup(groupTemplate.getGroupName(),
							groupTemplate.getQuota(), internal)
					.flatMap(sql::getGroupId).map(sql::asGroupRecord));
		}
	}

	/**
	 * Delete a group. This removes all users from that group automatically.
	 *
	 * @param groupId
	 *            The ID of the group to delete.
	 * @return The deleted group name on success; {@link Optional#empty()} on
	 *         failure.
	 */
	public Optional<String> deleteGroup(int groupId) {
		try (GroupsSQL sql = new GroupsSQL()) {
			return sql.transaction(
					() -> sql.deleteGroup(groupId).map(string("group_name")));
		}
	}

	/**
	 * Adds a user to a group.
	 *
	 * @param user
	 *            What user to add.
	 * @param group
	 *            What group to add to.
	 * @return Whether the adding succeeded.
	 */
	public boolean addUserToGroup(UserRecord user, GroupRecord group) {
		try (Connection c = getConnection();
				Update insert = c.update(ADD_USER_TO_GROUP)) {
			return c.transaction(
					() -> insert.key(user.getUserId(), group.getGroupId()))
					.isPresent();
		}
	}

	/**
	 * Removes a user from a group.
	 *
	 * @param user
	 *            What user to remove.
	 * @param group
	 *            What group to remove from.
	 * @return Whether the removing succeeded.
	 */
	public boolean removeUserFromGroup(UserRecord user, GroupRecord group) {
		try (Connection c = getConnection();
				Update delete = c.update(REMOVE_USER_FROM_GROUP)) {
			return c.transaction(() -> delete.call(user.getUserId(),
					group.getGroupId())) > 0;
		}
	}
}
