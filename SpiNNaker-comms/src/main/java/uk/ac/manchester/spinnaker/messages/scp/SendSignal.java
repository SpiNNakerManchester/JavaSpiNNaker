/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.Constants.ALL_CORE_SIGNAL_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.Constants.APP_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_SIG;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.Signal;

/**
 * An SCP Request to send a signal to cores. This supports any signal that uses
 * either {@linkplain Signal.Type#MULTICAST multicast} or
 * {@linkplain Signal.Type#NEAREST_NEIGHBOUR nearest neighbour} propagation
 * rules; the difference between propagation types is not normally important to
 * user code. There is no response payload.
 * <p>
 * See {@code signal_app()} in {@code scamp-app.c} for where these signals
 * handled or transferred to user code, and {@code sark_int()} in
 * {@code sark_base.c} for the normal user-code handlers.
 *
 * @see CountState
 */
public class SendSignal extends SCPRequest<CheckOKResponse> {
	/**
	 * @param appID
	 *            The ID of the application to signal (only for multicast
	 *            signals).
	 * @param signal
	 *            The signal to send.
	 */
	public SendSignal(AppID appID, Signal signal) {
		super(BOOT_MONITOR_CORE, CMD_SIG, signal.type.value,
				data(appID, signal), ALL_CORE_SIGNAL_MASK);
	}

	// @formatter:off
	/*
	 * [ 31-24  |  23-16 | 15-8 |    7-0 ]
	 * [ unused | signal | mask | app_id ]
	 */
	// @formatter:on
	private static int data(AppID appID, Signal signal) {
		return (signal.value << BYTE2) | (APP_MASK << BYTE1)
				| (appID.appID << BYTE0);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Send Signal", CMD_SIG, buffer);
	}
}
