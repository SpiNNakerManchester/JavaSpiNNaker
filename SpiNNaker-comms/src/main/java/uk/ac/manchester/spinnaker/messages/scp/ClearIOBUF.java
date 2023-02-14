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

import static uk.ac.manchester.spinnaker.messages.scp.RunningCommand.CLEAR_IOBUF;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to clear the IOBUF on a core. There is no response payload.
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
