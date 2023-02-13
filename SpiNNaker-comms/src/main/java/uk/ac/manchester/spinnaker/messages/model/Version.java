/*
 * Copyright (c) 2018-2023 The University of Manchester
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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A three-part semantic version description.
 *
 * @author Donal Fellows
 */
public final class Version implements Comparable<Version> {
	// There is no standard Version class. WRYYYYYYYYYYYYYYYY!!!!
	/**
	 * The major version number. Two versions are not compatible if they have
	 * different major version numbers. Major version number differences
	 * dominate.
	 */
	public final int majorVersion;

	/** The minor version number. */
	public final int minorVersion;

	/** The revision number. Less important than the minor version number. */
	public final int revision;

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
	public Version(@JsonProperty("major-version") int major,
			@JsonProperty("minor-version") int minor,
			@JsonProperty("revision") int rev) {
		majorVersion = major;
		minorVersion = minor;
		revision = rev;
	}

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
		majorVersion = parseInt(major);
		minorVersion = parseInt(minor);
		revision = parseInt(rev);
	}

	// This RE is in Extended mode syntax, which COMMENTS enables
	private static final Pattern VERSION_RE = Pattern.compile(
			"  ^(\"?)               # A optional quote\n"
			+ "(?<major>\\d+)       # A major version number\n"
			+ "(?:\\.(?<minor>\\d+) # An optional minor version number\n"
			+ "(?:\\.(?<revision>\\d+))? # An optional revision number\n"
			+ ")?\\1$               # Back reference to optional quote\n",
			Pattern.COMMENTS);

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
	public Version(String threePartVersion) {
		var m = VERSION_RE.matcher(threePartVersion);
		if (!m.matches()) {
			throw new IllegalArgumentException(
					"bad version string: " + threePartVersion);
		}
		majorVersion = parseInt(m.group("major"));
		minorVersion = parsePossibleInt(m.group("minor"));
		revision = parsePossibleInt(m.group("revision"));
	}

	private static int parsePossibleInt(String s) {
		if (s == null) {
			return 0;
		}
		return parseInt(s);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Version)) {
			return false;
		}
		var v = (Version) other;
		return majorVersion == v.majorVersion && minorVersion == v.minorVersion
				&& revision == v.revision;
	}

	@Override
	public int hashCode() {
		return (majorVersion << 10) ^ (minorVersion << 5) ^ revision;
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
		return "" + majorVersion + "." + minorVersion + "." + revision;
	}
}
