package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.Signal.STOP;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_NNP;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.Signal;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** An SCP Request to stop an application. */
public final class ApplicationStop extends SCPRequest<CheckOKResponse> {
	private static final int APP_MASK = 0xFF;

	private static int argument1() {
		return 0x3f << 16;
	}

	private static int argument2(int appID, Signal signal) {
		return (5 << 28) | (signal.value << 16) | (APP_MASK << 8) | appID;
	}

	private static int argument3() {
		return (1 << 31) | (0x3f << 8);
	}

	/**
	 * @param appID
	 *            The ID of the application, between 0 and 255
	 */
	public ApplicationStop(int appID) {
		super(new SDPHeader(REPLY_EXPECTED, DEFAULT_MONITOR_CORE, 0), CMD_NNP,
				argument1(), argument2(appID, STOP), argument3());
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Send Stop", CMD_NNP, buffer);
	}
}
