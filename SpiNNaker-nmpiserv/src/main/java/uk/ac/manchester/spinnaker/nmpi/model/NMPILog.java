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

import static java.util.Objects.isNull;

/**
 * A Neuromorphic Platform Interface log core.
 */
public class NMPILog {
	/**
	 * The content of the log.
	 */
	private StringBuilder buffer;

	public NMPILog(StringBuilder buffer) {
		this.buffer = buffer;
	}

	/**
	 * Gets the current log contents.
	 *
	 * @return The log contents, or {@code null} if the log is not yet
	 *         initialised.
	 */
	public String getContent() {
		if (isNull(buffer)) {
			return null;
		}
		return buffer.toString();
	}

	/**
	 * Set the content.
	 *
	 * @param content The content to set
	 */
	public void setContent(final String content) {
		this.buffer = new StringBuilder(content);
	}

	/**
	 * Append the string to the log.
	 *
	 * @param content
	 *            The string to append.
	 */
	public void appendContent(final String content) {
		if (isNull(this.buffer)) {
			this.buffer = new StringBuilder(content);
		} else {
			this.buffer.append(content);
		}
	}
}
