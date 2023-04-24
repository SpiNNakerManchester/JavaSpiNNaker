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

import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/**
 * An EIEIO message's basic operations.
 *
 * @param <Header>
 *            The type of header on this message.
 */
public interface EIEIOMessage<Header extends EIEIOHeader>
		extends SerializableMessage {
	/** @return the header of this message. */
	Header getHeader();

	/** @return the minimum length of a message instance in bytes. */
	int minPacketLength();
}