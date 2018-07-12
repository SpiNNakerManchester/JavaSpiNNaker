package uk.ac.manchester.spinnaker.messages.model;

/**
 * SARK Run time errors.
 */
public enum RunTimeError {
	// TODO document the values in here
	/** No error */
	NONE(0),
	/** */
	RESET(1),
	/** */
	UNDEF(2),
	/** */
	SVC(3),
	/** */
	PABT(4),
	/** */
	DABT(5),
	/** */
	IRQ(6),
	/** */
	FIQ(7),
	/** */
	VIC(8),
	/** */
	ABORT(9),
	/** */
	MALLOC(10),
	/** */
	DIVBY0(11),
	/** */
	EVENT(12),
	/** */
	SWERR(13),
	/** */
	IOBUF(14),
	/** */
	ENABLE(15),
	/** */
	NULL(16),
	/** */
	PKT(17),
	/** */
	TIMER(18),
	/** */
	API(19),
	/** */
	SARK_VERSION_INCORRECT(20);
	public final int value;

	private RunTimeError(int value) {
		this.value = value;
	}
}
