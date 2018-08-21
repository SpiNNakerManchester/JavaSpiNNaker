package uk.ac.manchester.spinnaker.messages.model;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

/** SCP Signals. */
public enum Signal {
	/** The system is initialising after boot. */
	INITIALISE(0, Type.NEAREST_NEIGHBOUR),
	/** The system is powering down. */
	POWER_DOWN(1, Type.NEAREST_NEIGHBOUR),
	/** The system is ceasing to run user apps. */
	STOP(2, Type.NEAREST_NEIGHBOUR),
	/** Tells apps to start. */
	START(3, Type.NEAREST_NEIGHBOUR),
	/** Tells apps to advance from the SYNC0 state. */
	SYNC0(4, Type.MULTICAST),
	/** Tells apps to advance from the SYNC1 state. */
	SYNC1(5, Type.MULTICAST),
	/** Tells apps to pause. */
	PAUSE(6, Type.MULTICAST),
	/** Tells apps to continue from pause. */
	CONTINUE(7, Type.MULTICAST),
	/** Tells apps to exit. */
	EXIT(8, Type.MULTICAST),
	/** Used for clock synchronisation? */
	TIMER(9, Type.MULTICAST),
	/** For application use. */
	USER_0(10, Type.MULTICAST),
	/** For application use. */
	USER_1(11, Type.MULTICAST),
	/** For application use. */
	USER_2(12, Type.MULTICAST),
	/** For application use. */
	USER_3(13, Type.MULTICAST);

	/** The value used for the signal. */
	public final byte value;
	/** The "type" of the signal. */
	public final Type type;
	private static final Map<Byte, Signal> MAP = new HashMap<>();

	/**
	 * @param value the value
	 * @param type the type
	 */
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
		/** Signal is sent to all cores via MC packets. */
		MULTICAST(0),
		/** Signal is sent to all cores via P2P packets. */
		POINT_TO_POINT(1),
		/** Signal is sent to all cores via NN packets. */
		NEAREST_NEIGHBOUR(2);
		/** The SARK encoding. */
		public final int value;
		Type(int value) {
			this.value = value;
		}
	}
}
