/*
 * Copyright (c) 2018 The University of Manchester
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
	 * @hidden
	 */
	@Deprecated(forRemoval = true)
	CPU_STATE_12,
	/**
	 * CPU State 13.
	 *
	 * @deprecated Should be unused. Might be changed to another meaning by a
	 *             future version of SCAMP.
	 * @hidden
	 */
	@Deprecated(forRemoval = true)
	CPU_STATE_13,
	/**
	 * CPU State 14.
	 *
	 * @deprecated Should be unused. Might be changed to another meaning by a
	 *             future version of SCAMP.
	 * @hidden
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
