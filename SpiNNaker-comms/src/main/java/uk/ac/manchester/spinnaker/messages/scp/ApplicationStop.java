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
import static uk.ac.manchester.spinnaker.messages.scp.Bits.TOP_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.Constants.APP_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_NNP;

import uk.ac.manchester.spinnaker.messages.model.AppID;

/** An SCP Request to stop an application. */
public final class ApplicationStop extends SimpleRequest {
	// TODO Better names for these constants
	private static final int SHIFT = 28;

	private static final int MAGIC1 = 0x3f;

	private static final int MAGIC2 = 5;

	private static final int MAGIC3 = 0x3f;

	private static int argument1() {
		return MAGIC1 << BYTE2;
	}

	private static int argument2(AppID appID) {
		return (MAGIC2 << SHIFT) | (STOP.value << BYTE2) | (APP_MASK << BYTE1)
				| (appID.appID << BYTE0);
	}

	private static int argument3() {
		return (1 << TOP_BIT) | (MAGIC3 << BYTE1);
	}

	/**
	 * @param appID
	 *            The ID of the application
	 */
	public ApplicationStop(AppID appID) {
		super("Send Stop", BOOT_MONITOR_CORE, CMD_NNP, argument1(),
				argument2(appID), argument3());
	}
}
