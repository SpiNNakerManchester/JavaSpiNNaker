package uk.ac.manchester.spinnaker.messages.bmp;

/** The SCP BMP Information Types. */
public enum BMPInfo {
	/** Serial information. */
	SERIAL(0),
	/** CAN status information. */
	CAN_STATUS(2),
	/** ADC information. */
	ADC(3),
	/** IP Address. */
	IP_ADDR(4);

	/** The raw BMP value. */
	public final byte value;

	BMPInfo(int value) {
		this.value = (byte) value;
	}
}
