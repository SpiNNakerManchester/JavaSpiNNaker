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
