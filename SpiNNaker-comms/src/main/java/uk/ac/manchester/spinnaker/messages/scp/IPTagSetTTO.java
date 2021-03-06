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

/** An SCP request to set the transient timeout for future SCP requests. */
public class IPTagSetTTO extends SCPRequest<IPTagGetInfo.Response> {
	/**
	 * @param chip
	 *            The chip to set the tag timout on.
	 * @param tagTimeout
	 *            The timeout to set.
	 */
	public IPTagSetTTO(HasChipLocation chip, IPTagTimeOutWaitTime tagTimeout) {
		super(chip.getScampCore(), CMD_IPTAG, TTO.value << COMMAND_FIELD,
				tagTimeout.value);
	}

	@Override
	public IPTagGetInfo.Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new IPTagGetInfo.Response(buffer);
	}
}
