package uk.ac.manchester.spinnaker.messages.model;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

/** SARK CPU States. */
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
    /** The canonical SARK value for the state. */
    public final int value;
    private static final Map<Integer, CPUState> MAP = new HashMap<>();

    CPUState() {
        value = ordinal();
    }

    static {
        for (CPUState state : values()) {
            MAP.put(state.value, state);
        }
    }

    /**
     * Get the element for a value.
     *
     * @param value
     *            The value to look up
     * @return The enumeration item it represents
     */
    public static CPUState get(int value) {
        return requireNonNull(MAP.get(value),
                "value not an official SARK value");
    }
}
