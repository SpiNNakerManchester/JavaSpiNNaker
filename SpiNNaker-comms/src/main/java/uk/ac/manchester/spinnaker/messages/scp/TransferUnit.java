package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

/** What to move data in units of. */
enum TransferUnit {
	/** A byte. */
	BYTE(0),
	/** Two bytes. */
	HALF_WORD(1),
	/** Four bytes. */
	WORD(2);

	/**
	 * The encoded transfer unit.
	 */
	public final int value;

	TransferUnit(int value) {
		this.value = value;
	}

	/**
	 * Is a value an odd number?
	 *
	 * @param value
	 *            The value to test
	 * @return True exactly when the value is odd.
	 */
	private static boolean odd(int value) {
		return (value & 1) == 1;
	}

	/**
	 * What is an efficient transfer unit to use, given a starting address and a
	 * size of data to move.
	 *
	 * @param address
	 *            The address.
	 * @param size
	 *            The data size.
	 * @return The preferred transfer unit.
	 */
	public static TransferUnit efficientTransferUnit(int address, int size) {
		if (address % WORD_SIZE == 0 && size % WORD_SIZE == 0) {
			return WORD;
		} else if (odd(address) || odd(size)) {
			return BYTE;
		} else {
			return HALF_WORD;
		}
	}
}
