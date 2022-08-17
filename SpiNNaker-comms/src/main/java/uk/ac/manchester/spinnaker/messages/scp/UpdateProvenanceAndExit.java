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

import static uk.ac.manchester.spinnaker.messages.scp.RunningCommand.UPDATE_PROVENCE_REGION_AND_EXIT;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to update the runtime info on a core.
 */
public class UpdateProvenanceAndExit extends SCPRequest<NoResponse> {
	/**
	 * @param core
	 *            The SpiNNaker core to update the provenance info of.
	 */
	public UpdateProvenanceAndExit(HasCoreLocation core) {
		super(new RunningSDPHeader(core, false),
				UPDATE_PROVENCE_REGION_AND_EXIT, 0, 0, 0, null);
	}

	@Override
	public NoResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new NoResponse("update provenance and exit",
				UPDATE_PROVENCE_REGION_AND_EXIT, buffer);
	}
}
