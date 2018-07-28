package uk.ac.manchester.spinnaker.messages.model;

import java.util.HashMap;
import java.util.Map;

/**
 * The values used by the SCP IP tag time outs. These control how long to wait
 * for any message request which requires a response, before raising an error.
 * The value is calculated via the following formula:
 * <dl>
 * <dd>10ms * 2<sup>tagTimeout.value - 1</sup></dd>
 * </dl>
 */
public enum IPTagTimeOutWaitTime {
	/** */
	TIMEOUT_10_ms(1),
	/** */
	TIMEOUT_20_ms(2),
	/** */
	TIMEOUT_40_ms(3),
	/** */
	TIMEOUT_80_ms(4),
	/** */
	TIMEOUT_160_ms(5),
	/** */
	TIMEOUT_320_ms(6),
	/** */
	TIMEOUT_640_ms(7),
	/** */
	TIMEOUT_1280_ms(8),
	/** */
	TIMEOUT_2560_ms(9);
	public final int value;
	private static final Map<Integer, IPTagTimeOutWaitTime> map = new HashMap<>();

	private IPTagTimeOutWaitTime(int value) {
		this.value = value;
	}

	static {
		for (IPTagTimeOutWaitTime tto : values()) {
			map.put(tto.value, tto);
		}
	}

	public static IPTagTimeOutWaitTime get(int value) {
		return map.get(value);
	}
}
