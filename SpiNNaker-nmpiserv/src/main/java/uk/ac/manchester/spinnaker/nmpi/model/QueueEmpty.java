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
 * A message indicating that the queue is empty.
 */
public class QueueEmpty implements QueueNextResponse {
	/** Any warning returned. */
	private String warning;

	/**
	 * Get any warning returned.
	 *
	 * @return The warning
	 */
	public String getWarning() {
		return warning;
	}

	/**
	 * Set the warning.
	 *
	 * @param warning
	 *            The warning to set
	 */
	public void setWarning(final String warning) {
		this.warning = warning;
	}
}
