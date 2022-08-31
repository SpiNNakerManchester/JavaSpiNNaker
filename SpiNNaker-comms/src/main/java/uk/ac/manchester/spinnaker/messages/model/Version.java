/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Integer.compare;
import static java.lang.Integer.parseInt;
import static java.util.Objects.isNull;

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
		if (isNull(s)) {
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
