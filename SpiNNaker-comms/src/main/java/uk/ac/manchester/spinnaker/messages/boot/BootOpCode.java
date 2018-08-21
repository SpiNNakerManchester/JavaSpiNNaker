package uk.ac.manchester.spinnaker.messages.boot;

import java.util.HashMap;
import java.util.Map;

/** Boot message operation codes. */
public enum BootOpCode {
	/** Sent by SpiNNaker to announce itself ready for booting. */
	HELLO(0x41),
	/** Start a fill of the boot image (i.e., SCAMP). */
	FLOOD_FILL_START(0x1),
	/** Message contains a block from the boot image. */
	FLOOD_FILL_BLOCK(0x3),
	/** Finish a fill of the boot image (i.e., SCAMP). */
	FLOOD_FILL_CONTROL(0x5);

	/** The encoded form of the opcode. */
	public final int value;
	private static final Map<Integer, BootOpCode> MAP = new HashMap<>();

	BootOpCode(int value) {
		this.value = value;
	}

	static {
		for (BootOpCode c : values()) {
			MAP.put(c.value, c);
		}
	}

	/**
	 * @param opcode
	 *            The opcode to convert.
	 * @return The converted opcode, or <tt>null</tt> if it was unrecognised.
	 */
	public static BootOpCode get(int opcode) {
		return MAP.get(opcode);
	}
}
