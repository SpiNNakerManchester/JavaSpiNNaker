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

import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.scp.SCPCommand;

/**
 * A request that has a response with no payload.
 */
public abstract class SimpleRequest extends BMPRequest<SimpleRequest.Response> {
	/**
	 * Make a request.
	 *
	 * @param operation
	 *            The initial description of the high-level operation that this
	 *            is part of, for error reporting.
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 */
	SimpleRequest(String operation, BMPBoard board, SCPCommand command) {
		super(operation, board, command);
	}

	/**
	 * Make a request.
	 *
	 * @param operation
	 *            The initial description of the high-level operation that this
	 *            is part of, for error reporting.
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 */
	SimpleRequest(String operation, BMPBoard board, SCPCommand command,
			int argument1) {
		super(operation, board, command, argument1);
	}

	/**
	 * Make a request.
	 *
	 * @param operation
	 *            The initial description of the high-level operation that this
	 *            is part of, for error reporting.
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 * @param argument2
	 *            The second argument
	 */
	SimpleRequest(String operation, BMPBoard board, SCPCommand command,
			int argument1, int argument2) {
		super(operation, board, command, argument1, argument2);
	}

	/**
	 * Make a request.
	 *
	 * @param operation
	 *            The initial description of the high-level operation that this
	 *            is part of, for error reporting.
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
	SimpleRequest(String operation, BMPBoard board, SCPCommand command,
			int argument1, int argument2, int argument3) {
		super(operation, board, command, argument1, argument2, argument3);
	}

	/**
	 * Make a request.
	 *
	 * @param operation
	 *            The initial description of the high-level operation that this
	 *            is part of, for error reporting.
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
	SimpleRequest(String operation, BMPBoard board, SCPCommand command,
			int argument1, int argument2, int argument3, ByteBuffer data) {
		super(operation, board, command, argument1, argument2, argument3, data);
	}

	/**
	 * Make a request.
	 *
	 * @param operation
	 *            The initial description of the high-level operation that this
	 *            is part of, for error reporting.
	 * @param boards
	 *            The boards to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 */
	SimpleRequest(String operation, Collection<BMPBoard> boards,
			SCPCommand command, int argument1) {
		super(operation, boards, command, argument1);
	}

	/**
	 * Make a request.
	 *
	 * @param operation
	 *            The initial description of the high-level operation that this
	 *            is part of, for error reporting.
	 * @param boards
	 *            The boards to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 * @param argument2
	 *            The second argument
	 */
	SimpleRequest(String operation, Collection<BMPBoard> boards,
			SCPCommand command, int argument1, int argument2) {
		super(operation, boards, command, argument1, argument2);
	}

	@Override
	public final Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * A response to a BMP request that only checks whether the request was
	 * successful, but doesn't include a payload.
	 */
	public final class Response extends BMPRequest<Response>.BMPResponse {
		Response(ByteBuffer buffer) throws Exception {
			super(buffer);
		}
	}
}
