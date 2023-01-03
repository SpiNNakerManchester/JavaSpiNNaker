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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Collections.unmodifiableMap;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import uk.ac.manchester.spinnaker.alloc.db.Row;

/**
 * The description and model of a group. POJO class; changes not automatically
 * reflected in the DB.
 */
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public final class GroupRecord {
	/** The type of a group. */
	public enum GroupType {
		/** Marks a group that can contain local users. */
		INTERNAL,
		/** Marks a group that represents a real-world organisation. */
		ORGANISATION,
		/** Marks an EBRAINS/HBP collabratory. */
		COLLABRATORY;

		/** @return Whether this is an internal group. */
		public boolean isInternal() {
			return ordinal() == 0;
		}
	}

	private Integer groupId;

	private String groupName;

	private Long quota;

	private GroupType type;

	private Map<String, URI> members;

	/** Make an empty record. */
	public GroupRecord() {
	}

	/**
	 * Make a record from a database query result.
	 *
	 * @param row
	 *            The query result.
	 */
	public GroupRecord(Row row) {
		setGroupId(row.getInteger("group_id"));
		setGroupName(row.getString("group_name"));
		setQuota(row.getLong("quota"));
		setType(row.getEnum("group_type", GroupType.class));
	}

	/**
	 * @return The group identifier. Read-only; cannot be set by the service.
	 */
	@JsonInclude(NON_NULL)
	@Null
	public Integer getGroupId() {
		return groupId;
	}

	void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	/** @return The group's name. */
	@NotBlank
	public String getGroupName() {
		return groupName;
	}

	/** @param groupName The group's name. */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/** @return The quota of the group in board-seconds, if one exists. */
	public Optional<Long> getQuota() {
		return Optional.ofNullable(quota);
	}

	/** @param quota The quota of the group in board-seconds, if one exists. */
	public void setQuota(Long quota) {
		this.quota = quota;
	}

	/** @return What type of group is this. */
	public GroupType getType() {
		return type;
	}

	void setType(GroupType type) {
		this.type = type;
	}

	/** @return The members of the group, if populated. */
	public Map<String, URI> getMembers() {
		return members;
	}

	/** @param members The members of the group. */
	public void setMembers(Map<String, URI> members) {
		this.members = unmodifiableMap(members);
	}
}
