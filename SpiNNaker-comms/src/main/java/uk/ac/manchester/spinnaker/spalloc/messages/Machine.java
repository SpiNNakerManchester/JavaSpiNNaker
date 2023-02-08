/*
 * Copyright (c) 2018 The University of Manchester
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
