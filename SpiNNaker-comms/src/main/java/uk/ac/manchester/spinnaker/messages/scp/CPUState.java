package uk.ac.manchester.spinnaker.messages.scp;

import java.util.HashMap;
import java.util.Map;

/** SARK CPU States */
public enum CPUState {
	/** */
	DEAD,
	/** */
	POWERED_DOWN,
	/** */
	RUN_TIME_EXCEPTION,
	/** */
	WATCHDOG,
	/** */
	INITIALISING,
	/** */
	READY,
	/** */
	C_MAIN,
	/** */
	RUNNING,
	/** */
	SYNC0,
	/** */
	SYNC1,
	/** */
	PAUSED,
	/** */
	FINISHED,
	/** */
	@Deprecated
	CPU_STATE_12,
	/** */
	@Deprecated
	CPU_STATE_13,
	/** */
	@Deprecated
	CPU_STATE_14,
	/** */
	IDLE;
	public final int value;
	private static final Map<Integer, CPUState> map = new HashMap<>();

	private CPUState() {
		value = ordinal();
	}

	static {
		for (CPUState state : values()) {
			map.put(state.value, state);
		}
	}

	public static CPUState get(int value) {
		return map.get(value);
	}
}
