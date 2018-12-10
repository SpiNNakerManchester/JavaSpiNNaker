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

import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_SIG;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.Signal;

/** An SCP Request to send a signal to cores. */
public class SendSignal extends SCPRequest<CheckOKResponse> {
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
		super(DEFAULT_MONITOR_CORE, CMD_SIG, signal.type.value,
				argument2(appID, signal), ALL_CORE_MASK);
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
