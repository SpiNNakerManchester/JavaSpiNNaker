package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.TTO;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** An SCP Request information about IP tags. */
public class IPTagGetInfo extends SCPRequest<IPTagGetInfo.Response> {
	private static final int IPTAG_MAX = 255;

	/**
	 * @param chip
	 *            The chip to query for information.
	 */
	public IPTagGetInfo(HasChipLocation chip) {
		super(chip.getScampCore(), CMD_IPTAG, TTO.value << COMMAND_FIELD,
				IPTAG_MAX, null);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new IPTagGetInfo.Response(buffer);
	}

	/** An SCP response to a request for information about IP tags. */
	public static class Response extends CheckOKResponse {
		/**
		 * The timeout for transient IP tags (i.e., responses to SCP commands).
		 */
		public final IPTagTimeOutWaitTime transientTimeout;
		/** The count of the IP tag pool size. */
		public final int poolSize;
		/** The count of the number of fixed IP tag entries. */
		public final int fixedSize;

		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Get IP Tag Info", CMD_IPTAG, buffer);
			transientTimeout = IPTagTimeOutWaitTime.get(buffer.get());
			buffer.get(); // skip 1
			poolSize = Byte.toUnsignedInt(buffer.get());
			fixedSize = Byte.toUnsignedInt(buffer.get());
		}
	}
}
