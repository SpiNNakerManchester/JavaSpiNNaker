package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.IPTagCommand.TTO;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** An SCP Request information about IP tags */
public class IPTagGetInfo extends SCPRequest<IPTagGetInfo.Response> {
	private static final int IPTAG_MAX = 255;

	/**
	 * @param chip
	 *            The chip to query for information.
	 */
	public IPTagGetInfo(HasChipLocation chip, int tagTimeout) {
		super(new SDPHeader(REPLY_EXPECTED, chip.getScampCore(), 0),
				new SCPRequestHeader(CMD_IPTAG), TTO.value << 16, IPTAG_MAX,
				null);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new IPTagGetInfo.Response(buffer);
	}

	/** An SCP response to a request for information about IP tags */
	public static class Response extends CheckOKResponse {
		/**
		 * The timeout for transient IP tags (i.e. responses to SCP commands)
		 */
		public final IPTagTimeOutWaitTime transientTimeout;
		/** The count of the IP tag pool size */
		public final byte poolSize;
		/** The count of the number of fixed IP tag entries */
		public final byte fixedSize;

		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Get IP Tag Info", CMD_IPTAG, buffer);
			transientTimeout = IPTagTimeOutWaitTime.get(buffer.get());
			buffer.get(); // skip 1
			poolSize = buffer.get();
			fixedSize = buffer.get();
		}
	}
}
