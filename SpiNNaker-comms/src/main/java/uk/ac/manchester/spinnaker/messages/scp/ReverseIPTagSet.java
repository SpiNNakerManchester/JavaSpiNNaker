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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.tags.TagID;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.utils.validation.UDPPort;

/**
 * An SCP Request to set a Reverse IP Tag. Reverse IP tags are tags that funnel
 * packets from the outside world to a particular SpiNNaker core.
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
