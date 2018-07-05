package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.IPTagCommand.CLR;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** An SCP Request to clear an IP Tag */
public class IPTagClear extends SCPRequest<CheckOKResponse> {
	/**
	 * @param chip
	 *            The chip to clear the tag on.
	 * @param tag
	 *            The ID of the tag to clear (0..7)
	 */
	public IPTagClear(HasChipLocation chip, int tag) {
		super(new SDPHeader(REPLY_EXPECTED,
				new CoreLocation(chip.getX(), chip.getY(), 0), 0),
				new SCPRequestHeader(CMD_IPTAG), argument1(tag), null, null);
	}

	private static Integer argument1(int tag) {
		return (CLR.value << 16) | (tag & 0x7);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new CheckOKResponse("Clear IP Tag", CMD_IPTAG, buffer);
	}
}
