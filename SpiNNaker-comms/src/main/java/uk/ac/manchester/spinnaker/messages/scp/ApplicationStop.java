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

import static uk.ac.manchester.spinnaker.messages.model.Signal.STOP;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE3;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.TOP_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.Constants.APP_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_NNP;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.AppID;

/**
 * An SCP Request to stop an application. There is no response payload.
 * <p>
 * This maps to a call to {@code ff_nn_send()} in {@code scamp-nn.c}, which in
 * turn triggers a call to {@code proc_stop_app()} in {@code scamp-app.c} across
 * all SCAMP instances.
 */
public final class ApplicationStop extends SCPRequest<CheckOKResponse> {
	private static final int SHIFT = 28;

	private static final int NN_CMD_SIG0 = 0;

	private static final int FORWARD = 0x3f;

	private static final int RETRY = 0;

	private static final int SIG0_APP = 5;

	private static final int ADD_ID = 1;

	private static final int ALL_LINKS = 0x3f;

	private static int key() {
		return (NN_CMD_SIG0 << BYTE3) | (FORWARD << BYTE2) | (RETRY << BYTE1);
	}

	private static int data(AppID appID) {
		// Call the SIG0 NN operation, subcode STOP (stop application)
		// No masking of the app ID
		return (SIG0_APP << SHIFT) | (STOP.value << BYTE2) | (APP_MASK << BYTE1)
				| (appID.appID() << BYTE0);
	}

	private static int nnConfig() {
		// Add a suitable ID in SCAMP, send message on all links
		// Number of repeats is zero, inter-repeat delay is zero
		return (ADD_ID << TOP_BIT) | (ALL_LINKS << BYTE1);
	}

	/**
	 * @param appID
	 *            The ID of the application
	 */
	public ApplicationStop(AppID appID) {
		super(BOOT_MONITOR_CORE, CMD_NNP, key(), data(appID), nnConfig());
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Send Stop", CMD_NNP, buffer);
	}
}
