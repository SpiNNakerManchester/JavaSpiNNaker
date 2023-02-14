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

import static uk.ac.manchester.spinnaker.messages.scp.ReinjectorCommand.CLEAR;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to set the dropped packet reinjected packet types. There is no
 * response payload.
 * <p>
 * Handled by {@code reinjection_clear()} in {@code extra_monitor_support.c}.
 */
public class ClearReinjectionQueue extends ReinjectorRequest<EmptyResponse> {
	/**
	 * @param core
	 *            The coordinates of the monitor core.
	 */
	public ClearReinjectionQueue(HasCoreLocation core) {
		super(core, CLEAR);
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("Clear reinjection queue", CLEAR, buffer);
	}
}
