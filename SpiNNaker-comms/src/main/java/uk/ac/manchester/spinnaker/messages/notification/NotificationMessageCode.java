/*
 * Copyright (c) 2018-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.notification;

import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

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
			makeEnumBackingMap(values(), v -> v.value);

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
