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
package uk.ac.manchester.spinnaker.alloc.client;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

class BriefMachineDescription {
	/** The machine name. */
	String name;

	/** The tags of the machine. */
	List<String> tags;

	/** The URI to the machine. */
	URI uri;

	/** The width of the machine, in triads. */
	int width;

	/** The height of the machine, in triads. */
	int height;

	/** The dead boards of the machine. */
	List<BoardCoords> deadBoards;

	/** The dead links of the machine. */
	List<DeadLink> deadLinks;

	public void setName(String name) {
		this.name = name;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public void setUri(URI uri) {
		this.uri = SpallocClientFactory.asDir(uri);
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	@JsonAlias("dead-boards")
	public void setDeadBoards(List<BoardCoords> deadBoards) {
		this.deadBoards = deadBoards;
	}

	@JsonAlias("dead-links")
	public void setDeadLinks(List<DeadLink> deadLinks) {
		this.deadLinks = deadLinks;
	}
}
