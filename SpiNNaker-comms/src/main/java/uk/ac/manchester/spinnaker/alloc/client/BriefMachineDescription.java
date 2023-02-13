/*
 * Copyright (c) 2021-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

import static uk.ac.manchester.spinnaker.alloc.client.ClientUtils.asDir;
import static uk.ac.manchester.spinnaker.alloc.client.ClientUtils.readOnlyCopy;

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
		this.uri = asDir(uri);
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	@JsonAlias("dead-boards")
	public void setDeadBoards(List<BoardCoords> deadBoards) {
		this.deadBoards = readOnlyCopy(deadBoards);
	}

	@JsonAlias("dead-links")
	public void setDeadLinks(List<DeadLink> deadLinks) {
		this.deadLinks = readOnlyCopy(deadLinks);
	}
}
