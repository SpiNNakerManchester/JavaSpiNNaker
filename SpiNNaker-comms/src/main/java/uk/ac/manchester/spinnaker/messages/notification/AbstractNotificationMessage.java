/*
 * Copyright (c) 2022 The University of Manchester
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

import java.nio.ByteBuffer;

/**
 * An notification message.
 *
 * @author Donal Fellows
 */
public abstract class AbstractNotificationMessage
		implements NotificationMessage {
	// Must be power of 2 (minus 1)
	private static final int MAX_COMMAND = 0x3FFF;

	private static final int FLAG1_BIT = 15;

	private static final int FLAG2_BIT = 14;

	/** The command code of the message. */
	private final NotificationMessageCode command;

	protected AbstractNotificationMessage(ByteBuffer buffer) {
		command = NotificationMessageCode.get(buffer.getShort() & MAX_COMMAND);
	}

	public AbstractNotificationMessage(NotificationMessageCode command) {
		this.command = command;
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		short value = (short) (0 << FLAG1_BIT | 1 << FLAG2_BIT
				| command.getValue());
		buffer.putShort(value);
	}

	public static NotificationMessage build(ByteBuffer buffer) {
		switch (NotificationMessageCode.get(buffer.getShort(0) & MAX_COMMAND)) {
		case DATABASE_CONFIRMATION:
			return new DatabaseConfirmation(buffer);
		case START_RESUME_NOTIFICATION:
			return new StartResume(buffer);
		case STOP_PAUSE_NOTIFICATION:
			return new PauseStop(buffer);
		default:
			// TODO Auto-generated method stub
			return null;
		}
	}
}
