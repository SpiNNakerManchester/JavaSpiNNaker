package uk.ac.manchester.spinnaker.spalloc;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;

/**
 * Utility operations for working with timestamps.
 *
 * @author Donal Fellows
 */
abstract class Utils {
	private Utils() {
	}

	/**
	 * Convert a timestamp into how long to wait for it.
	 *
	 * @param timestamp
	 *            the time of expiry, or <tt>null</tt> for a timestamp that is
	 *            never expired.
	 * @return the number of milliseconds remaining to wait for the timestamp to
	 *         expire, or <tt>null</tt> for "wait indefinitely".
	 */
	static Integer timeLeft(Long timestamp) {
		if (timestamp == null) {
			return null;
		}
		return max(0, (int) (timestamp - currentTimeMillis()));
	}

	/**
	 * Check if a timestamp has been reached.
	 *
	 * @param timestamp
	 *            the time of expiry, or <tt>null</tt> for a timestamp that is
	 *            never expired.
	 * @return true if the timestamp has been passed.
	 */
	static boolean timedOut(Long timestamp) {
		return timestamp != null && timestamp < currentTimeMillis();
	}

	/**
	 * Convert a delay (in milliseconds) into a timestamp.
	 *
	 * @param delay
	 *            how long the timeout is, in milliseconds, or <tt>null</tt> for
	 *            infinite.
	 * @return when the timeout expires, or <tt>null</tt> for "never".
	 */
	static Long makeTimeout(Integer delay) {
		if (delay == null) {
			return null;
		}
		return currentTimeMillis() + delay;
	}
}
