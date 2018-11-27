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
package uk.ac.manchester.spinnaker.messages;

import java.nio.ByteBuffer;

/**
 * Represents a serializable message or a piece of a serializable message.
 * <p>
 * Concrete classes that implement this interface that also wish to be
 * deserializable should also support the reverse operation by creating a
 * constructor that takes a single (little-endian) ByteBuffer as its only
 * argument.
 */
public interface SerializableMessage {
	/**
	 * Writes this message into the given buffer as a contiguous range of bytes.
	 * This is so that a message can be sent. Implementors may assume that the
	 * buffer has been configured to be
	 * {@linkplain java.nio.ByteOrder#LITTLE_ENDIAN little-endian} and that its
	 * <i>position</i> is at the point where they should begin writing. Once it
	 * has finished, the <i>position</i> should be immediately after the last
	 * byte written by this method.
	 *
	 * @param buffer
	 *            The buffer to write into.
	 */
	void addToBuffer(ByteBuffer buffer);
}
