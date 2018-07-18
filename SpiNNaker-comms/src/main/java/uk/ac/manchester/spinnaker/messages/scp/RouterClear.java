package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** A request to clear the router on a chip */
public class RouterClear extends SCPRequest<CheckOKResponse> {
	/**
	 * @param chip
	 *            The coordinates of the chip to clear the router of
	 */
	public RouterClear(HasChipLocation chip) {
		super(new SDPHeader(REPLY_EXPECTED, chip.getScampCore(), 0), CMD_RTR);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Router Clear", CMD_RTR, buffer);
	}
}
