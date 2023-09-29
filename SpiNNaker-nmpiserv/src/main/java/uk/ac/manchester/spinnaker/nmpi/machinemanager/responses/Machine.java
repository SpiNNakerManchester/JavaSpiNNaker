/*
 * Copyright (c) 2014 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.nmpi.machinemanager.responses;

import java.util.List;

/**
 * Describes a machine by its name, tags, width and height.
 */
public class Machine {
	/** The name of the machine. */
	private String name;

	/** The list of tags associated with the machine. */
	private List<String> tags;

	/** The width of the machine. */
	private int width;

	/** The height of the machine. */
	private int height;

	/**
	 * Get the name of the machine.
	 *
	 * @return The name of the machine
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the machine.
	 *
	 * @param name
	 *            The name of the machine to set
	 */
	void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the tags associated with the machine.
	 *
	 * @return The tags
	 */
	public List<String> getTags() {
		return tags;
	}

	/**
	 * Set the tags associated with the machine.
	 *
	 * @param tags
	 *            The tags to set
	 */
	void setTags(List<String> tags) {
		this.tags = tags;
	}

	/**
	 * Get the width of the machine.
	 *
	 * @return The width in chips
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Set the width of the machine.
	 *
	 * @param width
	 *            The width in chips
	 */
	void setWidth(int width) {
		this.width = width;
	}

	/**
	 * Get the height of the machine.
	 *
	 * @return The height in chips
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Set the height of the machine.
	 *
	 * @param height
	 *            The height in chips
	 */
	void setHeight(int height) {
		this.height = height;
	}
}
