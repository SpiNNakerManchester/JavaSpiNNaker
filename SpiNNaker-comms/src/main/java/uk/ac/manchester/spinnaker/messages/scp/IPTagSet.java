/*
 * Copyright (c) 2018-2019 The University of Manchester
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

import static java.lang.Byte.toUnsignedInt;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.BYTE_SHIFT;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.PORT_MASK;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.STRIP_FIELD_BIT;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.THREE_BITS_MASK;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.USE_SENDER_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagCommand.SET;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.tags.TagID;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.utils.validation.UDPPort;

/**
 * A request to set a (forward) IP Tag. Forward IP tags are tags that
 * funnel packets from SpiNNaker to the outside world. There is no response
 * payload.
 * <p>
 * Handled by {@code cmd_iptag()} in {@code scamp-cmd.c} (or {@code bmp_cmd.c},
 * if sent to a BMP).
 *
 * @see ReverseIPTagSet
 */
public class IPTagSet extends SCPRequest<EmptyResponse> {
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
	public IPTagSet(@Valid @NotNull HasChipLocation chip,
			@Size(min = INADDRSZ, max = INADDRSZ) byte[] host,
			@UDPPort int port, @TagID int tag, boolean strip,
			boolean useSender) {
		super(chip.getScampCore(), CMD_IPTAG, argument1(tag, strip, useSender),
				argument2(port), argument3(host));
		if (useSender && host != null && !Arrays.equals(host, INADDR_ANY)) {
			log.warn("IPTag has real host address but useSender was true");
		}
	}

	// arg1 = flags[11:8] : timeout : command : dest_port : tag
	private static int argument1(int tag, boolean strip, boolean useSender) {
		return (strip ? 1 << STRIP_FIELD_BIT : 0)
				| (useSender ? 1 << USE_SENDER_BIT : 0)
				| (SET.value << COMMAND_FIELD) | (tag & THREE_BITS_MASK);
	}

	// arg2 = dest_addr : port
	private static int argument2(int port) {
		return port & PORT_MASK;
	}

	private static int argument3(byte[] host) {
		if (host == null) {
			return 0;
		}
		return range(0, host.length)
				.map(i -> toUnsignedInt(host[host.length - 1 - i]))
				.reduce(0, (i, j) -> (i << BYTE_SHIFT) | j);
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("Set IP Tag", CMD_IPTAG, buffer);
	}
}
