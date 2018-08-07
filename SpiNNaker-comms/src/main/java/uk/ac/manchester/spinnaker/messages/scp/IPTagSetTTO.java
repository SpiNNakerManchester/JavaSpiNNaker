package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.TTO;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** An SCP request to set the transient timeout for future SCP requests. */
public class IPTagSetTTO extends SCPRequest<IPTagGetInfo.Response> {
	/**
	 * @param chip
	 *            The chip to set the tag timout on.
	 * @param tagTimeout
	 *            The timeout to set.
	 */
	public IPTagSetTTO(HasChipLocation chip, IPTagTimeOutWaitTime tagTimeout) {
		super(chip.getScampCore(), CMD_IPTAG, TTO.value << COMMAND_FIELD,
				tagTimeout.value, null);
	}

	@Override
	public IPTagGetInfo.Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new IPTagGetInfo.Response(buffer);
	}
}
