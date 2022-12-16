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

import javax.validation.constraints.NotEmpty;

/**
 * Information about the proxy to allow connection.
 *
 * @param spallocUrl
 *            The URL of the spalloc server to connect to.
 * @param jobUrl
 *            The URL of the job to connect to.
 * @param bearerToken
 *            The bearer token to use as authentication.
 */
public record ProxyInformation(@NotEmpty String spallocUrl,
		@NotEmpty String jobUrl, @NotEmpty String bearerToken) {
}
