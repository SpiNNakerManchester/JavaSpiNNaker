/*
 * Copyright (c) 2018-2019 The University of Manchester
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

import static java.lang.Byte.toUnsignedInt;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.SET;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.BYTE_SHIFT;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.PORT_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.STRIP_FIELD_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.THREE_BITS_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.USE_SENDER_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to set a (forward) IP Tag. Forward IP tags are tags that
 * funnel packets from SpiNNaker to the outside world.
 *
 * @see ReverseIPTagSet
 */
public class IPTagSet extends SCPRequest<CheckOKResponse> {
	private static final Logger log = getLogger(IPTagSet.class);

	private static final int INADDRSZ = 4;

	private static final byte[] INADDR_ANY = new byte[INADDRSZ];

	/**
	 * @param chip
	 *            The chip to set the tag on.
	 * @param host
	 *            The host address, as an array of 4 bytes. May be {@code null}
	 *            to use the ANY address.
	 * @param port
	 *            The port, between 0 and 65535
	 * @param tag
	 *            The tag, between 0 and 7
	 * @param strip
	 *            if the SDP header should be stripped from the packet.
	 * @param useSender
	 *            if the sender's IP address and port should be used.
	 */
	public IPTagSet(HasChipLocation chip, byte[] host, int port, int tag,
			boolean strip, boolean useSender) {
		super(chip.getScampCore(), CMD_IPTAG, argument1(tag, strip, useSender),
				argument2(port), argument3(host));
		if (useSender && nonNull(host) && !Arrays.equals(host, INADDR_ANY)) {
			log.warn("IPTag has real host address but useSender was true");
		}
	}

	private static int argument1(int tag, boolean strip, boolean useSender) {
		return (strip ? 1 << STRIP_FIELD_BIT : 0)
				| (useSender ? 1 << USE_SENDER_BIT : 0)
				| (SET.value << COMMAND_FIELD) | (tag & THREE_BITS_MASK);
	}

	private static int argument2(int port) {
		return port & PORT_MASK;
	}

	private static int argument3(byte[] host) {
		if (isNull(host)) {
			return 0;
		}
		return range(0, host.length)
				.map(i -> toUnsignedInt(host[host.length - 1 - i]))
				.reduce(0, (i, j) -> (i << BYTE_SHIFT) | j);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new CheckOKResponse("Set IP Tag", CMD_IPTAG, buffer);
	}
}
