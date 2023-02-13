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

import static java.net.InetAddress.getByAddress;
import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.CORE_MASK;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.PORT_SHIFT;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.THREE_BITS_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagCommand.GET;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.tags.TagID;
import uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime;
import uk.ac.manchester.spinnaker.messages.model.TagDescription;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to get an IP tag. The response payload is the
 * {@linkplain TagDescription tag description}.
 * <p>
 * Handled by {@code cmd_iptag()} in {@code scamp-cmd.c} (or {@code bmp_cmd.c},
 * if sent to a BMP).
 */
public final class IPTagGet extends SCPRequest<IPTagGet.Response>
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
			throws UnexpectedResponseCodeException {
		requireNonNull(conn,
				"can only parse a tag description after the message has "
						+ "been sent on a connection");
		return new Response(buffer);
	}

	private static final int IPV4_BYTES = 4;

	private static final int MAC_BYTES = 6;

	/** An SCP response to a request for an IP tags. */
	protected final class Response
			extends PayloadedResponse<TagDescription, RuntimeException> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Get IP Tag Info", CMD_IPTAG, buffer);
		}

		@Override
		protected TagDescription parse(ByteBuffer buffer) {
			var ipBytes = new byte[IPV4_BYTES];
			buffer.get(ipBytes);

			var macAddress = new byte[MAC_BYTES];
			buffer.get(macAddress);

			int port = Short.toUnsignedInt(buffer.getShort());
			var timeout = IPTagTimeOutWaitTime.get(buffer.getShort());
			short flags = buffer.getShort();
			int count = buffer.getInt();
			int rxPort = Short.toUnsignedInt(buffer.getShort());
			int y = Byte.toUnsignedInt(buffer.get());
			int x = Byte.toUnsignedInt(buffer.get());
			int pp = Byte.toUnsignedInt(buffer.get()); // processor+port

			try {
				return new TagDescription(count, flags, getByAddress(ipBytes),
						macAddress, port, rxPort,
						new CoreLocation(x, y, pp & CORE_MASK),
						(pp >>> PORT_SHIFT) & THREE_BITS_MASK, timeout,
						sdpHeader.getSource().asChipLocation(),
						conn.getRemoteIPAddress(), tag);
			} catch (UnknownHostException unexpectedException) {
				return null;
			}
		}
	}
}
