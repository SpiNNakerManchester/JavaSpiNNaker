package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.Signal.STOP;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.TOP_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_NNP;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.Signal;

/** An SCP Request to stop an application. */
public final class ApplicationStop extends SCPRequest<CheckOKResponse> {
	private static final int APP_MASK = 0xFF;
	// TODO Better names for these constants
	private static final int SHIFT = 28;
	private static final int MAGIC1 = 0x3f;
	private static final int MAGIC2 = 5;
	private static final int MAGIC3 = 0x3f;

	private static int argument1() {
		return MAGIC1 << BYTE2;
	}

	private static int argument2(int appID, Signal signal) {
		return (MAGIC2 << SHIFT) | (signal.value << BYTE2) | (APP_MASK << BYTE1)
				| (appID << BYTE0);
	}

	private static int argument3() {
		return (1 << TOP_BIT) | (MAGIC3 << BYTE1);
	}

	/**
	 * @param appID
	 *            The ID of the application, between 0 and 255
	 */
	public ApplicationStop(int appID) {
		super(DEFAULT_MONITOR_CORE, CMD_NNP, argument1(),
				argument2(appID, STOP), argument3());
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Send Stop", CMD_NNP, buffer);
	}
}
