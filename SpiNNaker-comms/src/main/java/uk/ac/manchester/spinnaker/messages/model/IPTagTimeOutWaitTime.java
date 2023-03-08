/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.model;

import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

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
			makeEnumBackingMap(values(), v -> v.value);

	IPTagTimeOutWaitTime(int value) {
		this.value = value;
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
