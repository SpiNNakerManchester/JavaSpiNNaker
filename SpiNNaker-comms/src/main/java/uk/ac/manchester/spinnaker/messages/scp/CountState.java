package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.Signal.Type.POINT_TO_POINT;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_SIG;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.CPUState;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** An SCP Request to get a count of the cores in a particular state. */
public class CountState extends SCPRequest<CountState.Response> {
	private static final int ALL_CORE_MASK = 0xFFFF;
	private static final int APP_MASK = 0xFF;
	private static final int COUNT_OPERATION = 1;
	private static final int COUNT_MODE = 2;
	private static final int OP_SHIFT = 22;
	private static final int MODE_SHIFT = 20;

	/**
	 * @param appId
	 *            The ID of the application to run, between 16 and 255
	 * @param state
	 *            The state to count
	 */
	public CountState(int appId, CPUState state) {
		super(DEFAULT_MONITOR_CORE, CMD_SIG, POINT_TO_POINT.value,
				argument2(appId, state), ALL_CORE_MASK);
	}

	private static int argument2(int appId, CPUState state) {
		int data = (APP_MASK << BYTE1) | (appId << BYTE0);
		data |= COUNT_OPERATION << OP_SHIFT;
		data |= COUNT_MODE << MODE_SHIFT;
		data |= state.value << BYTE2;
		return data;
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request for the number of cores in a given state.
	 */
	public static final class Response extends CheckOKResponse {
		/** The count of the number of cores with the requested state. */
		public final int count;

		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("CountState", CMD_SIG, buffer);
			count = buffer.getInt();
		}
	}
}
