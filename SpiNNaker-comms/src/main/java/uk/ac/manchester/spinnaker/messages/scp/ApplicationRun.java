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

import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE3;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_AR;

import java.nio.ByteBuffer;

import jakarta.validation.Valid;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.ValidP;
import uk.ac.manchester.spinnaker.messages.model.AppID;

/**
 * A request to run an application.
 */
public class ApplicationRun extends SCPRequest<CheckOKResponse> {
	private static final int WAIT_BIT = 18;

	/**
	 * @param appID
	 *            The ID of the application to run
	 * @param chip
	 *            The coordinates of the chip to run on
	 * @param processors
	 *            The processors of the chip to run on, between 1 and 17
	 */
	public ApplicationRun(AppID appID, @Valid HasChipLocation chip,
			Iterable<@ValidP Integer> processors) {
		this(appID, chip, processors, false);
	}

	/**
	 * @param appId
	 *            The ID of the application to run
	 * @param chip
	 *            The coordinates of the chip to run on
	 * @param processors
	 *            The processors of the chip to run on, between 1 and 17
	 * @param wait
	 *            True if the processors should enter a "wait" state on starting
	 */
	public ApplicationRun(AppID appId, @Valid HasChipLocation chip,
			Iterable<@ValidP Integer> processors, boolean wait) {
		super(chip.getScampCore(), CMD_AR, argument1(appId, processors, wait));
	}

	private static int argument1(AppID appId, Iterable<Integer> processors,
			boolean wait) {
		int processorMask = 0;
		if (processors != null) {
			for (int p : processors) {
				if (p >= 1 && p < MAX_NUM_CORES) {
					processorMask |= 1 << p;
				}
			}
		}
		processorMask |= appId.appID << BYTE3;
		if (wait) {
			processorMask |= 1 << WAIT_BIT;
		}
		return processorMask;
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Run Application", CMD_AR, buffer);
	}
}
