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
package uk.ac.manchester.spinnaker.alloc.model;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;

/**
 * The description and model of a user. POJO class. Some things are stated to be
 * not settable despite having setters; they're settable <em>in instances of
 * this class</em> but the service itself will not respect being asked to change
 * them.
 */
public final class UserRecord {
	private Integer userId;

	private String userName;

	private String password;

	private Boolean hasPassword;

	private Boolean isEnabled;

	private Boolean isLocked;

	private TrustLevel trustLevel;

	private Instant lastSuccessfulLogin;

	private Instant lastFailedLogin;

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

	public void setHasPassword(Boolean hasPassword) {
		this.hasPassword = hasPassword;
	}

	/**
	 * @return Whether this account is enabled. Disabled accounts
	 *         <em>cannot</em> use the service until explicitly enabled.
	 */
	public Boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(Boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	/**
	 * @return Whether this account is temporarily locked. Locked accounts
	 *         should unlock automatically after 24 hours. Can be explicitly set
	 *         to {@code false} to force an unlock.
	 */
	public Boolean isLocked() {
		return isLocked;
	}

	public void setLocked(Boolean isLocked) {
		this.isLocked = isLocked;
	}

	/**
	 * @return The permissions of the account.
	 */
	@NotNull(message = "a trust level must be given")
	public TrustLevel getTrustLevel() {
		return trustLevel;
	}

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

	public void setLastSuccessfulLogin(Instant timestamp) {
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

	public void setLastFailedLogin(Instant timestamp) {
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

	public void setGroups(Map<String, URI> groups) {
		this.groups = groups;
	}

	/**
	 * @return Whether this represents a request to use external authentication
	 *         (instead of just not setting the password).
	 */
	@JsonIgnore
	public boolean isExternallyAuthenticated() {
		return isNull(password) && nonNull(hasPassword) && hasPassword;
	}

	boolean isPasswordSet() {
		return nonNull(password) && !password.trim().isEmpty();
	}

	@AssertTrue(message = "either set a password or mark for using OpenID")
	boolean isAuthenticationSane() {
		return isPasswordSet() || isExternallyAuthenticated();
	}

	/**
	 * Forces correct shrouding of information.
	 *
	 * @return the object (for convenience)
	 */
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
		if (isNull(isEnabled())) {
			setEnabled(true);
		}
		if (isNull(isLocked())) {
			setLocked(false);
		}
	}
}
