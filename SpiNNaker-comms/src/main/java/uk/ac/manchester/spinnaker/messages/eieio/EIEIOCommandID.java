/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

import java.util.Map;

/**
 * What SpiNNaker-specific EIEIO commands there are.
 */
public enum EIEIOCommandID implements EIEIOCommand {
	/** Fill in buffer area with padding. */
	EVENT_PADDING(2),
	/** End of all buffers, stop execution. */
	EVENT_STOP(3),
	/** Stop complaining that there is SDRAM free space for buffers. */
	STOP_SENDING_REQUESTS(4),
	/** Start complaining that there is SDRAM free space for buffers. */
	START_SENDING_REQUESTS(5),
	/** Spinnaker requesting new buffers for spike source population. */
	SPINNAKER_REQUEST_BUFFERS(6),
	/** Buffers being sent from host to SpiNNaker. */
	HOST_SEND_SEQUENCED_DATA(7),
	/** Buffers available to be read from a buffered out vertex. */
	SPINNAKER_REQUEST_READ_DATA(8),
	/** Host confirming data being read form SpiNNaker memory. */
	HOST_DATA_READ(9),
	/** Host confirming request to read data received. */
	HOST_DATA_READ_ACK(12);

	private final int value;

	private static final Map<Integer, EIEIOCommandID> MAP =
			makeEnumBackingMap(values(), v -> v.value);

	EIEIOCommandID(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return value;
	}

	/**
	 * Get a command given its encoded form.
	 *
	 * @param command
	 *            the encoded command
	 * @return the ID, or {@code null} if the encoded form was unrecognised.
	 */
	public static EIEIOCommand get(int command) {
		var id = MAP.get(command);
		if (id != null) {
			return id;
		}
		return new CustomEIEIOCommand(command);
	}
}
