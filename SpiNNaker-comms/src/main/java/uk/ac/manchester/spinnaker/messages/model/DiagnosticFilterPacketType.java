package uk.ac.manchester.spinnaker.messages.model;

/**
 * Packet type flags for the diagnostic filters. Note that only one has to match
 * for the counter to be incremented.
 */
public enum DiagnosticFilterPacketType {
	/** Packet is multicast */
	MULTICAST(0),
	/** Packet is point-to-point */
	POINT_TO_POINT(1),
	/** Packet is nearest-neighbour */
	NEAREST_NEIGHBOUR(2),
	/** Packet is fixed-route */
	FIXED_ROUTE(3);
	public final int value;

	private DiagnosticFilterPacketType(int value) {
		this.value = value;
	}
}
