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
package uk.ac.manchester.spinnaker.nmpiexec.jobprocess;

import java.util.List;

/**
 * A single item of provenance data.
 */
public class ProvenanceItem {
	/** The path of the item. */
	private final List<String> path;

	/** The value if the item. */
	private final String value;

	/**
	 * Create a provenance item.
	 *
	 * @param path
	 *            The location of the item in the provenance tree.
	 * @param value
	 *            The content of the value.
	 */
	public ProvenanceItem(final List<String> path, final String value) {
		this.path = path;
		this.value = value;
	}

	/**
	 * Get the path to the item.
	 *
	 * @return The path
	 */
	public List<String> getPath() {
		return path;
	}

	/**
	 * Get the value of the item.
	 *
	 * @return The value
	 */
	public String getValue() {
		return value;
	}
}
