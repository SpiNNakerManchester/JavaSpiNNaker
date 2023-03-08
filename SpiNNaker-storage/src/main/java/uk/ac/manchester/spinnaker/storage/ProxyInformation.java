/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/**
 * Information about the proxy to allow connection.
 */
public class ProxyInformation {
	/**
	 * The URL of the spalloc server to connect to.
	 */
	@NotEmpty
	public final String spallocUrl;

	/**
	 * The URL of the job to connect to.
	 */
	@NotEmpty
	public final String jobUrl;

	/**
	 * The headers to use for authentication.
	 */
	@NotEmpty
	public final Map<String, String> headers;

	/**
	 * The cookies to use for authentication.
	 */
	@NotEmpty
	public final Map<String, String> cookies;

	/**
	 * Create a new instance.
	 *
	 * @param spallocUrl
	 *            The URL of the Spalloc server.
	 * @param jobUrl
	 *            The URL of the job.
	 * @param headers
	 *            The headers to use for authentication.
	 * @param cookies
	 *            The cookies to use for authentication.
	 */
	public ProxyInformation(String spallocUrl, String jobUrl,
			Map<String, String> headers, Map<String, String> cookies) {
		this.spallocUrl = spallocUrl;
		this.jobUrl = jobUrl;
		this.headers = headers;
		this.cookies = cookies;
	}
}
