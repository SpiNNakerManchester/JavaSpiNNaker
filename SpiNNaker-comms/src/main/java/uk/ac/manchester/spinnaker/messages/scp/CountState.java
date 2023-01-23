/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
 * An SCP Request to get a count of the cores in a particular state.
 * <p>
 * Actual adding up of states is in {@code proc_process()} and
 * {@code p2p_region()} in {@code scamp-cmd.c}.
 */
public class CountState extends SCPRequest<CountState.Response> {
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

	/*
	 * [  31-28 | 27-26 | 25-24  | 23-22 | 21-20 | 19-16 |     15-8 |    7-0 ]
	 * [ unused | level | unused |    op |  mode | state | app_mask | app_id ]
	 */
	private static int data(AppID appId, CPUState state) {
		int data = (APP_MASK << BYTE1) | (appId.appID << BYTE0);
		data |= APP_STAT << OP_SHIFT;
		data |= MODE_SUM << MODE_SHIFT;
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
