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
package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request the logical location of a board.
 *
 * @see <a href=
 *      "https://spalloc-server.readthedocs.io/en/stable/protocol/#commands.get_board_at_position"
 *      >Spalloc Server documentation</a>
 * @see BoardCoordinates The basic result type associated with the request
 */
public class GetBoardAtPositionCommand extends Command<Integer> {
	/**
	 * Create a request.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The logical coordinates of the board.
	 */
	public GetBoardAtPositionCommand(String machine,
			BoardPhysicalCoordinates coords) {
		super("get_board_at_position");
		addKwArg("machine_name", machine);
                // The current spalloc server expects the param names x, y, z
		addKwArg("x", coords.getCabinet());
		addKwArg("y", coords.getFrame());
		addKwArg("z", coords.getBoard());
	}
}
