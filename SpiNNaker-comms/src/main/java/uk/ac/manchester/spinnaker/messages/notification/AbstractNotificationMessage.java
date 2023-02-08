/*
 * Copyright (c) 2022 The University of Manchester
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

import java.nio.ByteBuffer;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

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

	/**
	 * @param buffer
	 *            Where to read the command code from. The command code will be
	 *            in the first two bytes; this constructor advances the buffer
	 *            position.
	 */
	protected AbstractNotificationMessage(ByteBuffer buffer) {
		command = NotificationMessageCode.get(buffer.getShort() & MAX_COMMAND);
	}

	/** @param command The command code of the message. */
	public AbstractNotificationMessage(NotificationMessageCode command) {
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
		switch (NotificationMessageCode.get(buffer.getShort(0) & MAX_COMMAND)) {
		case DATABASE_CONFIRMATION:
			return new DatabaseConfirmation(buffer);
		case START_RESUME_NOTIFICATION:
			return new StartResume(buffer);
		case STOP_PAUSE_NOTIFICATION:
			return new PauseStop(buffer);
		default:
			throw new UnsupportedOperationException();
		}
	}
}
