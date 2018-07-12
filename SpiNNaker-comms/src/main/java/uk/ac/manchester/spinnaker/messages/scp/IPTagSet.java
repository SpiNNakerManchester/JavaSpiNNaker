package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.SET;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

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
		super(new SDPHeader(REPLY_EXPECTED, chip.getScampCore(), 0),
				new SCPRequestHeader(CMD_IPTAG), argument1(tag, strip),
				argument2(port), argument3(host));
	}

	private static int argument1(int tag, boolean strip) {
		return (strip ? 1 << 28 : 0) | (SET.value << 16) | (tag & 0x7);
	}

	private static int argument2(int port) {
		return port & 0xFFFF;
	}

	private static int argument3(byte[] host) {
		return (host[3] << 24) | (host[2] << 16) | (host[1] << 8) | host[0];
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new CheckOKResponse("Set IP Tag", CMD_IPTAG, buffer);
	}
}
