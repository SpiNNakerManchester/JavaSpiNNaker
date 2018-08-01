package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.Constants.SHORT_SIZE;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

enum TransferUnit {
	BYTE(0), HALF_WORD(1), WORD(2);
	public final int value;

	TransferUnit(int value) {
		this.value = value;
	}

	public static TransferUnit efficientTransferUnit(int address, int size) {
		if (address % WORD_SIZE == 0 && size % WORD_SIZE == 0) {
			return WORD;
		} else if (address % WORD_SIZE == SHORT_SIZE
				|| size % WORD_SIZE == SHORT_SIZE) {
			return HALF_WORD;
		} else {
			return BYTE;
		}
	}
}
