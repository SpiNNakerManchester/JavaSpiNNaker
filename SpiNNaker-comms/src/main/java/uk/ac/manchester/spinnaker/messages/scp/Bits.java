package uk.ac.manchester.spinnaker.messages.scp;

/** Standard bit shifts. */
abstract class Bits {
	private Bits() {
	}

	/** The top bit of the word. */
	static final int TOP_BIT = 31;
	/** Bits 31&ndash;24. */
	static final int BYTE3 = 24;
	/** Bits 23&ndash;16. */
	static final int BYTE2 = 16;
	/** Bits 15&ndash;8. */
	static final int BYTE1 = 8;
	/** Bits 7&ndash;0. */
	static final int BYTE0 = 0;
}
