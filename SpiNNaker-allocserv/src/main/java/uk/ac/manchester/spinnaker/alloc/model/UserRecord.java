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
import static java.util.Collections.emptyMap;

import java.time.Instant;
import java.util.Map;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;

/**
 * The description and model of a user. POJO class. Some things are stated
 * to be not settable despite having setters; they're settable <em>in
 * instances of this class</em> but the service itself will not respect
 * being asked to change them.
 */
public final class UserRecord {
	private Integer userId;

	private String userName;

	private String password;

	private Boolean hasPassword;

	private Boolean isEnabled;

	private Boolean isLocked;

	private TrustLevel trustLevel;

	private Map<String, Long> quota;

	private Instant lastSuccessfulLogin;

	private Instant lastFailedLogin;

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
	 * @return The user's unencrypted password. <em>Never</em> returned by
	 *         the service, but may be written.
	 */
	@JsonInclude(NON_NULL)
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return Whether the user has a password set. If they don't, they'll
	 *         have to log in by other mechanisms (e.g., HBP/EBRAINS OpenID
	 *         Connect).
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
	 *         should unlock automatically after 24 hours. Can be explicitly
	 *         set to {@code false} to force an unlock.
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
	 * @return The quota map of the account, saying how many board-seconds
	 *         can be used on each machine.
	 */
	public Map<String, Long> getQuota() {
		return quota;
	}

	public void setQuota(Map<String, Long> quota) {
		this.quota = quota;
	}

	/**
	 * @return The time that the last successful login was. Read-only;
	 *         cannot be set via the admin API.
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
	 * @return The time that the last failed login was. Read-only; cannot be
	 *         set via the admin API.
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
	 * @return Whether this represents a request to use external
	 *         authentication (instead of just not setting the password).
	 */
	@JsonIgnore
	public boolean isExternallyAuthenticated() {
		return password == null && hasPassword != null && hasPassword;
	}

	boolean isPasswordSet() {
		return password != null && !password.trim().isEmpty();
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
		if (password != null) {
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
		if (getTrustLevel() == null) {
			setTrustLevel(TrustLevel.USER);
		}
		if (getQuota() == null) {
			setQuota(emptyMap());
		}
		if (isEnabled() == null) {
			setEnabled(true);
		}
		if (isLocked() == null) {
			setLocked(false);
		}
	}
}
