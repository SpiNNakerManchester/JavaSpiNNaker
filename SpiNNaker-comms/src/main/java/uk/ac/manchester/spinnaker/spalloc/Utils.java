package uk.ac.manchester.spinnaker.spalloc;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;

abstract class Utils {
	private Utils() {
	}

	/** Convert a timestamp into how long to wait for it. */
	static Integer timeLeft(Long timestamp) {
		if (timestamp == null) {
			return null;
		}
		return max(0, (int) (timestamp - currentTimeMillis()));
	}

	/** Check if a timestamp has been reached. */
	static boolean timedOut(Long timestamp) {
		return timestamp != null && timestamp < currentTimeMillis();
	}

	/** Convert a delay (in milliseconds) into a timestamp. */
	static Long makeTimeout(Integer delay) {
		if (delay == null) {
			return null;
		}
		return currentTimeMillis() + delay;
	}
}
