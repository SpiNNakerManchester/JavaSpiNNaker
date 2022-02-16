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
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Null;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The description and model of a group. POJO class.
 */
public final class GroupRecord {
	// TODO list group memberships
	private Integer groupId;

	private String groupName;

	private Long quota;

	private boolean internal;

	private List<UserRecord> members = emptyList();

	/**
	 * @return The group identifier. Read-only; cannot be set by the service.
	 */
	@JsonInclude(NON_NULL)
	@Null
	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	/**
	 * @return The group's name.
	 */
	@NotBlank
	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/**
	 * @return The quota of the group in board-seconds, if one exists.
	 */
	public Optional<Long> getQuota() {
		return Optional.of(quota);
	}

	public void setQuota(Long quota) {
		this.quota = quota;
	}

	/** @return The members of the group, if populated. */
	public List<UserRecord> getMembers() {
		return unmodifiableList(members);
	}

	public void setMembers(List<UserRecord> members) {
		this.members = members;
	}

	/** @return Whether this is an internally-defined group. */
	public boolean isInternal() {
		return internal;
	}

	public void setInternal(boolean internal) {
		this.internal = internal;
	}
}
