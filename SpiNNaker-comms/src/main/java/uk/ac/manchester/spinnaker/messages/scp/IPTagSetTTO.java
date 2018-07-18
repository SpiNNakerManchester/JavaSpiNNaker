package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.TTO;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** An SCP request to set the transient timeout for future SCP requests */
public class IPTagSetTTO extends SCPRequest<IPTagGetInfo.Response> {
	/**
	 * @param chip
	 *            The chip to set the tag timout on.
	 * @param tagTimeout
	 *            The timeout to set.
	 */
	public IPTagSetTTO(HasChipLocation chip, IPTagTimeOutWaitTime tagTimeout) {
		super(new SDPHeader(REPLY_EXPECTED, chip.getScampCore(), 0), CMD_IPTAG,
				TTO.value << 16, tagTimeout.value, null);
	}

	@Override
	public IPTagGetInfo.Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new IPTagGetInfo.Response(buffer);
	}
}
