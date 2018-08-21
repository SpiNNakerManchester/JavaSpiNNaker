package uk.ac.manchester.spinnaker.messages.scp;

import static java.util.stream.IntStream.range;
import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.SET;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.BYTE_SHIFT;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.PORT_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.STRIP_FIELD_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.THREE_BITS_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to set a (forward) IP Tag. Forward IP tags are tags that
 * funnel packets from SpiNNaker to the outside world.
 *
 * @see ReverseIPTagSet
 */
public class IPTagSet extends SCPRequest<CheckOKResponse> {
	/**
	 * @param chip
	 *            The chip to set the tag on.
	 * @param host
	 *            The host address, as an array of 4 bytes.
	 * @param port
	 *            The port, between 0 and 65535
	 * @param tag
	 *            The tag, between 0 and 7
	 * @param strip
	 *            if the SDP header should be stripped from the packet.
	 */
	public IPTagSet(HasChipLocation chip, byte[] host, int port, int tag,
			boolean strip) {
		super(chip.getScampCore(), CMD_IPTAG, argument1(tag, strip),
				argument2(port), argument3(host));
	}

	private static int argument1(int tag, boolean strip) {
		return (strip ? 1 << STRIP_FIELD_BIT : 0) | (SET.value << COMMAND_FIELD)
				| (tag & THREE_BITS_MASK);
	}

	private static int argument2(int port) {
		return port & PORT_MASK;
	}

	private static int argument3(byte[] host) {
		return range(0, host.length).map(i -> host[host.length - 1 - i])
				.reduce(0, (i, j) -> (i << BYTE_SHIFT) | j);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new CheckOKResponse("Set IP Tag", CMD_IPTAG, buffer);
	}
}
