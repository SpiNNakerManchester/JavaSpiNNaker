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
	 * <p>
	 * Calling this method <em>should not</em> update the internal state of the
	 * message. It <em>should</em> be possible to add the message to multiple
	 * buffers without special precautions by the caller.
	 *
	 * @param buffer
	 *            The buffer to write into.
	 */
	void addToBuffer(ByteBuffer buffer);
}
