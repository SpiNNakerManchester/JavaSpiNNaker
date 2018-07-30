package uk.ac.manchester.spinnaker.messages.eieio;

/** Possible types of EIEIO packets. */
public enum EIEIOType {
	/** Indicates that data is keys which are 16 bits. */
	KEY_16_BIT(0, 2, 0),
	/** Indicates that data is keys and payloads of 16 bits. */
	KEY_PAYLOAD_16_BIT(1, 2, 2),
	/** Indicates that data is keys of 32 bits. */
	KEY_32_BIT(2, 4, 0),
	/** Indicates that data is keys and payloads of 32 bits. */
	KEY_PAYLOAD_32_BIT(3, 4, 4);

	private final int value;
	/** The number of bytes used by each key element. */
	public final int keyBytes;
	/** The number of bytes used by each payload element. */
	public final int payloadBytes;
	/** The maximum value of the key or payload (if there is a payload). */
	public final long maxValue;

	private EIEIOType(int value, int keyBytes, int payloadBytes) {
		this.value = value;
		this.keyBytes = keyBytes;
		this.payloadBytes = payloadBytes;
		this.maxValue = (1L << (keyBytes * 8)) - 1;
	}

	/** @return The encoded type. */
	public int getValue() {
		return value;
	}

	/**
	 * Get the type given its encoded form.
	 *
	 * @param value
	 *            The encoded type.
	 * @return The type object
	 * @throws IllegalArgumentException
	 *             if the encoded type is unrecognised.
	 */
	public static EIEIOType getByValue(int value) {
		for (EIEIOType t : values()) {
			if (t.value == value) {
				return t;
			}
		}
		throw new IllegalArgumentException("no such type");
	}
}
