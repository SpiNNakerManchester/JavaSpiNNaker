package uk.ac.manchester.spinnaker.messages.model;

/**
 * P2P Routing table routes
 */
public enum P2PTableRoute {
	/** */ // TODO Document this
	EAST(0b000),
	/** */ // TODO Document this
	NORTH_EAST(0b001),
	/** */ // TODO Document this
	NORTH(0b010),
	/** */ // TODO Document this
	WEST(0b011),
	/** */ // TODO Document this
	SOUTH_WEST(0b100),
	/** */ // TODO Document this
	SOUTH(0b101),
	/** No route to this chip */
	NONE(0b110),
	/** Route to the monitor on the current chip */
	MONITOR(0b111);
	public final int value;

	private P2PTableRoute(int value) {
		this.value = value;
	}
}
