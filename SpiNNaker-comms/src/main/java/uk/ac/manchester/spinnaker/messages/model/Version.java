package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Integer.compare;
import static java.lang.Integer.parseInt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	public Version(int major, int minor, int rev) {
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

	static final Pattern VERSION_RE =
			Pattern.compile("^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?$");

	/**
	 * Create a version number.
	 *
	 * @param threePartVersion
	 *            the version identifier, as X or X.Y or X.Y.Z
	 */
	public Version(String threePartVersion) {
		Matcher m = VERSION_RE.matcher(threePartVersion);
		if (!m.matches()) {
			throw new IllegalArgumentException(
					"bad version string: " + threePartVersion);
		}
		majorVersion = parseInt(m.group(1));
		minorVersion = (m.groupCount() > 1 ? parseInt(m.group(2)) : 0);
		revision = (m.groupCount() > 2 ? parseInt(m.group(3)) : 0);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof Version)) {
			return false;
		}
		Version v = (Version) other;
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
