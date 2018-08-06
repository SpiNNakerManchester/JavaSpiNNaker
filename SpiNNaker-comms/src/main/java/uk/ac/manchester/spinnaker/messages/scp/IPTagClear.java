package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.CLR;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.THREE_BITS_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** An SCP Request to clear an IP Tag. */
public class IPTagClear extends SCPRequest<CheckOKResponse> {
	/**
	 * @param chip
	 *            The chip to clear the tag on.
	 * @param tag
	 *            The ID of the tag to clear (0..7)
	 */
	public IPTagClear(HasChipLocation chip, int tag) {
		super(chip.getScampCore(), CMD_IPTAG, argument1(tag), null, null);
	}

	private static Integer argument1(int tag) {
		return (CLR.value << COMMAND_FIELD) | (tag & THREE_BITS_MASK);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new CheckOKResponse("Clear IP Tag", CMD_IPTAG, buffer);
	}
}
