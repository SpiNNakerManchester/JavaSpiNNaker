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

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_VER;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;

/**
 * A request to read the version of software running on a core. The
 * response payload is the {@linkplain VersionInfo version descriptor}.
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
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new Response(buffer);
	}

	/** An SCP response to a request for the version of software running. */
	// Used in tests
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
