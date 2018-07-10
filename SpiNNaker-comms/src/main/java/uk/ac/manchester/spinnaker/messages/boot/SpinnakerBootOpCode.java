package uk.ac.manchester.spinnaker.messages.boot;

import java.util.HashMap;
import java.util.Map;

/** Boot message Operation Codes */
public enum SpinnakerBootOpCode {
	/** */
	HELLO(0x41),
	/** */
	FLOOD_FILL_START(0x1),
	/** */
	FLOOD_FILL_BLOCK(0x3),
	/** */
	FLOOD_FILL_CONTROL(0x5);
	public final int value;
	private static final Map<Integer, SpinnakerBootOpCode> map = new HashMap<>();

	private SpinnakerBootOpCode(int value) {
		this.value = value;
	}

	static {
		for (SpinnakerBootOpCode c : values()) {
			map.put(c.value, c);
		}
	}

	public static SpinnakerBootOpCode get(int opcode) {
		return map.get(opcode);
	}
}
