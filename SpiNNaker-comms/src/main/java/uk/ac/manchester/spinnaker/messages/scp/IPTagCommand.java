package uk.ac.manchester.spinnaker.messages.scp;

/** SCP IP tag Commands */
public enum IPTagCommand {
	/**  */
	NEW(0),
	/**  */
	SET(1),
	/** */
	GET(2),
	/** */
	CLR(3),
	/** */
	TTO(4);
	public final byte value;

	private IPTagCommand(int value) {
		this.value = (byte) value;
	}
}
