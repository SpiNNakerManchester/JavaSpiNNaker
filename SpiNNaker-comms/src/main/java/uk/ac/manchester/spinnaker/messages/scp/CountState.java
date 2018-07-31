package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_SIG;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.CPUState;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** An SCP Request to get a count of the cores in a particular state. */
public class CountState extends SCPRequest<CountState.Response> {
	private static final int COUNT_SIGNAL_TYPE = 1;
	private static final int ALL_CORE_MASK = 0xFFFF;
	private static final int APP_MASK = 0xFF;
	private static final int COUNT_OPERATION = 1;
	private static final int COUNT_MODE = 2;

	/**
	 * @param appId
	 *            The ID of the application to run, between 16 and 255
	 * @param state
	 *            The state to count
	 */
	public CountState(int appId, CPUState state) {
		super(new SDPHeader(REPLY_EXPECTED, DEFAULT_MONITOR_CORE, 0), CMD_SIG,
				COUNT_SIGNAL_TYPE, argument2(appId, state), ALL_CORE_MASK);
	}

	private static int argument2(int appId, CPUState state) {
		int data = (APP_MASK << 8) | appId;
		data |= COUNT_OPERATION << 22;
		data |= COUNT_MODE << 20;
		data |= state.value << 16;
		return data;
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request for the number of cores in a given state.
	 */
	public static class Response extends CheckOKResponse {
		/** The count of the number of cores with the requested state. */
		public final int count;

		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("CountState", CMD_SIG, buffer);
			count = buffer.getInt();
		}
	}
}
