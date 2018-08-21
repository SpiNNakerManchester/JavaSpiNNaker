package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_VER;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;

/** An SCP request to read the version of software running on a core. */
public class GetVersion extends SCPRequest<GetVersion.Response> {
	/**
	 * @param core
	 *            The location of the core to read from.
	 */
	public GetVersion(HasCoreLocation core) {
		super(core, CMD_VER);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for the version of software running. */
	public static final class Response extends CheckOKResponse {
		/** The version information received. */
		public final VersionInfo versionInfo;

		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Version", CMD_VER, buffer);
			versionInfo = new VersionInfo(buffer);
		}
	}
}
