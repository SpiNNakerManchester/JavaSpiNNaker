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
	SEND_LAST_DATA_IN(2002),
	/** Gatherer to host: there are missing sequence numbers. */
	RECEIVE_FIRST_MISSING_SEQ_DATA_IN(2003),
	/**
	 * Gatherer to host: here are more missing sequence numbers. Sequence number
	 * <tt>-1</tt> marks the end.
	 */
	RECEIVE_MISSING_SEQ_DATA_IN(2004),
	/** Gatherer to host: all present and correct. */
	RECEIVE_FINISHED_DATA_IN(2005);
	private static final Map<Integer, FastDataInCommandID> MAP =
			new HashMap<>();
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
	 * @return The matching constant, or <tt>null</tt> if the value is
	 *         unrecognised.
	 */
	public static FastDataInCommandID forValue(int value) {
		if (MAP.isEmpty()) {
			for (FastDataInCommandID c : values()) {
				MAP.put(c.value, c);
			}
		}
		return MAP.get(value);
	}
}
