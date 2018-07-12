package uk.ac.manchester.spinnaker.messages.model;

/**
 * Payload flags for the diagnostic filters. Note that only one has to match for
 * the counter to be incremented.
 */
public enum DiagnosticFilterPayloadStatus {
	/** Packet has a payload */
	WITH_PAYLOAD(0),
	/** Packet doesn't have a payload */
	WITHOUT_PAYLOAD(1);
	public final int value;

	private DiagnosticFilterPayloadStatus(int value) {
		this.value = value;
	}
}
