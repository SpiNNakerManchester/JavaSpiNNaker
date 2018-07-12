package uk.ac.manchester.spinnaker.messages.scp;

enum TransferUnit {
	BYTE(0), HALF_WORD(1), WORD(2);
	public final int value;

	private TransferUnit(int value) {
		this.value = value;
	}

	public static TransferUnit efficientTransferUnit(int address, int size) {
		if (address % 4 == 0 && size % 4 == 0) {
			return WORD;
		} else if (address % 4 == 2 || size % 4 == 2) {
			return HALF_WORD;
		} else {
			return BYTE;
		}
	}
}
