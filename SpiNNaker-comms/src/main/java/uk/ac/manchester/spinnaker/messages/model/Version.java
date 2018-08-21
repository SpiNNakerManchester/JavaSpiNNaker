package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Integer.compare;
import static java.lang.Integer.parseInt;

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
