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

import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.SET;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.DEST_P_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.DEST_X_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.DEST_Y_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.PORT_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.REVERSE_FIELD_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.SDP_PORT_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.STRIP_FIELD_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.THREE_BITS_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.nio.ByteBuffer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.tags.TagID;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.utils.validation.UDPPort;

/**
 * An SCP Request to set a Reverse IP Tag. Reverse IP tags are tags that funnel
 * packets from the outside world to a particular SpiNNaker core. There is no
 * response payload.
 * <p>
 * Handled by {@code cmd_iptag()} in {@code scamp-cmd.c} (or {@code bmp_cmd.c},
 * if sent to a BMP).
 *
 * @see IPTagSet
 */
public class ReverseIPTagSet extends SCPRequest<CheckOKResponse> {
	/**
	 * @param chip
	 *            The chip to set the tag on.
	 * @param destination
	 *            The coordinates of the destination processor.
	 * @param port
	 *            The port, between 0 and 65535.
	 * @param tag
	 *            The tag, between 0 and 7.
	 * @param sdpPort
	 *            The SDP port associated with this tag.
	 */
	public ReverseIPTagSet(@Valid @NotNull HasChipLocation chip,
			@Valid @NotNull HasCoreLocation destination, @UDPPort int port,
			@TagID int tag, int sdpPort) {
		super(chip.getScampCore(), CMD_IPTAG,
				argument1(sdpPort, destination, tag),
				argument2(destination, port), 0);
	}

	// arg1 = flags[11:8] : timeout : command : dest_port : tag
	private static int argument1(int sdpPort, HasCoreLocation destination,
			int tag) {
		final int strip = 1;
		final int reverse = 1;
		return (reverse << REVERSE_FIELD_BIT) | (strip << STRIP_FIELD_BIT)
				| (SET.value << COMMAND_FIELD)
				| ((sdpPort & THREE_BITS_MASK) << SDP_PORT_FIELD)
				| (destination.getP() << DEST_P_FIELD)
				| (tag & THREE_BITS_MASK);
	}

	// arg2 = dest_addr : port
	private static int argument2(HasCoreLocation destination, int port) {
		return (destination.getX() << DEST_X_FIELD)
				| (destination.getY() << DEST_Y_FIELD) | (port & PORT_MASK);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new CheckOKResponse("Set Reverse IP Tag", CMD_IPTAG, buffer);
	}
}
