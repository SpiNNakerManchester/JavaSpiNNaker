package uk.ac.manchester.spinnaker.messages.model;

import java.util.HashMap;
import java.util.Map;

/**
 * SARK Run time errors.
 */
public enum RunTimeError {
	/** No error. */
	NONE(0),
	/** Branch through zero. */
	RESET(1),
	/** Undefined instruction. */
	UNDEF(2),
	/** Undefined SVC or no handler. */
	SVC(3),
	/** Prefetch abort. */
	PABT(4),
	/** Data abort. */
	DABT(5),
	/** Unhandled IRQ. */
	IRQ(6),
	/** Unhandled FIQ. */
	FIQ(7),
	/** Unconfigured VIC vector. */
	VIC(8),
	/** Generic user abort. */
	ABORT(9),
	/** "malloc" failure. */
	MALLOC(10),
	/** Divide by zero. */
	DIVBY0(11),
	/** Event startup failure. */
	EVENT(12),
	/** Fatal SW error. */
	SWERR(13),
	/** Failed to allocate IO buffer. */
	IOBUF(14),
	/** Bad event enable. */
	ENABLE(15),
	/** Generic null pointer error. */
	NULL(16),
	/** Pkt startup failure. */
	PKT(17),
	/** Timer startup failure. */
	TIMER(18),
	/** API startup failure. */
	API(19),
	/** SW version conflict. */
	SARK_VERSION_INCORRECT(20);
	public final int value;
	private static final Map<Integer, RunTimeError> map = new HashMap<>();
	static {
		for (RunTimeError v : values()) {
			map.put(v.value, v);
		}
	}

	private RunTimeError(int value) {
		this.value = value;
	}

	public static RunTimeError get(int value) {
		return map.get(value);
	}
}
