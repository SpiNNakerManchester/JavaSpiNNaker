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

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_VER;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;

/**
 * An SCP request to read the version of software running on a core.
 * <p>
 * Calls {@code cmd_ver()} in {@code scamp-cmd.c} or {@code sark_cmd_ver()} in
 * {@code sark_base.c}, depending on which core the message is sent to.
 */
public class GetVersion extends SCPRequest<GetVersion.Response> {
	/**
	 * @param core
	 *            The location of the core to read from.
	 */
	public GetVersion(HasCoreLocation core) {
		super(core, CMD_VER);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for the version of software running. */
	public static final class Response
			extends PayloadedResponse<VersionInfo, RuntimeException> {
		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Version", CMD_VER, buffer);
		}

		/** @return The version information received. */
		@Override
		protected VersionInfo parse(ByteBuffer buffer) {
			return new VersionInfo(buffer, false);
		}
	}
}
