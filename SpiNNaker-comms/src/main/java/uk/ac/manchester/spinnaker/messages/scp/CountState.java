/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.Signal.Type.POINT_TO_POINT;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.Constants.ALL_CORE_SIGNAL_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.Constants.APP_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_SIG;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.CPUState;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to get a count of the cores in a particular state. The
 * response payload is the integer count.
 * <p>
 * Actual adding up of states is in {@code proc_process()} and
 * {@code p2p_region()} in {@code scamp-cmd.c}. This is the main use of
 * point-to-point signals (and the only one exposed to users).
 */
public final class CountState extends SCPRequest<CountState.Response> {
	/* enum send_reg_ctrl */
	private static final int APP_STAT = 1;

	/* enum state_coalesce_mode */
	private static final int MODE_SUM = 2;

	private static final int OP_SHIFT = 22;

	private static final int MODE_SHIFT = 20;

	/**
	 * @param appID
	 *            The ID of the application to count states of
	 * @param state
	 *            The state to count
	 */
	public CountState(AppID appID, CPUState state) {
		super(BOOT_MONITOR_CORE, CMD_SIG, POINT_TO_POINT.value,
				data(appID, state), ALL_CORE_SIGNAL_MASK);
	}

	// @formatter:off
	/*
	 * [  31-28 | 27-26 |  25-24 | 23-22 | 21-20 | 19-16 |     15-8 |    7-0 ]
	 * [ unused | level | unused |    op |  mode | state | app_mask | app_id ]
	 */
	// @formatter:on
	private static int data(AppID appId, CPUState state) {
		int data = (APP_MASK << BYTE1) | (appId.appID() << BYTE0);
		data |= APP_STAT << OP_SHIFT;
		data |= MODE_SUM << MODE_SHIFT;
		data |= state.value << BYTE2;
		return data;
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request for the number of cores in a given state.
	 */
	protected static final class Response
			extends PayloadedResponse<Integer, RuntimeException> {
		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("CountState", CMD_SIG, buffer);
		}

		@Override
		protected Integer parse(ByteBuffer buffer) {
			return buffer.getInt();
		}
	}
}
