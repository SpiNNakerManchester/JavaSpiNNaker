package uk.ac.manchester.spinnaker.messages.scp;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

public enum BigDataCommand implements CommandCode {

    /**
     * Initialise Big Data
     */
    BIG_DATA_INIT(0),

    /**
     * Free Big Data
     */
    BIG_DATA_FREE(1),

    /**
     * Get Big Data information
     */
    BIG_DATA_INFO(2);


    /** The SCAMP encoding. */
    public final short value;
    private static final Map<Short, BigDataCommand> MAP = new HashMap<>();

    BigDataCommand(int value) {
        this.value = (short) value;
    }

    static {
        for (BigDataCommand r : values()) {
            MAP.put(r.value, r);
        }
    }

    /**
    * Convert an encoded value into an enum element.
    *
    * @param value
    *            The value to convert
    * @return The enum element
    */
    public static BigDataCommand get(short value) {
        return requireNonNull(MAP.get(value),
                "unrecognised command value: " + value);
    }

    @Override
    public short getValue() {
        return value;
    }

}
