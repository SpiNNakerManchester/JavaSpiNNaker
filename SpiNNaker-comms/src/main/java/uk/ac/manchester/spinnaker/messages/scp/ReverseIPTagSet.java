package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.SET;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/**
 * An SCP Request to set a Reverse IP Tag. Reverse IP tags are tags that funnel
 * packets from the outside world to a particular SpiNNaker core.
 *
 * @see IPTagSet
 */
public class ReverseIPTagSet extends SCPRequest<CheckOKResponse> {
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
	public ReverseIPTagSet(HasChipLocation chip, HasCoreLocation destination,
			int port, int tag, int sdpPort) {
		super(new SDPHeader(REPLY_EXPECTED, chip.getScampCore(), 0), CMD_IPTAG,
				argument1(sdpPort, destination, tag),
				argument2(destination, port), 0);
	}

	private static int argument1(int sdpPort, HasCoreLocation destination,
			int tag) {
		final int strip = 1;
		final int reverse = 1;
		return (reverse << 29) | (strip << 28) | (SET.value << 16)
				| ((sdpPort & 0x7) << 13) | (destination.getP() << 8)
				| (tag & 0x7);
	}

	private static int argument2(HasCoreLocation destination, int port) {
		return (destination.getX() << 24) | (destination.getY() << 16)
				| (port & 0xFFFF);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new CheckOKResponse("Set Reverse IP Tag", CMD_IPTAG, buffer);
	}
}
