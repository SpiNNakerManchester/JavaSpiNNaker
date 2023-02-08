/*
 * Copyright (c) 2019 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.scp.ReinjectorCommand.GET_STATUS;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to get the status of the dropped packet reinjection. The
 * response payload is the {@linkplain ReinjectionStatus reinjection status}.
 * <p>
 * Handled by {@code reinjection_get_status()} in
 * {@code extra_monitor_support.c}.
 */
public class GetReinjectionStatus
		extends ReinjectorRequest<GetReinjectionStatus.Response> {
	/**
	 * @param core
	 *            the monitor core to read from
	 */
	public GetReinjectionStatus(HasCoreLocation core) {
		super(core, GET_STATUS);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request for the dropped packet reinjection status.
	 */
	public static final class Response
			extends PayloadedResponse<ReinjectionStatus, RuntimeException> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Get packet reinjection status", GET_STATUS, buffer);
		}

		/** @return The chip information received. */
		@Override
		protected ReinjectionStatus parse(ByteBuffer buffer) {
			return new ReinjectionStatus(buffer);
		}
	}
}
