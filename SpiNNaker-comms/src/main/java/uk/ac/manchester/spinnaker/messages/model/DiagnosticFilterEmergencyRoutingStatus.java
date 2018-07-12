package uk.ac.manchester.spinnaker.messages.model;

/**
 * Emergency routing status flags for the diagnostic filters. Note that only one
 * has to match for the counter to be incremented.
 */
public enum DiagnosticFilterEmergencyRoutingStatus {
	/** Packet is not emergency routed. */
	NORMAL(0),
	/**
	 * Packet is in first hop of emergency route; packet should also have been
	 * sent here by normal routing.
	 */
	FIRST_STAGE_COMBINED(1),
	/**
	 * Packet is in first hop of emergency route; packet wouldn't have reached
	 * this router without emergency routing.
	 */
	FIRST_STAGE(2),
	/**
	 * Packet is in last hop of emergency route and should now return to normal
	 * routing.
	 */
	SECOND_STAGE(3);
	public final int value;

	private DiagnosticFilterEmergencyRoutingStatus(int value) {
		this.value = value;
	}
}
