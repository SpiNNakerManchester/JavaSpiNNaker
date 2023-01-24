/*
 * Copyright (c) 2021-2022 The University of Manchester
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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.SUB;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.isBusy;
import static uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType.COLLABRATORY;
import static uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType.INTERNAL;
import static uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType.ORGANISATION;
import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_READER;
import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_USER;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.security.TrustLevel.ADMIN;
import static uk.ac.manchester.spinnaker.alloc.security.TrustLevel.USER;
import static uk.ac.manchester.spinnaker.utils.OptionalUtils.ifElse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.stereotype.Service;

import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.RestrictedApi;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AuthProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.QuotaProperties;
import uk.ac.manchester.spinnaker.alloc.admin.UserControl;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Update;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;

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
		implements LocalAuthenticationProvider<LocalAuthProviderImpl.TestAPI> {
	private static final Logger log = getLogger(LocalAuthProviderImpl.class);

	@Autowired
	private ServiceMasterControl control;

	@Autowired
	private AuthProperties authProps;

	@Autowired
	private QuotaProperties quotaProps;

	@Autowired
	private PasswordServices passServices;

	@Autowired
	private UserControl userController;

	private static final String DUMMY_USER = "user1";

	private static final String DUMMY_PASSWORD = "user1Pass";

	private Optional<UserRecord> makeInitUser(String username) {
		if (!authProps.isAddDummyUser()) {
			return Optional.empty();
		}
		var pass = DUMMY_PASSWORD;
		boolean poorPassword = true;
		if (authProps.isDummyRandomPass()) {
			pass = passServices.generatePassword();
			poorPassword = false;
		}
		if (!createUser(username, pass, ADMIN)) {
			// User creation failed; probably already exists, which is OK
			return Optional.empty();
		}
		if (authProps.isDummyRandomPass()) {
			log.info("admin user {} has password: {}", username, pass);
		}
		if (poorPassword) {
			log.warn("user {} has default password!", username);
		}
		return Optional.of(userController.getUser(username, null)
				.orElseThrow(() -> new SetupException(
						"default user was created, yet wasn't found!")));
	}

	private Optional<GroupRecord> makeInitGroup(String groupname) {
		if (groupname.isBlank()) {
			// No system group name, so ignore group setup
			return Optional.empty();
		}
		var template = new GroupRecord();
		template.setGroupName(groupname);
		// What should the default quota be here? Using no-quota-at-all for now
		template.setQuota(null);
		try {
			return userController.createGroup(template, INTERNAL).map(g -> {
				log.info("system group '{}' created", groupname);
				return g;
			});
		} catch (DataIntegrityViolationException e) {
			if (e.getMessage().contains("A UNIQUE constraint failed")) {
				// Already exists; no big deal
				return Optional.empty();
			} else if (e.getMessage().contains("Duplicate entry")) {
				return Optional.empty();
			}
			throw e;
		}
	}

	@PostConstruct
	private void initUserIfNecessary() {
		// User setup
		var user = makeInitUser(DUMMY_USER);

		// Group setup
		var group = makeInitGroup(authProps.getSystemGroup());

		// Connect the two if we made them both
		user.ifPresent(u -> group.ifPresent(g -> {
			if (!userController.addUserToGroup(u, g).isPresent()) {
				log.warn("user {} was not added to default group {}",
						u.getUserName(), g.getGroupName());
			}
		}));
	}

	private static final class SetupException extends RuntimeException {
		private static final long serialVersionUID = -3915472090182223715L;

		SetupException(String message) {
			super(message);
		}
	}

	@Override
	@PreAuthorize(IS_ADMIN)
	public boolean createUser(String username, String password,
			TrustLevel trustLevel) {
		var name = username.strip();
		if (name.isBlank()) {
			// Won't touch the DB if the username is empty
			throw new UsernameNotFoundException("empty user name?");
		}
		var encPass = passServices.encodePassword(password);
		try (var sql = new AuthQueries()) {
			return sql.transaction(
					() -> sql.createUser(username, encPass, trustLevel, null)
							.isPresent());
		}
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
				/*
				 * Technically, at this point we're already authenticated as
				 * we've checked that the token from Keycloak is valid. We still
				 * have to take an authorization decision though.
				 */
				var user = ((OAuth2AuthenticationToken) auth).getPrincipal();
				return authorizeOpenId(
						authProps.getOpenid().getUsernamePrefix()
								+ user.getAttribute(PREFERRED_USERNAME),
						user.getAttribute(SUB), new OriginatingCredential(user),
						auth.getAuthorities());
			} else if (auth instanceof BearerTokenAuthentication) {
				/*
				 * Technically, at this point we're already authenticated as
				 * we've checked that the token from Keycloak is valid. We still
				 * have to take an authorization decision though.
				 */
				var bearerAuth = (BearerTokenAuthentication) auth;
				var token = bearerAuth.getToken();
				return authorizeOpenId(
						authProps.getOpenid().getUsernamePrefix()
								+ bearerAuth.getTokenAttributes().get(
										PREFERRED_USERNAME),
						bearerAuth.getName(), new OriginatingCredential(token),
						auth.getAuthorities());
			} else {
				return null;
			}
		} catch (DataAccessException e) {
			throw new InternalAuthenticationServiceException(
					"database access problem", e);
		}
	}

	@Override
	public Authentication updateAuthentication(HttpServletRequest req,
			SecurityContext ctx) {
		var current = ctx.getAuthentication();
		if (nonNull(current)) {
			if (supports(current.getClass())) {
				var updated = authenticate(current);
				if (nonNull(updated) && updated != current) {
					log.debug("filter updated security from {} to {}", current,
							updated);
					ctx.setAuthentication(updated);
					return updated;
				}
			} else if (!isUnsupportedAuthTokenClass(current.getClass())) {
				log.warn("unexpected authentication type {} (token: {})"
						+ "on request {}",
						current.getClass(), current, req.getRequestURI());
			}
		}
		return null;
	}

	/** The classes that we know what to do about. */
	private static final Class<?>[] SUPPORTED_AUTH_TOKEN_CLASSES = {
		UsernamePasswordAuthenticationToken.class,
		OAuth2AuthenticationToken.class, BearerTokenAuthentication.class,
		AlreadyDoneMarker.class
	};

	/** The classes that we <em>know</em> we don't ever want to handle. */
	private static final Class<?>[] UNSUPPORTED_AUTH_TOKEN_CLASSES = {
		AnonymousAuthenticationToken.class, RememberMeAuthenticationToken.class,
		RunAsUserToken.class, TestingAuthenticationToken.class
	};

	@Override
	public boolean supports(Class<?> cls) {
		for (var c : SUPPORTED_AUTH_TOKEN_CLASSES) {
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
		for (var c : UNSUPPORTED_AUTH_TOKEN_CLASSES) {
			if (c.isAssignableFrom(cls)) {
				return true;
			}
		}
		return false;
	}

	private static final class PerformedUsernamePasswordAuthenticationToken
			extends UsernamePasswordAuthenticationToken
			implements AlreadyDoneMarker {
		private static final long serialVersionUID = -3164620207079316329L;

		PerformedUsernamePasswordAuthenticationToken(String name,
				String password, List<GrantedAuthority> authorities) {
			super(name, password, List.copyOf(authorities));
		}
	}

	/**
	 * Do authorization mapping for users coming in with a direct username and
	 * password.
	 *
	 * @param auth
	 *            The credentials they presented
	 * @return The remapped auth token, or {@code null} if we are throwing
	 *         things out at this stage.
	 * @throws AuthenticationException
	 *             If something is badly wrong.
	 */
	private Authentication authenticateDirect(
			UsernamePasswordAuthenticationToken auth)
			throws AuthenticationException {
		log.info("authenticating Local Login {}", auth.toString());
		// We ALWAYS trim the username; extraneous whitespace is bogus
		var name = auth.getName().strip();
		if (name.isBlank()) {
			// Won't touch the DB if the username is empty
			throw new UsernameNotFoundException("empty user name?");
		}
		var password = auth.getCredentials().toString();
		var authorities = new ArrayList<GrantedAuthority>();

		try (var queries = new AuthQueries()) {
			if (!authLocalAgainstDB(name, password, authorities, queries)) {
				return null;
			}
		}
		return new PerformedUsernamePasswordAuthenticationToken(name, password,
				authorities);
	}

	/**
	 * Do authorization mapping for users coming in via OpenID Connect from
	 * HBP/EBRAINS.
	 *
	 * @param name
	 *            The name of the user <em>with standard namespacing prefix
	 *            added</em>.
	 * @param subject
	 *            The OpenID subject.
	 * @param credential
	 *            The reason we believe the user should be allowed to use the
	 *            service.
	 * @param authorities
	 *            The claimed authorities derived from the HBP OpenID service.
	 *            Interesting because they include claims about organisation and
	 *            collabratory membership.
	 * @return The remapped auth token, or {@code null} if we are throwing
	 *         things out at this stage.
	 * @throws AuthenticationException
	 *             If something is badly wrong.
	 */
	private Authentication authorizeOpenId(String name, String subject,
			OriginatingCredential credential,
			Collection<? extends GrantedAuthority> authorities)
			throws AuthenticationException {
		log.debug("authenticating OpenID {}", credential);
		if (isNull(name)
				|| name.equals(authProps.getOpenid().getUsernamePrefix())) {
			// No actual name there?
			throw new UsernameNotFoundException("empty user name?");
		}
		try (var queries = new AuthQueries()) {
			if (!queries.transaction(() -> authOpenIDAgainstDB(name, subject,
					authorities, queries))) {
				return null;
			}
		} catch (RuntimeException e) {
			log.warn("serious problem when processing login for OpenID user {}",
					name, e);
			return null;
		}
		// Users from OpenID always have the same permissions
		return new OpenIDDerivedAuthenticationToken(name, credential);
	}

	/** Holds either a {@link OAuth2User} or a {@link Jwt}. */
	private static final class OriginatingCredential {
		private final OAuth2User user;

		private final OAuth2AccessToken token;

		OriginatingCredential(OAuth2User user) {
			this.user = requireNonNull(user);
			this.token = null;
		}

		OriginatingCredential(OAuth2AccessToken token) {
			this.user = null;
			this.token = requireNonNull(token);
		}

		@Override
		public String toString() {
			if (nonNull(user)) {
				return user.toString();
			} else {
				return token.toString();
			}
		}
	}

	private static final class OpenIDDerivedAuthenticationToken
			extends AbstractAuthenticationToken
			implements OpenIDUserAware, AlreadyDoneMarker {
		private static final long serialVersionUID = 970898019896708267L;

		private final String who;

		private final OriginatingCredential credential;

		private OpenIDDerivedAuthenticationToken(String who,
				OriginatingCredential credential) {
			super(List.of(new SimpleGrantedAuthority(GRANT_READER),
					new SimpleGrantedAuthority(GRANT_USER)));
			this.who = who;
			this.credential = credential;
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
			return Optional.ofNullable(credential.user);
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

		private final Update loginFailure = conn.update(MARK_LOGIN_FAILURE);

		private final Update createUser = conn.update(CREATE_USER);

		private final Update createGroup =
				conn.update(CREATE_GROUP_IF_NOT_EXISTS);

		Update unlock = conn.update(UNLOCK_LOCKED_USERS);

		@Override
		public void close() {
			createGroup.close();
			createUser.close();
			loginFailure.close();
			loginSuccess.close();
			userAuthorities.close();
			getUserBlocked.close();
			unlock.close();
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
		 * Create a user.
		 *
		 * @param username
		 *            The username to create
		 * @param encPass
		 *            The already-encoded password to use; {@code null} for a
		 *            user that needs to authenticate by another mechanism.
		 * @param trustLevel
		 *            What level of permissions to grant
		 * @param subject
		 *            The real OpenID {@code SUB} claim, if available/known.
		 * @return The user ID if the user was successfully created.
		 */
		Optional<Integer> createUser(String username, String encPass,
				TrustLevel trustLevel, String subject) {
			return createUser.key(username, encPass, trustLevel, false, subject)
					.map(userId -> {
						log.info("added user {} with trust level {}", username,
								trustLevel);
						return userId;
					});
		}

		/**
		 * Create a group if it doesn't already exist.
		 *
		 * @param name
		 *            Unique name of the group
		 * @param type
		 *            Type of group
		 * @param quota
		 *            Size of quota
		 * @return Whether a group was created.
		 */
		boolean createGroup(String name, GroupType type, Long quota) {
			return createGroup.call(name, quota, type) > 0;
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
			return userAuthorities.call1(userId).orElseThrow();
		}

		/**
		 * Tells the database that the user login worked. This is necessary
		 * because the DB can't perform the password check itself.
		 *
		 * @param userId
		 *            Who logged in?
		 */
		void noteLoginSuccessForUser(int userId) {
			long now = System.currentTimeMillis() / 1000;
			if (loginSuccess.call(now, null, userId) != 1) {
				log.warn("failed to note success for user {}", userId);
			}
		}

		/**
		 * Tells the database that the user login worked. This is necessary
		 * because the DB can't perform the password check itself.
		 *
		 * @param userId
		 *            Who logged in?
		 * @param subject
		 *            What is their OpenID {@code sub} (subject) claim?
		 */
		void noteLoginSuccessForUser(int userId, String subject) {
			long now = System.currentTimeMillis() / 1000;
			if (loginSuccess.call(now, subject, userId) != 1) {
				log.warn("failed to note success for user {}", userId);
			}
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
			long now = System.currentTimeMillis() / 1000;
			loginFailure.call(now, authProps.getMaxLoginFailures(), userId);
		}

		void unlock() {
			long now = System.currentTimeMillis() / 1000;
			unlock.call(authProps.getAccountLockDuration(), now);
		}
	}

	/**
	 * Collection of queries for setting the group memberships of a user. These
	 * are queries that would be part of {@link AuthQueries} except for the
	 * dance of handling temporary database tables.
	 * <p>
	 * Use this class by calling {@link #define(String,GroupType)} several
	 * times, and then calling {@link #apply(int)} once. Closing this resource
	 * removes temporary storage; it does not support nested use.
	 *
	 * @author Donal Fellows
	 */
	private final class GroupSynch extends AbstractSQL {
		private final Update insert;

		private final Update add;

		private final Update remove;

		GroupSynch(AuthQueries sql) {
			super(sql.getConnection());
			try (var make = conn.update(GROUP_SYNC_MAKE_TEMP_TABLE)) {
				make.call();
			} catch (BadSqlGrammarException e) {
				if (!e.getMessage().contains("already exists")) {
					throw e;
				}
				log.warn("Group temp table already exists (" + e.getMessage() + ")");
			}
			insert = conn.update(GROUP_SYNC_INSERT_TEMP_ROW);
			add = conn.update(GROUP_SYNC_ADD_GROUPS);
			remove = conn.update(GROUP_SYNC_REMOVE_GROUPS);
		}

		@Override
		public void close() {
			remove.close();
			add.close();
			insert.close();
			/*
			 * Seems we can only drop a temporary table if there are no (other)
			 * open statements that refer to it *AND* there is no transaction
			 * open that used it. Or is that no connection? Either way,
			 * SQLITE_LOCKED is not a desired failure state!
			 *
			 * https://sqlite.org/forum/forumpost/433d2fdb07fc8f13 says some of
			 * the constraints, but not all; the need for the transaction to be
			 * closed comes from elsewhere in that thread.
			 *
			 * Fortunately, we can just delete the contents of the temporary
			 * table instead, and that's just as good *and can be done in the
			 * transaction*.
			 */
			try (var drop = conn.update(GROUP_SYNC_DROP_TEMP_TABLE)) {
				drop.call();
			}
			super.close();
		}

		/**
		 * List one of the groups that we want a user to be a member of. We're
		 * building a set of these.
		 *
		 * @param name
		 *            The name of the group
		 * @param type
		 *            The type of the group
		 */
		void define(String name, GroupType type) {
			insert.call(name, type);
		}

		/**
		 * Apply the set of groups to a user. This will become (with minimal
		 * changes) the set of groups that they are members of.
		 *
		 * @param userId
		 *            What user are we talking about
		 */
		void apply(int userId) {
			int added = add.call(userId);
			int removed = remove.call(userId);
			if (added > 0 || removed > 0) {
				log.info("changed count of groups for user {}: +{}/-{}", userId,
						added, removed);
			}
		}
	}

	@Immutable
	static final class CollabratoryAuthority extends SimpleGrantedAuthority {
		private static final long serialVersionUID = 4964366746649162092L;

		private final String collabratory;

		CollabratoryAuthority(String collabClaimed) {
			super("COLLAB_" + collabClaimed);
			collabratory = collabClaimed;
		}

		String getCollabratory() {
			return collabratory;
		}
	}

	@Immutable
	static final class OrganisationAuthority extends SimpleGrantedAuthority {
		private static final long serialVersionUID = 8260068770503054502L;

		private final String organisation;

		OrganisationAuthority(String orgClaimed) {
			super("ORG_" + orgClaimed);
			organisation = orgClaimed;
		}

		String getOrganisation() {
			return organisation;
		}
	}

	private static final Pattern COLLAB_MATCHER =
			Pattern.compile("^collab-(.*)-(admin|editor|viewer)$");

	/**
	 * Convert a list of claimed collabs into authorities.
	 *
	 * @param claim
	 *            The (sub-)claim of collabs.
	 * @param results
	 *            Where to add the generated authorities.
	 * @return Whether we processed a claim at all. (The claim could be empty;
	 *         that's OK.)
	 */
	private boolean collabToAuthority(List<String> claim,
			Collection<GrantedAuthority> results) {
		if (isNull(claim)) {
			return false;
		}
		var seen = new HashSet<>();
		for (var collab : claim) {
			var reduced = COLLAB_MATCHER.matcher(collab).replaceFirst("$1");
			if (!seen.contains(reduced)) {
				results.add(new CollabratoryAuthority(reduced));
				seen.add(reduced);
			}
		}
		return true;
	}

	/**
	 * Convert a list of claimed organisations into authorities.
	 *
	 * @param claim
	 *            The claim of organisations.
	 * @param results
	 *            Where to add the generated authorities.
	 * @return Whether we processed a claim at all. (The claim could be empty;
	 *         that's OK.)
	 */
	private boolean orgToAuthority(List<String> claim,
			Collection<GrantedAuthority> results) {
		if (isNull(claim)) {
			return false;
		}
		for (var org : claim) {
			/*
			 * No special processing required; orgs start with / in name and are
			 * already guaranteed to be unique.
			 */
			results.add(new OrganisationAuthority(org));
		}
		return true;
	}

	/**
	 * Extract the {@code team/roles} sub-claim. This is <em>awful</em> because
	 * the promised types are all inferred and the <em>actual</em> types
	 * guaranteed by the way claims are encoded are more that we are taking an
	 * {@link Object} because {@link ClaimAccessor#getClaim(String)} is merely
	 * pinky-swearing that objects are of the right type, not enforcing it.
	 *
	 * @param rolesClaim
	 *            Overall claim.
	 * @return The {@code team/roles} sub-claim, provided it exists and really
	 *         looks like a list of strings.
	 */
	private static List<String>
			getTeamsFromClaim(Map<String, List<String>> rolesClaim) {
		// Messy; all the implicit types and hidden casts!
		try {
			var teamsClaim = rolesClaim.get("team");
			if (!teamsClaim.isEmpty()) {
				/*
				 * Check invokes a String instance method on the first item of
				 * the list; if this works, we're valid. It's pitched into
				 * log.trace so things don't complain, but we never expect to
				 * enable this.
				 */
				log.trace("team claim first item length",
						teamsClaim.get(0).length());
			}
			return teamsClaim;
		} catch (ClassCastException | NullPointerException
				| IndexOutOfBoundsException e) {
			log.debug("failed to convert claim", e);
		}
		return List.of();
	}

	private void mapAuthorities(String source, ClaimAccessor claimSet,
			Collection<GrantedAuthority> results) {
		if (!collabToAuthority(getTeamsFromClaim(claimSet.getClaim("roles")),
				results)) {
			log.warn("no team in {}", source);
		}
		if (!orgToAuthority(claimSet.getClaimAsStringList("unit"), results)) {
			log.warn("no unit in {}", source);
		}
		// All OpenID users get read-write access
		results.add(new SimpleGrantedAuthority(GRANT_READER));
		results.add(new SimpleGrantedAuthority(GRANT_USER));
	}

	@Override
	public void mapAuthorities(OidcUserAuthority user,
			Collection<GrantedAuthority> ga) {
		mapAuthorities("userinfo", user.getUserInfo(), ga);
	}

	@Immutable
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
		return ifElse(
				queries.transaction(() -> lookUpUserDetails(username, queries)),
				details -> {
					checkPassword(username, password, details, queries);
					// Succeeded; finalize into external form
					return queries.transaction(() -> {
						queries.noteLoginSuccessForUser(details.userId);
						// Convert tiered trust level to grant form
						details.trustLevel.getGrants()
								.forEach(authorities::add);
						return true;
					});
				}, () -> false);
	}

	/**
	 * Look up a local user. Does all checks <em>except</em> the password check;
	 * that's slow (because bcrypt) so we do it outside the transaction. This
	 * needs to be done in a writable (exclusive) transaction.
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
		return ifElse(queries.getUser(username), userInfo -> {
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
				var authInfo = queries.getUserAuthorities(userId);
				var encPass = authInfo.getString("encrypted_password");
				if (isNull(encPass)) {
					/*
					 * We know this user, but they can't use this authentication
					 * method. They'll probably have to use OpenID.
					 */
					return Optional.empty();
				}
				var trust = authInfo.getEnum("trust_level", TrustLevel.class);
				log.info("User trust level is " + trust);
				return Optional.of(new LocalAuthResult(userId, trust, encPass));
			} catch (AuthenticationException e) {
				queries.noteLoginFailureForUser(userId, username);
				log.info("login failure for {}", username, e);
				throw e;
			}
		}, Optional::empty);
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
	 */
	private void checkPassword(String username, String password,
			LocalAuthResult details, AuthQueries queries) {
		if (!passServices.matchPassword(password, details.passInfo)) {
			queries.transaction(() -> {
				queries.noteLoginFailureForUser(details.userId, username);
				log.info("login failure for {}: bad password", username);
				throw new BadCredentialsException("bad password");
			});
		}
	}

	/**
	 * Check if an OpenID user can use the service. A transaction <em>must</em>
	 * be held open when calling this.
	 *
	 * @param username
	 *            The username, already obtained and verified.
	 * @param subject
	 *            The real OpenID {@code sub} claim, if available/known.
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
	private boolean authOpenIDAgainstDB(String username, String subject,
			Collection<? extends GrantedAuthority> authorities,
			AuthQueries queries) {
		var collabs = new ArrayList<String>();
		var orgs = new ArrayList<String>();
		authorities.forEach(ga -> inflateGroup(ga, collabs, orgs, queries));
		return ifElse(queries.getUser(username), userInfo -> {
			log.info("Found user " + username + " in database");
			int userId = userInfo.getInt("user_id");
			synchExternalGroups(username, userId, orgs, collabs, queries);
			if (userInfo.getBoolean("disabled")) {
				log.info("user {} has a disabled account", username);
				throw new DisabledException("account is disabled");
			}
			try {
				if (userInfo.getBoolean("locked")) {
					// Note that this extends the lock!
					throw new LockedException("account is locked");
				}
				var authInfo = queries.getUserAuthorities(userId);
				if (nonNull(authInfo.getString("encrypted_password"))) {
					/*
					 * We know this user, but they can't use this authentication
					 * method. They'll probably have to use username+password.
					 */
					return false;
				}
				checkSubject(username, subject,
						authInfo.getString("openid_subject"));
				queries.noteLoginSuccessForUser(userId, subject);
				log.info("login success for {}", username);
				return true;
			} catch (AuthenticationException e) {
				queries.noteLoginFailureForUser(userId, username);
				log.info("login failure for {}", username, e);
				throw e;
			}
		}, () -> {
			/*
			 * No such user; need to inflate one now. If we successfully make
			 * the user, they're also immediately authorised as they're
			 * definitely not disabled or locked.
			 */
			return ifElse(queries.createUser(username, null, USER, subject),
					id -> {
						synchExternalGroups(username, id, orgs, collabs,
								queries);
						return true;
					}, () -> {
						// Can't note a failure; no record to note it in!
						log.warn("failed to make user {}", username);
						return false;
					});
		});
	}

	/**
	 * Check if the subject matches and issue a warning if there's a potential
	 * problem.
	 *
	 * @param username
	 *            The username (for logging).
	 * @param subject
	 *            The new user subject ID.
	 * @param oldSubject
	 *            The old user subject ID.
	 */
	private void checkSubject(String username, String subject,
			String oldSubject) {
		if (isNull(subject)) {
			log.warn("null subject for {}", username);
		} else if (subject.isBlank()) {
			log.warn("empty subject for {}", username);
		}
		if (nonNull(oldSubject)) {
			/*
			 * We know this user from before; double check that they're the same
			 * person as before. The {@code sub} claim is a true identifier.
			 */
			if (!requireNonNull(subject).equals(oldSubject)) {
				log.warn("user {} subject changed from {} to {}", username,
						oldSubject, subject);
			}
		}
	}

	private void inflateGroup(GrantedAuthority ga, List<String> collabs,
			List<String> orgs, AuthQueries queries) {
		if (ga instanceof CollabratoryAuthority) {
			var collab = (CollabratoryAuthority) ga;
			var collab1 = collab.getCollabratory();
			if (queries.createGroup(collab1, COLLABRATORY,
					quotaProps.getDefaultCollabQuota())) {
				log.info("created collabratory '{}'", collab1);
			}
			collabs.add(collab.getCollabratory());
		} else if (ga instanceof OrganisationAuthority) {
			var org = (OrganisationAuthority) ga;
			var org1 = org.getOrganisation();
			if (queries.createGroup(org1, ORGANISATION,
					quotaProps.getDefaultOrgQuota())) {
				log.info("created organisation '{}'", org1);
			}
			orgs.add(org.getOrganisation());
		}
	}

	/**
	 * Synchronise the lists of different types of groups for a user. Messy
	 * because we want to do minimal inserts and deletes.
	 *
	 * @param username
	 *            Which user (for logging only)
	 * @param userId
	 *            Which user (for DB access only)
	 * @param orgs
	 *            The list of organisation names
	 * @param collabs
	 *            The list of collab names
	 * @param queries
	 *            How to touch the DB
	 */
	private void synchExternalGroups(String username, int userId,
			List<String> orgs, List<String> collabs, AuthQueries queries) {
		if (orgs.isEmpty() && collabs.isEmpty()) {
			log.warn(
					"no organisations or collabratories for user {}; "
							+ "not synching in case this is a glitch "
							+ "(if this is the first time they've logged in, "
							+ "creating a job will fail in this session)",
					username);
			return;
		}
		try (var synch = new GroupSynch(queries)) {
			for (var org : orgs) {
				synch.define(org, ORGANISATION);
			}
			for (var collab : collabs) {
				synch.define(collab, COLLABRATORY);
			}
			synch.apply(userId);
		} catch (RuntimeException e) {
			log.warn("problem when synchronizing group memberships for {}",
					username, e);
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

	private void unlock() {
		try (var sql = new AuthQueries()) {
			sql.transaction(() -> {
				sql.unlock();
				return null;
			});
		}
	}

	interface TestAPI {
		/**
		 * Run the core of the user unlocker.
		 */
		void unlock();
	}

	/**
	 * @return The test interface.
	 * @deprecated This interface is just for testing.
	 */
	@Override
	@ForTestingOnly
	@RestrictedApi(explanation = "just for testing", link = "index.html",
			allowedOnPath = ".*/src/test/java/.*")
	@Deprecated
	public TestAPI getTestAPI() {
		ForTestingOnly.Utils.checkForTestClassOnStack();
		return new TestAPI() {
			@Override
			public void unlock() {
				LocalAuthProviderImpl.this.unlock();
			}
		};
	}
}
