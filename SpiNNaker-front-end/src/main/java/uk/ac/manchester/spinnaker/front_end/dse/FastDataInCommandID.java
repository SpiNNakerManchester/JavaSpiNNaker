/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.dse;

import java.util.HashMap;
import java.util.Map;

/**
 * Command IDs for the SDP packets for data in.
 *
 * @author Donal Fellows
 */
public enum FastDataInCommandID {
	/** Host to Gatherer: start accepting data bound for location. */
	SEND_DATA_TO_LOCATION(200),
	/** Host to Gatherer: more data with sequence number. */
	SEND_SEQ_DATA(2000),
	/** Host to Gatherer: all data transmitted. */
	SEND_TELL_DATA_IN(2001),
	/** Gatherer to host: there are missing sequence numbers. */
	RECEIVE_MISSING_SEQ_DATA_IN(2002),
	/** Gatherer to host: all present and correct. */
	RECEIVE_FINISHED_DATA_IN(2003);

	private static final Map<Integer, FastDataInCommandID> MAP =
			new HashMap<>();

	static {
		for (var c : values()) {
			MAP.put(c.value, c);
		}
	}

	/** The protocol ID of this constant. */
	public final int value;

	FastDataInCommandID(int value) {
		this.value = value;
	}

	/**
	 * Get a constant by its protocol ID.
	 *
	 * @param value
	 *            The protocol ID
	 * @return The matching constant.
	 * @throws IllegalArgumentException
	 *             if the value isn't one of the ones accepted by this class.
	 */
	public static FastDataInCommandID forValue(int value) {
		var id = MAP.get(value);
		if (id == null) {
			throw new IllegalArgumentException(
					"unexpected command code: " + value);
		}
		return id;
	}
}
