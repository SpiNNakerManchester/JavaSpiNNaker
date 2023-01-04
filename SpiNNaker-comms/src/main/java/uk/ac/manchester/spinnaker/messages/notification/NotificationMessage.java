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

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/**
 * A notification message.
 *
 * @author Donal Fellows
 */
public abstract sealed class NotificationMessage implements SerializableMessage
		permits DatabaseConfirmation, PauseStop, StartResume {
	// Must be power of 2 (minus 1)
	private static final int MAX_COMMAND = 0x3FFF;

	private static final int FLAG1_BIT = 15;

	private static final int FLAG2_BIT = 14;

	/** The command code of the message. */
	private final NotificationMessageCode command;

	/**
	 * @param buffer
	 *            Where to read the command code from. The command code will be
	 *            in the first two bytes; this constructor advances the buffer
	 *            position.
	 */
	protected NotificationMessage(ByteBuffer buffer) {
		command = NotificationMessageCode.get(buffer.getShort() & MAX_COMMAND);
	}

	/** @param command The command code of the message. */
	public NotificationMessage(NotificationMessageCode command) {
		this.command = command;
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void addToBuffer(ByteBuffer buffer) {
		short value = (short) (0 << FLAG1_BIT | 1 << FLAG2_BIT
				| command.getValue());
		buffer.putShort(value);
	}

	/**
	 * Create a notification message instance from a buffer holding a received
	 * message.
	 *
	 * @param buffer
	 *            The received message data buffer.
	 * @return The parsed message.
	 * @throws UnsupportedOperationException
	 *             If the message is not understood.
	 */
	public static NotificationMessage build(ByteBuffer buffer) {
		return switch (NotificationMessageCode
				.get(buffer.getShort(0) & MAX_COMMAND)) {
		case DATABASE_CONFIRMATION -> new DatabaseConfirmation(buffer);
		case START_RESUME_NOTIFICATION -> new StartResume(buffer);
		case STOP_PAUSE_NOTIFICATION -> new PauseStop(buffer);
		default -> throw new UnsupportedOperationException();
		};
	}
}
