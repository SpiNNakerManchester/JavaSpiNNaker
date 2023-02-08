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

/**
 * Commands sent between an application and the monitor processor.
 */
public enum MailboxCommand {
	/** The mailbox is idle. */
	SHM_IDLE(0),
	/** The mailbox contains an SDP message. */
	SHM_MSG(1),
	/** The mailbox contains a non-operation. */
	SHM_NOP(2),
	/** The mailbox contains a signal. */
	SHM_SIGNAL(3),
	/** The mailbox contains a command. */
	SHM_CMD(4);

	/** The SARK value. */
	public final int value;

	private static final Map<Integer, MailboxCommand> MAP =
			makeEnumBackingMap(values(), v -> v.value);

	MailboxCommand(int value) {
		this.value = value;
	}

	/**
	 * Convert SARK value to enum.
	 *
	 * @param value
	 *            The value to convert.
	 * @return The enum member
	 */
	public static MailboxCommand get(int value) {
		return requireNonNull(MAP.get(value),
				"unknown mailbox command: " + value);
	}
}
