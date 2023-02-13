/*
 * Copyright (c) 2018-2023 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.scp.RouterCommand.INIT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to clear the router on a chip. There is no response payload.
 * <p>
 * Calls {@code rtr_mc_init()} in {@code sark_hw.c}, via {@code rtr_cmd()} in
 * {@code scamp-cmd.c}.
 */
public class RouterClear extends SCPRequest<EmptyResponse> {
	/**
	 * @param chip
	 *            The coordinates of the chip to clear the router of
	 */
	public RouterClear(HasChipLocation chip) {
		super(chip.getScampCore(), CMD_RTR, INIT.value);
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("Router Clear", CMD_RTR, buffer);
	}
}
