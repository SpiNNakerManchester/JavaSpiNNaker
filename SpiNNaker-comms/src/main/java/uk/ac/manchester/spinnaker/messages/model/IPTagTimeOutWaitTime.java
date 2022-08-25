/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
	/** Wait for 10ms. */
	TIMEOUT_10_ms(1),
	/** Wait for 20ms. */
	TIMEOUT_20_ms(2),
	/** Wait for 40ms. */
	TIMEOUT_40_ms(3),
	/** Wait for 80ms. */
	TIMEOUT_80_ms(4),
	/** Wait for 160ms. */
	TIMEOUT_160_ms(5),
	/** Wait for 320ms. */
	TIMEOUT_320_ms(6),
	/** Wait for 640ms. */
	TIMEOUT_640_ms(7),
	/** Wait for 1.28s. */
	TIMEOUT_1280_ms(8),
	/** Wait for 2.56s. */
	TIMEOUT_2560_ms(9);

	/** The SCAMP-encoded value. */
	public final int value;

	private static final Map<Integer, IPTagTimeOutWaitTime> MAP =
			new HashMap<>();

	IPTagTimeOutWaitTime(int value) {
		this.value = value;
	}

	static {
		for (IPTagTimeOutWaitTime tto : values()) {
			MAP.put(tto.value, tto);
		}
	}

	/**
	 * Deserialise a value into the enum.
	 *
	 * @param value
	 *            The value to deserialise.
	 * @return The deserialised value, or {@code null} if it is unrecognised.
	 */
	public static IPTagTimeOutWaitTime get(int value) {
		return MAP.get(value);
	}
}
