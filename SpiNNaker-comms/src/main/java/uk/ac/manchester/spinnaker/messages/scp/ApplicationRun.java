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

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE3;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_AR;

import java.nio.ByteBuffer;
import java.util.Collection;

import javax.validation.Valid;

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
		mask |= appId.appID() << BYTE3;
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
