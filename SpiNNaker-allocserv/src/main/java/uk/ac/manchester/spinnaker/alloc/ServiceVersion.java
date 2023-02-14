/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_SUPPORT;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.messages.model.Version;

/** The version information of the service. */
@Service
@Role(ROLE_SUPPORT)
public class ServiceVersion {
	private String fullVersion;

	private Version v;

	private String buildTimestamp;

	/**
	 * Create an instance.
	 *
	 * @param version
	 *            The version, injected from the POM by the build process.
	 * @param buildTimestamp
	 *            The build timestamp, injected from the POM by the build
	 *            process.
	 */
	public ServiceVersion(@Value("${version}") String version,
			@Value("${build-timestamp}") String buildTimestamp) {
		fullVersion = version;
		v = new Version(version.replaceAll("-.*", ""));
		this.buildTimestamp = buildTimestamp;
	}

	/**
	 * @return The logical version number. Expected to at least
	 *         <em>approximately</em> follow semver rules.
	 */
	public Version getVersion() {
		return v;
	}

	/**
	 * @return The full version identifier. Conforms only to Maven conventions.
	 */
	public String getFullVersion() {
		return fullVersion;
	}

	/**
	 * @return The build timestamp. The format of this is not intended for
	 *         further machine parsing.
	 */
	public String getBuildTimestamp() {
		return buildTimestamp;
	}
}
