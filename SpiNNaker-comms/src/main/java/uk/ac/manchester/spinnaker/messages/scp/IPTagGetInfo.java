/*
 * Copyright (c) 2018 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.TTO;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** An SCP Request information about IP tags. */
public class IPTagGetInfo extends SCPRequest<IPTagGetInfo.Response> {
	private static final int IPTAG_MAX = 255;

	/**
	 * @param chip
	 *            The chip to query for information.
	 */
	public IPTagGetInfo(HasChipLocation chip) {
		super(chip.getScampCore(), CMD_IPTAG, TTO.value << COMMAND_FIELD,
				IPTAG_MAX);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new IPTagGetInfo.Response(buffer);
	}

	/** An SCP response to a request for information about IP tags. */
	public static class Response extends CheckOKResponse {
		/**
		 * The timeout for transient IP tags (i.e., responses to SCP commands).
		 */
		public final IPTagTimeOutWaitTime transientTimeout;
		/** The count of the IP tag pool size. */
		public final int poolSize;
		/** The count of the number of fixed IP tag entries. */
		public final int fixedSize;

		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Get IP Tag Info", CMD_IPTAG, buffer);
			transientTimeout = IPTagTimeOutWaitTime.get(buffer.get());
			buffer.get(); // skip 1
			poolSize = Byte.toUnsignedInt(buffer.get());
			fixedSize = Byte.toUnsignedInt(buffer.get());
		}
	}
}
