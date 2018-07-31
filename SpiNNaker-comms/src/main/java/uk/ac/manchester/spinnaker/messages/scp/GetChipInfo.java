package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_INFO;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.ChipSummaryInfo;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** An SCP request to read the chip information from a core. */
public class GetChipInfo extends SCPRequest<GetChipInfo.Response> {
	/**
	 * @param chip
	 *            the chip to read from
	 */
	public GetChipInfo(HasChipLocation chip) {
		this(chip, false);
	}

	/**
	 * @param chip
	 *            the chip to read from
	 * @param withSize
	 *            Whether the size should be included in the response
	 */
	public GetChipInfo(HasChipLocation chip, boolean withSize) {
		super(new SDPHeader(REPLY_EXPECTED, chip.getScampCore(), 0), CMD_INFO,
				argument1(withSize), null, null);
	}

	private static int argument1(boolean withSize) {
		// Bits 0-4 + bit 6 = all information except size
		// Bits 0-6 = all information including size
		return withSize ? 0x7F : 0x5F;
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for the version of software running. */
	public static class Response extends CheckOKResponse {
		/** The chip information received. */
		public final ChipSummaryInfo chipInfo;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Version", CMD_INFO, buffer);
			this.chipInfo = new ChipSummaryInfo(buffer, sdpHeader.getSource());
		}
	}
}
