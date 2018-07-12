package uk.ac.manchester.spinnaker.messages.model;

/**
 * Default routing flags for the diagnostic filters. Note that only one has to
 * match for the counter to be incremented
 */
public enum DiagnosticFilterDefaultRoutingStatus {
	/** Packet is to be default routed */
	DEFAULT_ROUTED(0),
	/** Packet is not to be default routed */
	NON_DEFAULT_ROUTED(1);
	public final int value;

	private DiagnosticFilterDefaultRoutingStatus(int value) {
		this.value = value;
	}
}
