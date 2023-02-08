/*
 * Copyright (c) 2022 The University of Manchester
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

	/**
	 * @param id
	 *            The ID of this membership. Distinct from both the user and the
	 *            group.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/** @return The ID of the group that this is a membership of. */
	public int getGroupId() {
		return groupId;
	}

	/** @param groupId The ID of the group that this is a membership of. */
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	/** @return The name of the group that this is a membership of. */
	public String getGroupName() {
		return groupName;
	}

	/** @param groupName The name of the group that this is a membership of. */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/** @return The URL for the group that this is a membership of. */
	public URI getGroupUrl() {
		return groupUrl;
	}

	/** @param groupUrl The URL for the group that this is a membership of. */
	public void setGroupUrl(URI groupUrl) {
		this.groupUrl = groupUrl;
	}

	/** @return The ID of the user that this is a membership of. */
	public int getUserId() {
		return userId;
	}

	/** @param userId The ID of the user that this is a membership of. */
	public void setUserId(int userId) {
		this.userId = userId;
	}

	/** @return The name of the user that this is a membership of. */
	public String getUserName() {
		return userName;
	}

	/** @param userName The name of the user that this is a membership of. */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/** @return The URL for the user that this is a membership of. */
	public URI getUserUrl() {
		return userUrl;
	}

	/** @param userUrl The URL for the user that this is a membership of. */
	public void setUserUrl(URI userUrl) {
		this.userUrl = userUrl;
	}
}
