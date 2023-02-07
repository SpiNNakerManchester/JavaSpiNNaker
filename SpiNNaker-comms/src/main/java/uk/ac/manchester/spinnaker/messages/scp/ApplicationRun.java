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

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE3;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_AR;

import java.nio.ByteBuffer;
import java.util.Collection;

import jakarta.validation.Valid;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.ValidP;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * An SCP request to run an application. The application code must have been
 * placed at the location
 * {@link SystemVariableDefinition#system_sdram_base_address} points at. There
 * is no response payload.
 * <p>
 * Calls {@code proc_start_app()} in {@code scamp-app.c}.
 */
@UsedInJavadocOnly(SystemVariableDefinition.class)
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
			Collection<@ValidP Integer> processors) {
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
			Collection<@ValidP Integer> processors, boolean wait) {
		super(chip.getScampCore(), CMD_AR, idOpMask(appId, processors, wait));
	}

	// @formatter:off
	/*
	 * bits:   [  31-24 |     23-18 |           17-0 ]
	 * labels: [ app_id | app_flags | processor_mask ]
	 *
	 * It is an error to ask for this operation for the SCAMP core.
	 * The only app_flag we use is in the lowest bit: WAIT.
	 */
	// @formatter:on
	private static int idOpMask(AppID appId, Collection<Integer> processors,
			boolean wait) {
		int mask = 0;
		for (int p : requireNonNull(processors,
				"set of processors on chip must be not null")) {
			if (p >= 1 && p < MAX_NUM_CORES) {
				mask |= 1 << p;
			}
		}
		mask |= appId.appID << BYTE3;
		if (wait) {
			mask |= 1 << WAIT_BIT;
		}
		return mask;
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Run Application", CMD_AR, buffer);
	}
}
