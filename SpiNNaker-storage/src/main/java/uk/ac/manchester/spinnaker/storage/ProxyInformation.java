/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage;

import java.util.Map;

import javax.validation.constraints.NotEmpty;

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
