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
package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_VER;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;

/**
 * A request to read the version of software running on a board's BMP. The
 * response payload is a {@linkplain VersionInfo version descriptor}.
 * <p>
 * Calls or {@code cmd_ver()} in {@code bmp_cmd.c}.
 */
public class GetBMPVersion extends BMPRequest<GetBMPVersion.Response> {
	/**
	 * @param board
	 *            The board to get the version from
	 */
	public GetBMPVersion(BMPBoard board) {
		super(board, CMD_VER);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for the version of software running. */
	protected final class Response
			extends BMPRequest.PayloadedResponse<VersionInfo> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read Version", CMD_VER, buffer);
		}

		/** @return The version information received. */
		@Override
		protected VersionInfo parse(ByteBuffer buffer) {
			return new VersionInfo(buffer, true);
		}
	}
}
