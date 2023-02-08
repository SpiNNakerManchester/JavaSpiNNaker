/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.DEFAULT_PORT;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SpinnakerRequest;

/**
 * Represents an Abstract SCP Request.
 *
 * @param <T>
 *            The type of response expected to the request, if any.
 */
public abstract class SCPRequest<T extends SCPResponse>
		extends SpinnakerRequest {
	private static final int BOOT_DEST_X = 255;

	private static final int BOOT_DEST_Y = 255;

	/**
	 * The location of the default SCAMP.
	 */
	public static final CoreLocation BOOT_MONITOR_CORE =
			new CoreLocation(BOOT_DEST_X, BOOT_DEST_Y, 0);

	/**
	 * The pseudo-location of the default chip.
	 */
	public static final ChipLocation BOOT_CHIP =
			BOOT_MONITOR_CORE.asChipLocation();

	/** The first argument. */
	public final int argument1;

	/** The second argument. */
	public final int argument2;

	/** The third argument. */
	public final int argument3;

	/** The payload data as a buffer, or {@code null} if no payload data. */
	public final ByteBuffer data;

	/** The SCP request header of the message. */
	public final SCPRequestHeader scpRequestHeader;

	/**
	 * The constant value used to indicate that no payload data is in the
	 * message.
	 */
	protected static final ByteBuffer NO_DATA = null;

	/**
	 * Make a header. SCP uses a limited subset of SDP. It <i>always</i> wants a
	 * reply and always talks to a particular SDP port (the port for SCAMP).
	 *
	 * @param core
	 *            The SpiNNaker core that we want to talk to. Should be running
	 *            SCAMP.
	 * @return The SDP header.
	 */
	private static SDPHeader header(HasCoreLocation core) {
		return new SDPHeader(REPLY_EXPECTED, core, DEFAULT_PORT);
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 */
	protected SCPRequest(HasCoreLocation core, SCPCommand command) {
		this(header(core), command, 0, 0, 0, NO_DATA);
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 */
	protected SCPRequest(HasCoreLocation core, SCPCommand command,
			int argument1) {
		this(header(core), command, argument1, 0, 0, NO_DATA);
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 */
	protected SCPRequest(HasCoreLocation core, SCPCommand command,
			int argument1, int argument2) {
		this(header(core), command, argument1, argument2, 0, NO_DATA);
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 * @param argument3
	 *            The third argument.
	 */
	protected SCPRequest(HasCoreLocation core, SCPCommand command,
			int argument1, int argument2, int argument3) {
		this(header(core), command, argument1, argument2, argument3, NO_DATA);
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 * @param argument3
	 *            The third argument.
	 * @param data
	 *            The additional data. Starts at the <i>position</i> and goes to
	 *            the <i>limit</i>. The position and limit of the buffer will
	 *            not be updated by this constructor.
	 */
	protected SCPRequest(HasCoreLocation core, SCPCommand command,
			int argument1, int argument2, int argument3, ByteBuffer data) {
		this(header(core), command, argument1, argument2, argument3, data);
	}

	/**
	 * Create a new request.
	 *
	 * @param sdpHeader
	 *            The header.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 * @param argument3
	 *            The third argument.
	 * @param data
	 *            The additional data. Starts at the <i>position</i> and goes to
	 *            the <i>limit</i>. The position and limit of the buffer will
	 *            not be updated by this constructor.
	 */
	protected SCPRequest(SDPHeader sdpHeader, CommandCode command,
			int argument1, int argument2, int argument3, ByteBuffer data) {
		super(sdpHeader);
		this.scpRequestHeader = new SCPRequestHeader(command);
		this.argument1 = argument1;
		this.argument2 = argument2;
		this.argument3 = argument3;
		this.data = nonNull(data) ? data.duplicate() : null;
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		sdpHeader.addToBuffer(buffer);
		scpRequestHeader.addToBuffer(buffer);
		buffer.putInt(argument1);
		buffer.putInt(argument2);
		buffer.putInt(argument3);
		if (nonNull(data) && data.hasRemaining()) {
			buffer.put(data.duplicate());
		}
	}

	/**
	 * Parse the response to this message.
	 *
	 * @param buffer
	 *            The buffer to parse.
	 * @return The message response.
	 * @throws Exception
	 *             If anything goes wrong with parsing.
	 */
	public abstract T getSCPResponse(ByteBuffer buffer) throws Exception;
}
