package uk.ac.manchester.spinnaker.messages.eieio;

/** Possible prefixing of keys in EIEIO packets. */
public enum EIEIOPrefix {
	/** apply prefix on lower half of the word */
	LOWER_HALF_WORD(0),
	/** apply prefix on top half of the word */
	UPPER_HALF_WORD(1);

	private final int value;

	private EIEIOPrefix(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static EIEIOPrefix getByValue(int value) {
		for (EIEIOPrefix p : values()) {
			if (p.value == value) {
				return p;
			}
		}
		throw new IllegalArgumentException("no such prefix");
	}
}
