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

import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.DEFAULT_PORT;

import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.scp.SCPCommand;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/**
 * The base class of a request following the BMP protocol.
 *
 * @author Donal Fellows
 * @param <T>
 *            The type of the response to the request.
 */
public abstract class BMPRequest<T extends BMPRequest.BMPResponse>
		extends SCPRequest<T> {
	private static SDPHeader bmpHeader(int board) {
		return new SDPHeader(REPLY_EXPECTED, new BMPLocation(board),
				DEFAULT_PORT);
	}

	private static SDPHeader bmpHeader(BMPBoard board) {
		return bmpHeader(board.board);
	}

	private static SDPHeader bmpHeader(Collection<BMPBoard> boards) {
		return bmpHeader(
				boards.stream().mapToInt(b -> b.board).min().orElse(0));
	}

	/**
	 * Make a request.
	 *
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 */
	BMPRequest(BMPBoard board, SCPCommand command) {
		super(bmpHeader(board), command, 0, 0, 0, NO_DATA);
	}

	/**
	 * Make a request.
	 *
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 */
	BMPRequest(BMPBoard board, SCPCommand command, int argument1) {
		super(bmpHeader(board), command, argument1, 0, 0, NO_DATA);
	}

	/**
	 * Make a request.
	 *
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 * @param argument2
	 *            The second argument
	 */
	BMPRequest(BMPBoard board, SCPCommand command, int argument1,
			int argument2) {
		super(bmpHeader(board), command, argument1, argument2, 0, NO_DATA);
	}

	/**
	 * Make a request.
	 *
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 * @param argument2
	 *            The second argument
	 * @param argument3
	 *            The third argument
	 */
	BMPRequest(BMPBoard board, SCPCommand command, int argument1, int argument2,
			int argument3) {
		super(bmpHeader(board), command, argument1, argument2, argument3,
				NO_DATA);
	}

	/**
	 * Make a request.
	 *
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 * @param argument2
	 *            The second argument
	 * @param argument3
	 *            The third argument
	 * @param data
	 *            The payload
	 */
	BMPRequest(BMPBoard board, SCPCommand command, int argument1, int argument2,
			int argument3, ByteBuffer data) {
		super(bmpHeader(board), command, argument1, argument2, argument3, data);
	}

	/**
	 * Make a request.
	 *
	 * @param boards
	 *            The boards to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 */
	BMPRequest(Collection<BMPBoard> boards, SCPCommand command, int argument1) {
		super(bmpHeader(boards), command, argument1, 0, 0, NO_DATA);
	}

	/**
	 * Make a request.
	 *
	 * @param boards
	 *            The boards to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 * @param argument2
	 *            The second argument
	 */
	BMPRequest(Collection<BMPBoard> boards, SCPCommand command, int argument1,
			int argument2) {
		super(bmpHeader(boards), command, argument1, argument2, 0, NO_DATA);
	}

	/**
	 * Represents an SCP request thats tailored for the BMP connection. This
	 * basic class handles checking that the result is OK; subclasses manage
	 * deserializing any returned payload.
	 */
	public static class BMPResponse extends SCPResponse {
		/**
		 * Make a response object.
		 *
		 * @param operation
		 *            The operation that this part of.
		 * @param command
		 *            The command that this is a response to.
		 * @param buffer
		 *            The buffer to read the response from.
		 * @throws UnexpectedResponseCodeException
		 *             If the response is not a success.
		 */
		public BMPResponse(String operation, SCPCommand command,
				ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super(buffer);
			throwIfNotOK(operation, command);
		}
	}
}
