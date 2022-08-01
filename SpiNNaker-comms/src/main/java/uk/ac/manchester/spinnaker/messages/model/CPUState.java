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

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

import java.util.Map;

/** SARK CPU States. */
public enum CPUState {
	/** */
	DEAD,
	/** */
	POWERED_DOWN,
	/** */
	RUN_TIME_EXCEPTION,
	/** */
	WATCHDOG,
	/** */
	INITIALISING,
	/** */
	READY,
	/** */
	C_MAIN,
	/** */
	RUNNING,
	/** */
	SYNC0,
	/** */
	SYNC1,
	/** */
	PAUSED,
	/** */
	FINISHED,
	/** */
	@Deprecated
	CPU_STATE_12,
	/** */
	@Deprecated
	CPU_STATE_13,
	/** */
	@Deprecated
	CPU_STATE_14,
	/** */
	IDLE;

	/** The canonical SARK value for the state. */
	public final int value;

	private static final Map<Integer, CPUState> MAP =
			makeEnumBackingMap(values(), v -> v.value);

	CPUState() {
		value = ordinal();
	}

	/**
	 * Get the element for a value.
	 *
	 * @param value
	 *            The value to look up
	 * @return The enumeration item it represents
	 */
	public static CPUState get(int value) {
		return requireNonNull(MAP.get(value),
				"value not an official SARK value");
	}
}
