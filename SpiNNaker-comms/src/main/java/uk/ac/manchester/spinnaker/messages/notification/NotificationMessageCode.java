/*
 * Copyright (c) 2018-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.notification;

import java.util.HashMap;
import java.util.Map;

/**
 * What command codes are used in the notification protocol.
 */
public enum NotificationMessageCode {
	/** Database handshake with external program. */
	DATABASE_CONFIRMATION(1),
	/** Notify the external devices that the simulation has stopped. */
	STOP_PAUSE_NOTIFICATION(10),
	/** Notify the external devices that the simulation has started. */
	START_RESUME_NOTIFICATION(11);
	private final int value;

	private static final Map<Integer, NotificationMessageCode> MAP =
			new HashMap<>();

	static {
		for (NotificationMessageCode commmand : values()) {
			MAP.put(commmand.value, commmand);
		}
	}

	NotificationMessageCode(int value) {
		this.value = value;
	}

	/** @return The value of this code. */
	public int getValue() {
		return value;
	}

	/**
	 * Get a command from an ID.
	 *
	 * @param command
	 *            the encoded command
	 * @return the ID, or {@code null} if the encoded form was unrecognised.
	 */
	public static NotificationMessageCode get(int command) {
		return MAP.get(command);
	}
}
