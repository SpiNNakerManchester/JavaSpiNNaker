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

import java.util.HashMap;
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

	private static final Map<Integer, MailboxCommand> MAP = new HashMap<>();
	static {
		for (MailboxCommand v : values()) {
			MAP.put(v.value, v);
		}
	}

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
