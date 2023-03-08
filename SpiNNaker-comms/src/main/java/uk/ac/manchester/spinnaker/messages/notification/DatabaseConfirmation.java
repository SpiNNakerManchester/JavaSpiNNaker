/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.notification;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.messages.notification.NotificationMessageCode.DATABASE_CONFIRMATION;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Message which contains the path to the job description database created by
 * the toolchain which is to be used by any software which interfaces with
 * SpiNNaker.
 */
public class DatabaseConfirmation extends AbstractNotificationMessage {
	/**
	 * The path to the database, as seen by the sender's filesystem. Note that
	 * there is a length limit; the overall message must fit in a UDP message.
	 */
	public final String databasePath;

	/** The encoding of the database path into bytes. */
	private static final Charset CHARSET = defaultCharset();

	/**
	 * Create a message without a database path in it. This is used to
	 * acknowledge that the job database has been read and say that the
	 * listening software is ready for the simulation to start.
	 */
	public DatabaseConfirmation() {
		super(DATABASE_CONFIRMATION);
		databasePath = null;
	}

	/**
	 * Create a message with a database path in it.
	 *
	 * @param databasePath
	 *            The path.
	 */
	public DatabaseConfirmation(String databasePath) {
		super(DATABASE_CONFIRMATION);
		this.databasePath = requireNonNull(databasePath);
	}

	/**
	 * Deserialise from a buffer.
	 *
	 * @param data
	 *            The buffer to read from
	 */
	DatabaseConfirmation(ByteBuffer data) {
		super(data);
		if (data.remaining() > 0) {
			if (data.hasArray()) {
				databasePath = new String(data.array(),
						data.arrayOffset() + data.position(), data.remaining(),
						CHARSET);
			} else {
				// Must copy; ugh!
				var ary = new byte[data.remaining()];
				data.get(ary);
				databasePath = new String(ary, CHARSET);
			}
		} else {
			databasePath = null;
		}
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		if (databasePath != null) {
			buffer.put(databasePath.getBytes(CHARSET));
		}
	}
}
