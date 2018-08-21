package uk.ac.manchester.spinnaker.messages.model;

/** SCP IP tag Commands. */
public enum IPTagCommand {
	/** Create. */
	NEW(0),
	/** Update. */
	SET(1),
	/** Fetch. */
	GET(2),
	/** Delete. */
	CLR(3),
	/** Update Meta. */
	TTO(4);

	/** The SCAMP-encoded value. */
	public final byte value;
	IPTagCommand(int value) {
		this.value = (byte) value;
	}
}
