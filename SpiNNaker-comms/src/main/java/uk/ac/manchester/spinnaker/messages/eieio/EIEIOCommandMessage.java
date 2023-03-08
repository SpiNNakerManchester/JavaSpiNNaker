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

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

/**
 * An EIEIO message containing a command.
 *
 * @author Sergio Davies
 * @author Donal Fellows
 */
public class EIEIOCommandMessage
		implements EIEIOMessage<EIEIOCommandMessage.Header> {
	// Must be power of 2 (minus 1)
	private static final int MAX_COMMAND = 0x3FFF;

	private static final int FLAG1_BIT = 15;

	private static final int FLAG2_BIT = 14;

	/** The header of the message. */
	private final Header header;

	/**
	 * Constructor used when deserializing.
	 *
	 * @param buffer
	 *            Where to deserialize the message from.
	 */
	EIEIOCommandMessage(ByteBuffer buffer) {
		this.header = new Header(buffer);
	}

	/** @param command The command in the message. */
	public EIEIOCommandMessage(EIEIOCommand command) {
		this.header = new Header(command);
	}

	static EIEIOCommand peekCommand(ByteBuffer buffer) {
		return EIEIOCommandID.get(buffer.getShort(0) & MAX_COMMAND);
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void addToBuffer(ByteBuffer buffer) {
		header.addToBuffer(buffer);
	}

	@Override
	public int minPacketLength() {
		return 2;
	}

	@Override
	public Header getHeader() {
		return header;
	}

	/** EIEIO header for command packets. */
	public static class Header implements EIEIOHeader {
		/** The command ID in this header. */
		public final EIEIOCommand command;

		/**
		 * Create a new command header.
		 *
		 * @param command
		 *            The command.
		 */
		public Header(EIEIOCommand command) {
			this.command = requireNonNull(command, "must supply a command");
		}

		/**
		 * Create a new command header.
		 *
		 * @param command
		 *            The encoded command.
		 */
		public Header(int command) {
			this.command = EIEIOCommandID.get(command);
		}

		/**
		 * Read an EIEIO command header from a buffer.
		 *
		 * @param buffer
		 *            The buffer to read the data from
		 */
		private Header(ByteBuffer buffer) {
			command = EIEIOCommandID.get(buffer.getShort() & MAX_COMMAND);
		}

		@Override
		public void addToBuffer(ByteBuffer buffer) {
			short value = (short) (0 << FLAG1_BIT | 1 << FLAG2_BIT
					| command.getValue());
			buffer.putShort(value);
		}
	}
}
