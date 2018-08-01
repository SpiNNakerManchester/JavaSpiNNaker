package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_SIG;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.Signal;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** An SCP Request to send a signal to cores. */
public class SendSignal extends SCPRequest<CheckOKResponse> {
	private static final int BYTE2 = 16;
	private static final int BYTE1 = 8;
	private static final int BYTE0 = 0;
	private static final int ALL_CORE_MASK = 0xFFFF;
	private static final int APP_MASK = 0xFF;
	private static final int MAX_APP_ID = 255;

	/**
	 * @param appID
	 *            The ID of the application to run, between 16 and 255
	 * @param signal
	 *            The coordinates of the chip to run on
	 */
	public SendSignal(int appID, Signal signal) {
		super(new SDPHeader(REPLY_EXPECTED, DEFAULT_MONITOR_CORE, 0), CMD_SIG,
				signal.type.value, argument2(appID, signal), ALL_CORE_MASK);
		if (appID < 0 || appID > MAX_APP_ID) {
			throw new IllegalArgumentException(
					"appID must be between 0 and 255");
		}
	}

	private static int argument2(int appID, Signal signal) {
		return (signal.value << BYTE2) | (APP_MASK << BYTE1) | (appID << BYTE0);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Send Signal", CMD_SIG, buffer);
	}
}
