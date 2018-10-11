package uk.ac.manchester.spinnaker.transceiver;

/**
 * Marks a class that is used to track how many retries were used.
 *
 * @author Donal Fellows
 */
public interface RetryTracker {
	/**
	 * Note that a retry was required.
	 */
	void retryNeeded();
}
