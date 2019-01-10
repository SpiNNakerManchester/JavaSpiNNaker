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

import static uk.ac.manchester.spinnaker.messages.scp.ReinjectorCommand.GET_STATUS;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** An SCP Request to get the status of the dropped packet reinjection. */
public class GetReinjectionStatus
		extends SCPRequest<GetReinjectionStatus.Response> {
	/**
	 * @param chip
	 *            the monitor core to read from
	 */
	public GetReinjectionStatus(HasCoreLocation core) {
		super(new ReinjectionSDPHeader(core), GET_STATUS, 0, 0, 0, null);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request for the dropped packet reinjection status.
	 */
	public static final class Response extends CheckOKResponse {
		/** The chip information received. */
		public final ReinjectionStatus reinjectionStatus;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Get packet reinjection status", GET_STATUS, buffer);
			this.reinjectionStatus = new ReinjectionStatus(buffer);
		}
	}
}
