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

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.GET;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.THREE_BITS_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.tags.TagID;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to get an IP tag. The response payload is the
 * {@linkplain TagDescription tag description}.
 * <p>
 * Handled by {@code cmd_iptag()} in {@code scamp-cmd.c} (or {@code bmp_cmd.c},
 * if sent to a BMP).
 */
public class IPTagGet extends SCPRequest<IPTagGet.Response>
		implements ConnectionAwareMessage {
	private final int tag;

	private SCPConnection conn;

	// arg1 = flags[11:8] : timeout : command : dest_port : tag
	private static int argument1(int tagID) {
		return (GET.value << COMMAND_FIELD) | (tagID & THREE_BITS_MASK);
	}

	/**
	 * @param chip
	 *            The chip to get the tag from.
	 * @param tag
	 *            The tag to get the details of.
	 */
	public IPTagGet(HasChipLocation chip,
			@TagID(scamp = true, ephemeral = true) int tag) {
		super(chip.getScampCore(), CMD_IPTAG, argument1(tag), 1);
		this.tag = tag;
	}

	@Override
	public void setConnection(SCPConnection connection) {
		this.conn = connection;
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException, UnknownHostException {
		return new Response(buffer);
	}

	/** An SCP response to a request for an IP tags. */
	protected final class Response
			extends PayloadedResponse<TagDescription, UnknownHostException> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException, UnknownHostException {
			super("Get IP Tag Info", CMD_IPTAG, buffer);
		}

		@Override
		protected TagDescription parse(ByteBuffer buffer)
				throws UnknownHostException {
			requireNonNull(conn,
					"can only describe a tag fully after the message has "
							+ "been sent on a connection");
			return new TagDescription(buffer, sdpHeader.getSource(),
					conn.getRemoteIPAddress(), tag);
		}
	}
}
