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

import static uk.ac.manchester.spinnaker.messages.scp.ReinjectorCommand.RESET_COUNTERS;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to reset the statistics counters of the dropped packet
 * reinjection. There is no response payload.
 * <p>
 * Handled by {@code reinjection_reset_counters()} in
 * {@code extra_monitor_support.c}.
 */
public class ResetReinjectionCounters extends ReinjectorRequest<EmptyResponse> {
	/**
	 * @param core
	 *            The coordinates of the monitor core.
	 */
	public ResetReinjectionCounters(HasCoreLocation core) {
		super(core, RESET_COUNTERS);
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("Reset dropped packet reinjection counters",
				RESET_COUNTERS, buffer);
	}
}
