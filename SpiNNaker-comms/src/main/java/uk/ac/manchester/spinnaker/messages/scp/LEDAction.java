package uk.ac.manchester.spinnaker.messages.scp;

/** The SCP LED actions */
public enum LEDAction {
	/** Toggle the LED status */
	TOGGLE(1),
	/** Turn the LED off */
	OFF(2),
	/** Turn the LED on */
	ON(3);
	public final byte value;

	private LEDAction(int value) {
		this.value = (byte) value;
	}
}
