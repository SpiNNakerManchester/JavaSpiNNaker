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
package uk.ac.manchester.spinnaker.nmpi.model;

/**
 * POJO describing an HBP Collaboratory.
 */
public class Collab {

	/**
	 * The content field.
	 */
	private String content;

	/**
	 * The collab ID.
	 */
	private int id;

	/**
	 * Get the content field.
	 *
	 * @return The content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Set the content field.
	 *
	 * @param contentParam The value to set.
	 */
	public void setContent(final String contentParam) {
		this.content = contentParam;
	}

	/**
	 * Get the ID of the collab.
	 *
	 * @return The collab ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the ID of the collab.
	 *
	 * @param idParam The collab ID
	 */
	public void setId(final int idParam) {
		this.id = idParam;
	}
}
