package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_VER;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;

/**
 * An SCP request to read the version of software running on a board's BMP.
 */
public class GetBMPVersion extends BMPRequest<GetBMPVersion.Response> {
	/**
	 * @param board
	 *            The board to get the version from
	 */
	public GetBMPVersion(int board) {
		super(board, CMD_VER);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for the version of software running. */
	public final class Response extends BMPRequest.BMPResponse {
		/** The version information received. */
		public final VersionInfo versionInfo;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read ADC", CMD_VER, buffer);
			versionInfo = new VersionInfo(buffer);
		}
	}
}
