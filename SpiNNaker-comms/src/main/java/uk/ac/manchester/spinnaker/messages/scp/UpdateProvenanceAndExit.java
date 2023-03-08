/*
 * Copyright (c) 2019 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.RunningCommand.UPDATE_PROVENCE_REGION_AND_EXIT;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to update the runtime info on a core. Note that <em>this
 * request does not expect a response</em>; the response to this request is
 * detected by the core entering a non-running state.
 * <p>
 * This calls {@code simulation_control_scp_callback()} in {@code simulation.c}.
 */
public class UpdateProvenanceAndExit extends FECRequest<NoResponse> {
	/**
	 * @param core
	 *            The SpiNNaker core to update the provenance info of.
	 */
	public UpdateProvenanceAndExit(HasCoreLocation core) {
		super(core, false, UPDATE_PROVENCE_REGION_AND_EXIT);
	}

	@Override
	public NoResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new NoResponse("update provenance and exit",
				UPDATE_PROVENCE_REGION_AND_EXIT, buffer);
	}
}
