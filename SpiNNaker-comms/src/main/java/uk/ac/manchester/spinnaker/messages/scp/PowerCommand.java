package uk.ac.manchester.spinnaker.messages.scp;

/** The SCP Power Commands */
public enum PowerCommand {
	/** Power off the machine */
	POWER_OFF,
	/** Power on the machine */
	POWER_ON;
	public final byte value;

	private PowerCommand() {
		value = (byte) ordinal();
	}
}
