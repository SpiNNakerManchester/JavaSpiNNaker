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
package uk.ac.manchester.spinnaker.messages.eieio;

import java.util.HashMap;
import java.util.Map;

/**
 * What SpiNNaker-specific EIEIO commands there are.
 */
public enum EIEIOCommandID implements EIEIOCommand {
	/** Database handshake with external program. */
	DATABASE_CONFIRMATION(1),
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
	/** Notify the external devices that the simulation has stopped. */
	STOP_PAUSE_NOTIFICATION(10),
	/** Notify the external devices that the simulation has started. */
	START_RESUME_NOTIFICATION(11),
	/** Host confirming request to read data received. */
	HOST_DATA_READ_ACK(12);
	private final int value;
	private static final Map<Integer, EIEIOCommandID> MAP = new HashMap<>();
	static {
		for (EIEIOCommandID commmand : values()) {
			MAP.put(commmand.value, commmand);
		}
	}

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
		EIEIOCommandID id = MAP.get(command);
		if (id != null) {
			return id;
		}
		return new CustomEIEIOCommand(command);
	}
}
