package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Integer.compare;
import static java.lang.Integer.parseInt;

public final class Version implements Comparable<Version> {
	// There is no standard Version class. WRYYYYYYYYYYYYYYYY!!!!
	public final int majorVersion;
	public final int minorVersion;
	public final int revision;

	public Version(int major, int minor, int rev) {
		majorVersion = major;
		minorVersion = minor;
		revision = rev;
	}

	public Version(String major, String minor, String rev) {
		majorVersion = parseInt(major);
		minorVersion = parseInt(minor);
		revision = parseInt(rev);
	}

	@Override
	public int compareTo(Version o) {
		int cmp = compare(majorVersion, o.majorVersion);
		if (cmp == 0) {
			cmp = compare(minorVersion, o.minorVersion);
			if (cmp == 0) {
				cmp = compare(revision, o.revision);
			}
		}
		return cmp;
	}
}
