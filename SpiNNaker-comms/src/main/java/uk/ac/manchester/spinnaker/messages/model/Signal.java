package uk.ac.manchester.spinnaker.messages.model;

import static java.util.Objects.requireNonNull;

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
	private static final Map<Byte, Signal> MAP = new HashMap<>();

	Signal(int value, Type type) {
		this.value = (byte) value;
		this.type = type;
	}

	static {
		for (Signal r : values()) {
			MAP.put(r.value, r);
		}
	}

	public static Signal get(byte value) {
		return requireNonNull(MAP.get(value), "unknown signal: " + value);
	}

	/** The type of signal, determined by how it is transmitted. */
	public enum Type {
		MULTICAST(0), POINT_TO_POINT(1), NEAREST_NEIGHBOUR(2);
		/** The SARK encoding. */
		public final int value;
		Type(int value) {
			this.value = value;
		}
	}
}
