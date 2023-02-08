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
package uk.ac.manchester.spinnaker.spalloc.messages;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import uk.ac.manchester.spinnaker.machine.board.TriadCoords;

/**
 * Request the physical location of a board.
 *
 * @see <a href=
 *      "https://spalloc-server.readthedocs.io/en/stable/protocol/#commands.get_board_position"
 *      >Spalloc Server documentation</a>
 * @see BoardPhysicalCoordinates The basic result type associated with the
 *      request
 */
public class GetBoardPositionCommand extends Command<Integer> {
	/**
	 * Create a request.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The logical coordinates of the board.
	 */
	public GetBoardPositionCommand(@NotBlank String machine,
			@Valid TriadCoords coords) {
		super("get_board_position");
		addKwArg("machine_name", machine);
		addKwArg("x", coords.x);
		addKwArg("y", coords.y);
		addKwArg("z", coords.z);
	}
}
