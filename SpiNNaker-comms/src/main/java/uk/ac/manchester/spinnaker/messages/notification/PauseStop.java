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

import static uk.ac.manchester.spinnaker.messages.notification.NotificationMessageCode.STOP_PAUSE_NOTIFICATION;

import java.nio.ByteBuffer;

/**
 * Packet which indicates that the toolchain has paused or stopped.
 */
public final class PauseStop extends NotificationMessage {
	/** Create an instance. */
	public PauseStop() {
		super(STOP_PAUSE_NOTIFICATION);
	}

	PauseStop(ByteBuffer buffer) {
		super(buffer);
	}
}
