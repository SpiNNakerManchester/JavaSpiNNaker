/*
 * Copyright (c) 2019 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.scp.RunningCommand.NEW_RUNTIME_ID;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * An SCP Request to update the runtime info on a core.
 */
public class UpdateRuntime extends SimpleRequest {
	/**
	 * @param core
	 *            The SpiNNaker core to update the runtime info of.
	 * @param runTime
	 *            The number of machine timesteps.
	 * @param infiniteRun
	 *            Whether we are doing infinite running.
	 */
	public UpdateRuntime(HasCoreLocation core, int runTime,
			boolean infiniteRun) {
		super("update runtime", core, NEW_RUNTIME_ID, runTime,
				bool(infiniteRun), bool(true), null);
	}

	private static int bool(boolean value) {
		return value ? 1 : 0;
	}
}
