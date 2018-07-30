package uk.ac.manchester.spinnaker.messages.sdp;

import java.util.HashMap;
import java.util.Map;

/** SDPFlag for the message. */
public enum SDPFlag {
	/** Indicates that a reply is not expected. */
	REPLY_NOT_EXPECTED(0x07),
	/** Indicates that a reply is expected. */
	REPLY_EXPECTED(0x87);
	public final byte value;
	private static final Map<Byte, SDPFlag> map = new HashMap<>();

	private SDPFlag(int value) {
		this.value = (byte) value;
	}

	static {
		for (SDPFlag flag : values()) {
			map.put(flag.value, flag);
		}
	}

	public static SDPFlag get(byte value) {
		return map.get(value);
	}
}
