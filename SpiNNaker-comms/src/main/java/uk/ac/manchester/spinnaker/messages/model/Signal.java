package uk.ac.manchester.spinnaker.messages.model;

import java.util.HashMap;
import java.util.Map;

/** SCP Signals. */
public enum Signal {
	// TODO Document these values
	/** */
	INITIALISE(0, Type.NEAREST_NEIGHBOUR),
	/** */
	POWER_DOWN(1, Type.NEAREST_NEIGHBOUR),
	/** */
	STOP(2, Type.NEAREST_NEIGHBOUR),
	/** */
	START(3, Type.NEAREST_NEIGHBOUR),
	/** */
	SYNC0(4, Type.MULTICAST),
	/** */
	SYNC1(5, Type.MULTICAST),
	/** */
	PAUSE(6, Type.MULTICAST),
	/** */
	CONTINUE(7, Type.MULTICAST),
	/** */
	EXIT(8, Type.MULTICAST),
	/** */
	TIMER(9, Type.MULTICAST),
	/** */
	USER_0(10, Type.MULTICAST),
	/** */
	USER_1(11, Type.MULTICAST),
	/** */
	USER_2(12, Type.MULTICAST),
	/** */
	USER_3(13, Type.MULTICAST);
	/** The value used for the signal. */
	public final byte value;
	/** The "type" of the signal. */
	public final Type type;
	private static final Map<Byte, Signal> map = new HashMap<>();

	private Signal(int value, Type type) {
		this.value = (byte) value;
		this.type = type;
	}

	static {
		for (Signal r : values()) {
			map.put(r.value, r);
		}
	}

	public static Signal get(byte value) {
		return map.get(value);
	}

	/** The type of signal, determined by how it is transmitted. */
	public static enum Type {
		MULTICAST, POINT_TO_POINT, NEAREST_NEIGHBOUR
	}
}
