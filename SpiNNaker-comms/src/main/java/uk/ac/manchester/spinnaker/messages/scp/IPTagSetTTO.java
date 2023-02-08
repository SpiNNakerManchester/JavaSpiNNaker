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

import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.TTO;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP request to set the transient timeout for future SCP requests. The
 * response payload is the {@linkplain IPTagGetInfo.TagInfo tag <em>system</em>
 * information}.
 * <p>
 * Handled by {@code cmd_iptag()} in {@code scamp-cmd.c} (or {@code bmp_cmd.c},
 * if sent to a BMP).
 */
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
