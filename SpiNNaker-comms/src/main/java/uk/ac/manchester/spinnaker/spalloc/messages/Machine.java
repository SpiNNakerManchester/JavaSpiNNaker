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

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;

import java.util.List;

/**
 * Describes a machine by its name, tags, width and height.
 */
public class Machine {
	private String name;

	private List<String> tags = emptyList();

	private int width;

	private int height;

	private List<BoardCoordinates> deadBoards = emptyList();

	private List<BoardLink> deadLinks = emptyList();

	/** @return The name of the machine. */
	public String getName() {
		return name;
	}

	/** @param name The name of the machine. */
	public void setName(String name) {
		this.name = name;
	}

	/** @return The tags on the machine. */
	public List<String> getTags() {
		return tags;
	}

	/** @param tags The tags on the machine. */
	public void setTags(List<String> tags) {
		this.tags = isNull(tags) ? emptyList() : unmodifiableList(tags);
	}

	/** @return The width of the machine. */
	public int getWidth() {
		return width;
	}

	/** @param width The width of the machine. */
	public void setWidth(int width) {
		this.width = width;
	}

	/** @return The height of the machine. */
	public int getHeight() {
		return height;
	}

	/** @param height The height of the machine. */
	public void setHeight(int height) {
		this.height = height;
	}

	/** @return The dead boards on the machine. */
	public List<BoardCoordinates> getDeadBoards() {
		return deadBoards;
	}

	/** @param deadBoards The dead boards on the machine. */
	public void setDeadBoards(List<BoardCoordinates> deadBoards) {
		this.deadBoards = deadBoards;
	}

	/** @return The dead links on the machine. */
	public List<BoardLink> getDeadLinks() {
		return deadLinks;
	}

	/** @param deadLinks The dead links on the machine. */
	public void setDeadLinks(List<BoardLink> deadLinks) {
		this.deadLinks = deadLinks;
	}

	@Override
	public String toString() {
		return name + " " + width + "," + height + " # tags: " + tags.size()
				+ " deadBoards: " + deadBoards.size() + " deadLinks: "
				+ deadLinks.size();
	}
}
