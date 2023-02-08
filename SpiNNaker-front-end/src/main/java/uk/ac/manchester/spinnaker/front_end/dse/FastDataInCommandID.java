/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.dse;

import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

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
			makeEnumBackingMap(values(), v -> v.value);

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
