/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.messages.model.Version;

/** The version of the service. */
@Service
public class ServiceVersion {
	private String fullVersion;

	private Version v;

	public ServiceVersion(@Value("${version}") String version) {
		fullVersion = version;
		v = new Version(version.replaceAll("-.*", ""));
	}

	public Version getVersion() {
		return v;
	}

	public String getFullVersion() {
		return fullVersion;
	}
}
