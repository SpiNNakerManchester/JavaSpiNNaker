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

import static uk.ac.manchester.spinnaker.messages.scp.RunningCommand.CLEAR_IOBUF;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to clear the IOBUF on a core. There is no response payload.
 * <p>
 * This calls {@code sark_io_buf_reset()} in {@code sark_io.c} (via
 * {@code simulation_control_scp_callback()} in {@code simulation.c}).
 */
public class ClearIOBUF extends FECRequest<EmptyResponse> {
	/**
	 * @param core
	 *            The core to clear the IOBUF of.
	 */
	public ClearIOBUF(HasCoreLocation core) {
		super(core, true, CLEAR_IOBUF);
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("clear IOBUF", CLEAR_IOBUF, buffer);
	}
}
