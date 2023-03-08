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
package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/**
 * A marker interface for possible data elements in the EIEIO data packet.
 */
public interface AbstractDataElement {
	/**
	 * Writes this message chunk into the given buffer. This is so that a
	 * message can be sent.
	 *
	 * @param buffer
	 *            The buffer to write into.
	 * @param eieioType
	 *            The type of message this is being written into.
	 * @throws IllegalArgumentException
	 *             If this message is incompatible with the given message type.
	 */
	void addToBuffer(ByteBuffer buffer, EIEIOType eieioType);
}
