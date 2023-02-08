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
package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Integer.compare;
import static java.lang.Integer.parseInt;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A three-part semantic version description.
 *
 * @author Donal Fellows
 * @param majorVersion
 *            The major version number. Two versions are not compatible if they
 *            have different major version numbers. Major version number
 *            differences dominate. The major version is supposed to only be
 *            updated when an incompatible API change occurs.
 * @param minorVersion
 *            The minor version number. A version is compatible with another
 *            version if it has the same major version and a minor version that
 *            is greater than or equal to the other minor version. The minor
 *            version is supposed to be updated when a compatible API change
 *            occurs.
 * @param revision
 *            The revision number. Less important than the minor version number.
 *            Two versions are usually compatible if they have the same major
 *            and minor version, with the revision being typically unimportant
 *            for that decision. Revisions should be updated when a new release
 *            happens, even if that only contains bug fixes and no API changes.
 */
public record Version(@JsonProperty("major-version") int majorVersion,
		@JsonProperty("minor-version") int minorVersion,
		@JsonProperty("revision") int revision) implements Comparable<Version> {
	// There is no standard Version class. WRYYYYYYYYYYYYYYYY!!!!

	/**
	 * Create a version number.
	 *
	 * @param major
	 *            the major number
	 * @param minor
	 *            the minor number
	 * @param rev
	 *            the revision number
	 */
	public Version(String major, String minor, String rev) {
		this(parseInt(major), parseInt(minor), parseInt(rev));
	}

	// This RE is in Extended mode syntax, which COMMENTS enables
	private static final Pattern VERSION_RE = Pattern.compile("""
			^("?)                     # A optional quote
			(?<major>\\d+)            # A major version number
			(?:\\.(?<minor>\\d+)      # An optional minor version number
			(?:\\.(?<revision>\\d+))? # An optional revision number
			)? \\1 $                  # Back reference to optional quote
			""", Pattern.COMMENTS);

	/**
	 * Create a version number.
	 *
	 * @param threePartVersion
	 *            the version identifier, as {@code X} or {@code X.Y} or
	 *            {@code X.Y.Z}.
	 * @throws IllegalArgumentException
	 *             If the version string doesn't match one of the supported
	 *             patterns.
	 */
	@JsonCreator
	public static Version parse(String threePartVersion) {
		var m = VERSION_RE.matcher(threePartVersion);
		if (!m.matches()) {
			throw new IllegalArgumentException(
					"bad version string: " + threePartVersion);
		}
		return new Version(parseInt(m.group("major")),
				parsePossibleInt(m.group("minor")),
				parsePossibleInt(m.group("revision")));
	}

	private static int parsePossibleInt(String s) {
		if (s == null) {
			return 0;
		}
		return parseInt(s);
	}

	@Override
	public int compareTo(Version other) {
		int cmp = compare(majorVersion, other.majorVersion);
		if (cmp == 0) {
			cmp = compare(minorVersion, other.minorVersion);
			if (cmp == 0) {
				cmp = compare(revision, other.revision);
			}
		}
		return cmp;
	}

	@Override
	public String toString() {
		return majorVersion + "." + minorVersion + "." + revision;
	}

	/**
	 * Determine whether this version is compatible with the given requirement.
	 * For a version to match a requirement, it must have the same major version
	 * and a minor version/revision that is at least what the requirement
	 * states.
	 *
	 * @param requirement
	 *            The version that we are testing for compatibility with.
	 * @return True if they are compatible, false otherwise.
	 */
	public boolean compatibleWith(Version requirement) {
		return (majorVersion == requirement.majorVersion)
				&& compareTo(requirement) >= 0;
	}
}
