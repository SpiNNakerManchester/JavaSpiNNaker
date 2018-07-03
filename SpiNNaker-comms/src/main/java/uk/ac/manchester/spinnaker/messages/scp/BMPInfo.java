package uk.ac.manchester.spinnaker.messages.scp;

/** The SCP BMP Information Types */
public enum BMPInfo {
	/** Serial information */
	SERIAL(0),
	/** CAN status information */
	CAN_STATUS(2),
	/** ADC information */
	ADC(3),
	/** IP Address */
	IP_ADDR(4);
	public final byte value;

	private BMPInfo(int value) {
		this.value = (byte) value;
	}
}
