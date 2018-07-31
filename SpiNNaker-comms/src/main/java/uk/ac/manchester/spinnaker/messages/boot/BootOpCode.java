package uk.ac.manchester.spinnaker.messages.boot;

import java.util.HashMap;
import java.util.Map;

/** Boot message Operation Codes. */
public enum BootOpCode {
	// TODO Document these properly
	/** */
	HELLO(0x41),
	/** */
	FLOOD_FILL_START(0x1),
	/** */
	FLOOD_FILL_BLOCK(0x3),
	/** */
	FLOOD_FILL_CONTROL(0x5);
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

	public static BootOpCode get(int opcode) {
		return MAP.get(opcode);
	}
}
