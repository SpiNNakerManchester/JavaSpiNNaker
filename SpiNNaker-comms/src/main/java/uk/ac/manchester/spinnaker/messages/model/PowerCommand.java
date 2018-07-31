package uk.ac.manchester.spinnaker.messages.model;

/** The BMP Power Commands. */
public enum PowerCommand {
	/** Power off the machine. */
	POWER_OFF,
	/** Power on the machine. */
	POWER_ON;

	/** The BMP-encoded value. */
	public final byte value;

	PowerCommand() {
		value = (byte) ordinal();
	}
}
