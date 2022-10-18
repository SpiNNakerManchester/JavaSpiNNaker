/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc.messages;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes a machine by its name, tags, width and height.
 */
public class Machine {
	private String name;

	private List<String> tags = List.of();

	private int width;

	private int height;

	private List<BoardCoordinates> deadBoards = List.of();

	private List<BoardLink> deadLinks = List.of();

	/**
	 * @param name
	 *            The name of the machine.
	 * @param tags
	 *            The tags on the machine.
	 * @param width
	 *            The width of the machine.
	 * @param height
	 *            The height of the machine.
	 * @param deadBoards
	 *            The dead boards on the machine.
	 * @param deadLinks
	 *            The dead links on the machine.
	 */
	public Machine(@JsonProperty("name") String name,
			@JsonProperty("tags") List<String> tags,
			@JsonProperty("width") int width,
			@JsonProperty("height") int height,
			@JsonProperty("dead_boards") List<BoardCoordinates> deadBoards,
			@JsonProperty("dead_links") List<BoardLink> deadLinks) {
		this.name = name;
		this.tags = tags == null ? List.of() : List.copyOf(tags);
		this.width = width;
		this.height = height;
		this.deadBoards =
				deadBoards == null ? List.of() : List.copyOf(deadBoards);
		this.deadLinks = deadLinks == null ? List.of() : List.copyOf(deadLinks);
	}

	/** @return The name of the machine. */
	public String getName() {
		return name;
	}

	/** @return The tags on the machine. */
	public List<String> getTags() {
		return tags;
	}

	/** @return The width of the machine. */
	public int getWidth() {
		return width;
	}

	/** @return The height of the machine. */
	public int getHeight() {
		return height;
	}

	/** @return The dead boards on the machine. */
	public List<BoardCoordinates> getDeadBoards() {
		return deadBoards;
	}

	/** @return The dead links on the machine. */
	public List<BoardLink> getDeadLinks() {
		return deadLinks;
	}

	@Override
	public String toString() {
		return name + " " + width + "," + height + " # tags: " + tags.size()
				+ " deadBoards: " + deadBoards.size() + " deadLinks: "
				+ deadLinks.size();
	}
}
