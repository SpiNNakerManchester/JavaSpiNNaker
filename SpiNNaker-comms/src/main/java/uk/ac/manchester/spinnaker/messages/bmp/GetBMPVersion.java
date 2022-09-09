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
package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_VER;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;

/**
 * An SCP request to read the version of software running on a board's BMP.
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
	public final class Response extends BMPRequest.BMPResponse {
		/** The version information received. */
		public final VersionInfo versionInfo;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read Version", CMD_VER, buffer);
			versionInfo = new VersionInfo(buffer, true);
		}
	}
}
