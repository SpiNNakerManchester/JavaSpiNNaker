/*
 * Copyright (c) 2022 The University of Manchester
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

import java.net.URI;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Description of the membership of one user in one group.
 *
 * @see UserRecord
 * @see GroupRecord
 * @author Donal Fellows
 */
@JsonInclude(NON_NULL)
public class MemberRecord {
	private int id;

	private int groupId;

	private String groupName;

	private URI groupUrl;

	private int userId;

	@NotBlank
	private String userName;

	private URI userUrl;

	/**
	 * @return The ID of this membership. Distinct from both the user and the
	 *         group.
	 */
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/** @return The ID of the group that this is a membership of. */
	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	/** @return The name of the group that this is a membership of. */
	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/** @return The URL for the group that this is a membership of. */
	public URI getGroupUrl() {
		return groupUrl;
	}

	public void setGroupUrl(URI groupUrl) {
		this.groupUrl = groupUrl;
	}

	/** @return The ID of the user that this is a membership of. */
	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	/** @return The name of the user that this is a membership of. */
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	/** @return The URL for the user that this is a membership of. */
	public URI getUserUrl() {
		return userUrl;
	}

	public void setUserUrl(URI userUrl) {
		this.userUrl = userUrl;
	}
}
