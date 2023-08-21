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
package uk.ac.manchester.spinnaker.nmpi.model.job.nmpi;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * A reference to some data to be moved into or out of a {@link Job}.
 */
public class DataItem {
	/** The item URL. */
	private String url;

	/**
	 * Creates an empty item of data.
	 */
	public DataItem() {
		// Does Nothing
	}

	/**
	 * Make an instance that wraps a URL. The meaning of the URL depends on the
	 * usage of the data item.
	 *
	 * @param url
	 *            The URL to wrap.
	 */
	public DataItem(String url) {
		this.url = url;
	}

	/**
	 * Get the URL of the item of data.
	 *
	 * @return The URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the URL of the item of data.
	 *
	 * @param url
	 *            The URL
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Setter that ignores other properties.
	 *
	 * @param key
	 *            The key of the property
	 * @param value
	 *            The value of the property
	 * @hidden
	 */
	@JsonAnySetter
	public void ignoreExtra(String key, String value) {
		// Ignore anything else
	}
}
