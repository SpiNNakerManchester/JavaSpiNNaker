package uk.ac.manchester.spinnaker.messages.model;

import java.util.HashMap;
import java.util.Map;

/**
 * P2P Routing table routes
 */
public enum P2PTableRoute {
	// TODO Document these values
	/** */
	EAST(0b000),
	/** */
	NORTH_EAST(0b001),
	/** */
	NORTH(0b010),
	/** */
	WEST(0b011),
	/** */
	SOUTH_WEST(0b100),
	/** */
	SOUTH(0b101),
	/** No route to this chip */
	NONE(0b110),
	/** Route to the monitor on the current chip */
	MONITOR(0b111);
	public final int value;
	private static final Map<Integer, P2PTableRoute> map = new HashMap<>();
	static {
		for (P2PTableRoute r : values()) {
			map.put(r.value, r);
		}
	}

	private P2PTableRoute(int value) {
		this.value = value;
	}

	public static P2PTableRoute get(int value) {
		return map.get(value);
	}
}
