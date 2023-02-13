/*
 * Copyright (c) 2021-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.alloc.security.TrustLevel.USER;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * The description and model of a user. POJO class. Some things are stated to be
 * not settable despite having setters; they're settable <em>in instances of
 * this class</em> but the service itself will not respect being asked to change
 * them.
 */
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public final class UserRecord {
	private Integer userId;

	private String userName;

	private String password;

	private Boolean hasPassword;

	private Boolean enabled;

	private Boolean locked;

	private TrustLevel trustLevel;

	private Instant lastSuccessfulLogin;

	private Instant lastFailedLogin;

	private String openIdSubject;

	private boolean isInternal;

	private Map<String, URI> groups;

	/** Create an empty instance. */
	public UserRecord() {
	}

	/**
	 * Inflate the result of a {@link SQLQueries#GET_USER_DETAILS} query (or
	 * anything else that includes the same columns) into an object. Doesn't
	 * include inflating the groups that the user is a member of.
	 *
	 * @param row
	 *            The row with the result.
	 */
	@UsedInJavadocOnly(SQLQueries.class)
	public UserRecord(Row row) {
		try {
			setUserId(row.getInt("user_id"));
			setUserName(row.getString("user_name"));
			setHasPassword(row.getBoolean("has_password"));
			setTrustLevel(row.getEnum("trust_level", TrustLevel.class));
			setEnabled(!row.getBoolean("disabled"));
			setLocked(row.getBoolean("locked"));
			setLastSuccessfulLogin(
					row.getInstant("last_successful_login_timestamp"));
			setLastFailedLogin(row.getInstant("last_fail_timestamp"));
			setOpenIdSubject(row.getString("openid_subject"));
			isInternal = row.getBoolean("is_internal");
		} finally {
			// I mean it!
			setPassword(null);
		}
	}

	/**
	 * @return The user identifier. Read-only; cannot be set by the service.
	 */
	@JsonInclude(NON_NULL)
	@Null
	public Integer getUserId() {
		return userId;
	}

	/**
	 * @param userId
	 *            The user identifier. Read-only within the administration
	 *            interface; cannot be set by the service.
	 */
	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	/**
	 * @return The user's username.
	 */
	@NotBlank
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName
	 *            The user's username.
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return The user's unencrypted password. <em>Never</em> returned by the
	 *         service, but may be written.
	 */
	@JsonInclude(NON_NULL)
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            The user's unencrypted password. <em>Never</em> returned by
	 *            the service, but may be written.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return Whether the user has a password set. If they don't, they'll have
	 *         to log in by other mechanisms (e.g., HBP/EBRAINS OpenID Connect).
	 */
	public Boolean getHasPassword() {
		return hasPassword;
	}

	/**
	 * @param hasPassword
	 *            Whether the user has a password set. If they don't, they'll
	 *            have to log in by other mechanisms (e.g., HBP/EBRAINS OpenID
	 *            Connect).
	 */
	public void setHasPassword(Boolean hasPassword) {
		this.hasPassword = hasPassword;
	}

	/**
	 * @return Whether this account is enabled. Disabled accounts
	 *         <em>cannot</em> log into the service until explicitly enabled.
	 */
	public Boolean getEnabled() {
		return enabled;
	}

	/**
	 * @param enabled
	 *            Whether this account is enabled. Disabled accounts
	 *            <em>cannot</em> log into the service until explicitly enabled.
	 */
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @return Whether this account is temporarily locked. Locked accounts
	 *         should unlock automatically after 24 hours. Can be explicitly set
	 *         to {@code false} to force an unlock.
	 */
	public Boolean getLocked() {
		return locked;
	}

	/**
	 * @param locked
	 *            Whether this account is temporarily locked. Locked accounts
	 *            should unlock automatically after 24 hours. Can be explicitly
	 *            set to {@code false} to force an unlock.
	 */
	public void setLocked(Boolean locked) {
		this.locked = locked;
	}

	/**
	 * @return The permissions of the account.
	 */
	@NotNull(message = "a trust level must be given")
	public TrustLevel getTrustLevel() {
		return trustLevel;
	}

	/**
	 * @param trustLevel The permissions of the account.
	 */
	public void setTrustLevel(TrustLevel trustLevel) {
		this.trustLevel = trustLevel;
	}

	/**
	 * @return The time that the last successful login was. Read-only; cannot be
	 *         set via the admin API.
	 */
	@JsonInclude(NON_NULL)
	@Null
	public Instant getLastSuccessfulLogin() {
		return lastSuccessfulLogin;
	}

	void setLastSuccessfulLogin(Instant timestamp) {
		this.lastSuccessfulLogin = timestamp;
	}

	/**
	 * @return The time that the last failed login was. Read-only; cannot be set
	 *         via the admin API.
	 */
	@JsonInclude(NON_NULL)
	@Null
	public Instant getLastFailedLogin() {
		return lastFailedLogin;
	}

	void setLastFailedLogin(Instant timestamp) {
		this.lastFailedLogin = timestamp;
	}

	/**
	 * Note that no API that sets a user's information using a
	 * {@link UserRecord} allows setting the groups that user is a member of at
	 * the same time.
	 *
	 * @return The groups that the user is a member of. May be {@code null} if
	 *         this information is not being reported.
	 */
	public Map<String, URI> getGroups() {
		return groups;
	}

	/**
	 * @param groups The groups that the user is a member of.
	 */
	public void setGroups(Map<String, URI> groups) {
		this.groups = groups;
	}

	/** @return The OpenID subject that this user relates to, if known. */
	public String getOpenIdSubject() {
		return openIdSubject;
	}

	/** @param subject The OpenID subject that this user relates to. */
	public void setOpenIdSubject(String subject) {
		this.openIdSubject = subject;
	}

	/** @return Whether this is an internal user. */
	@JsonIgnore
	public boolean isInternal() {
		return isInternal;
	}

	/** @param internal Whether this is an internal user. */
	public void setInternal(boolean internal) {
		this.isInternal = internal;
	}

	@JsonIgnore
	private boolean isPasswordSetForAccount() {
		return nonNull(hasPassword) && hasPassword;
	}

	/**
	 * @return Whether this represents a request to use external authentication
	 *         (instead of just not setting the password).
	 */
	@JsonIgnore
	public boolean isExternallyAuthenticated() {
		return !isInternal;
	}

	@JsonIgnore
	boolean isPasswordSet() {
		return nonNull(password) && !password.isBlank();
	}

	@AssertTrue(message = "only set a password for non-OpenID accounts")
	@JsonIgnore
	boolean isAuthenticationSane() {
		if (isInternal) {
			return isPasswordSet() || isPasswordSetForAccount();
		} else {
			return !isPasswordSet();
		}
	}

	/**
	 * Forces correct shrouding of information.
	 *
	 * @return the object (for convenience)
	 */
	@CanIgnoreReturnValue
	public UserRecord sanitise() {
		// Make SURE that the password doesn't go back
		if (nonNull(password)) {
			hasPassword = true;
			password = null;
		}
		// Never need to send the userId back
		userId = null;
		return this;
	}

	/**
	 * Set up some defaults used when a user is being created.
	 */
	public void initCreationDefaults() {
		setUserId(null);
		if (isNull(getTrustLevel())) {
			setTrustLevel(USER);
		}
		if (isNull(getEnabled())) {
			setEnabled(true);
		}
		if (isNull(getLocked())) {
			setLocked(false);
		}
	}
}
