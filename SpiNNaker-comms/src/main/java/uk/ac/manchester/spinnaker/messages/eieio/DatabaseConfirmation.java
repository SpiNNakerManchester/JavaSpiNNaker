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
package uk.ac.manchester.spinnaker.messages.eieio;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.DATABASE_CONFIRMATION;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Packet which contains the path to the database created by the toolchain which
 * is to be used by any software which interfaces with SpiNNaker.
 */
public class DatabaseConfirmation extends EIEIOCommandMessage {
	/**
	 * The path to the database. Note that there is a length limit; the overall
	 * message must fit in a SpiNNaker UDP message.
	 */
	public final String databasePath;

	/**
	 * The encoding of the database path into bytes.
	 */
	private static final Charset CHARSET = defaultCharset();

	/**
	 * Create a message without a database path in it.
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
				databasePath = new String(data.array(), data.position(),
						data.remaining(), CHARSET);
			} else {
				// Must copy; ugh!
				byte[] ary = new byte[data.remaining()];
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
