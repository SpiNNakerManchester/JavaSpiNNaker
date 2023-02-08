/*
 * Copyright (c) 2021 The University of Manchester
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

import static java.util.Objects.requireNonNull;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

/**
 * Describes basic information about a user that they'd use to change their
 * password.
 *
 * @author Donal Fellows
 */
public class PasswordChangeRecord {
	private int userId;

	private String username;

	private String oldPassword;

	private String newPassword;

	private String newPassword2;

	/** Make an instance. */
	public PasswordChangeRecord() {
		this.userId = -1;
		this.username = "";
		this.oldPassword = "";
		this.newPassword = "";
		this.newPassword2 = "";
	}

	/**
	 * Make an instance.
	 *
	 * @param userId
	 *            The user ID.
	 * @param username
	 *            The user name.
	 */
	public PasswordChangeRecord(int userId, String username) {
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

	/**
	 * @param userId the user id
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}

	/**
	 * @return the username
	 */
	public final String getUsername() {
		return username;
	}

	/**
	 * @param username the username
	 */
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

	/**
	 * @param password the old password
	 */
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

	/**
	 * @param newPassword the first copy of the new password
	 */
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

	/**
	 * @param newPassword2 the second copy of the new password
	 */
	public void setNewPassword2(String newPassword2) {
		this.newPassword2 = newPassword2;
	}

	/** @return whether the password is being changed at all */
	@AssertFalse(message = "old and new passwords must be different")
	public boolean isNewPasswordSameAsOld() {
		return newPassword.equals(oldPassword);
	}

	/** @return whether the two copies of the new password are the same */
	@AssertTrue(
			message = "second copy of new password must be same as first")
	public boolean isNewPasswordMatched() {
		return newPassword.equals(newPassword2);
	}
}
