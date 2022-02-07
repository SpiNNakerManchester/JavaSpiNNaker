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

import static java.util.stream.Collectors.joining;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import uk.ac.manchester.spinnaker.alloc.db.Row;

/**
 * Describes a tagging of a machine.
 *
 * @author Donal Fellows
 */
public class MachineTagging {
	private String name;

	private int id;

	private URI url;

	private Set<String> tags = new HashSet<>();

	public MachineTagging() {
	}

	public MachineTagging(Row row) {
		id = row.getInt("machine_id");
		name = row.getString("machine_name");
		// Tags can't be handled this way
	}

	/** @return The name of the machine. */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/** @return The ID of the machine. */
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/** @return The URL to the page about the machine. */
	public URI getUrl() {
		return url;
	}

	public void setUrl(URI url) {
		this.url = url;
	}

	/** @return The tags of the machine. */
	public String getTags() {
		return tags.stream().collect(joining(", "));
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	/**
	 * @return Whether this machine has the "{@code default}" tag applied to it.
	 */
	public boolean isTaggedAsDefault() {
		return tags.contains("default");
	}
}
