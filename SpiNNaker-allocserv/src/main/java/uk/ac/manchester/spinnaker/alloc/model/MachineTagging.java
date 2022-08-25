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
import static java.util.stream.Collectors.joining;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.utils.MappableIterable;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Describes a tagging of a machine.
 *
 * @author Donal Fellows
 */
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public class MachineTagging {
	private String name;

	private int id;

	private URI url;

	private Set<String> tags = new HashSet<>();

	MachineTagging() {
	}

	/**
	 * Build a basic instance (without tags, which need another query) from the
	 * result of {@link SQLQueries#GET_ALL_MACHINES}.
	 *
	 * @param row
	 *            The database row.
	 */
	@UsedInJavadocOnly(SQLQueries.class)
	public MachineTagging(Row row) {
		id = row.getInt("machine_id");
		name = row.getString("machine_name");
		// Tags can't be handled this way
		// Url can't be handled now; lack the context
	}

	/** @return The name of the machine. */
	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	/** @return The ID of the machine. */
	public int getId() {
		return id;
	}

	void setId(int id) {
		this.id = id;
	}

	/** @return The URL to the page about the machine. */
	public URI getUrl() {
		return url;
	}

	/** @param url The URL to the page about the machine. */
	public void setUrl(URI url) {
		this.url = url;
	}

	/** @return The tags of the machine. */
	public String getTags() {
		return tags.stream().collect(joining(", "));
	}

	/** @param tags The tags of the machine. */
	void setTags(Set<String> tags) {
		this.tags = tags;
	}

	/** @param tags The tags of the machine. */
	public void setTags(MappableIterable<String> tags) {
		this.tags = tags.toSet();
	}

	/**
	 * @return Whether this machine has the "{@code default}" tag applied to it.
	 */
	public boolean isTaggedAsDefault() {
		return tags.contains("default");
	}
}
