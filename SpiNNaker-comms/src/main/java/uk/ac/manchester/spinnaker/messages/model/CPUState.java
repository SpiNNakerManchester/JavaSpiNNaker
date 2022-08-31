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

import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/** SARK CPU States. */
@UsedInJavadocOnly(Signal.class)
public enum CPUState {
	/** Core is dead. */
	DEAD,
	/** Core is powered down. */
	POWERED_DOWN,
	/** Core has had an RTE and not yet been reset. */
	RUN_TIME_EXCEPTION,
	/** Core was unresponsive and so was shutdown by the watchdog timer. */
	WATCHDOG,
	/** Core is preparing to enter service. */
	INITIALISING,
	/** Core is ready for service. */
	READY,
	/** Core is doing something with {@code c_main()} entry point. */
	// TODO what?
	C_MAIN,
	/** Core is running user code. */
	RUNNING,
	/** Core is waiting for {@link Signal#SYNC0}. */
	SYNC0,
	/** Core is waiting for {@link Signal#SYNC1}. */
	SYNC1,
	/** Core is paused. */
	PAUSED,
	/** Core has finished. */
	FINISHED,
	/**
	 * CPU State 12.
	 *
	 * @deprecated Should be unused. Might be changed to another meaning by a
	 *             future version of SCAMP.
	 */
	@Deprecated(forRemoval = true)
	CPU_STATE_12,
	/**
	 * CPU State 13.
	 *
	 * @deprecated Should be unused. Might be changed to another meaning by a
	 *             future version of SCAMP.
	 */
	@Deprecated(forRemoval = true)
	CPU_STATE_13,
	/**
	 * CPU State 14.
	 *
	 * @deprecated Should be unused. Might be changed to another meaning by a
	 *             future version of SCAMP.
	 */
	@Deprecated(forRemoval = true)
	CPU_STATE_14,
	/** Core is idle. User code may be run on it. */
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
