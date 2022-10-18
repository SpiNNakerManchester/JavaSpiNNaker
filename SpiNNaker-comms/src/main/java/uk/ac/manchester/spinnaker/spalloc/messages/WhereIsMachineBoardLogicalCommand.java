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

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import uk.ac.manchester.spinnaker.machine.board.TriadCoords;

/**
 * Request to get the location of a board in a machine by logical location.
 *
 * @see <a href=
 *      "https://spalloc-server.readthedocs.io/en/stable/protocol/#commands.where_is"
 *      >Spalloc Server documentation</a>
 * @see WhereIs The basic result type associated with the request
 */
public class WhereIsMachineBoardLogicalCommand extends Command<Integer> {
	/**
	 * Create a request to locate a board on a machine.
	 *
	 * @param machine
	 *            The machine to request about.
	 * @param coords
	 *            The logical coordinates of the board to ask about.
	 */
	public WhereIsMachineBoardLogicalCommand(@NotBlank String machine,
			@Valid TriadCoords coords) {
		super("where_is");
		addKwArg("machine", machine);
		addKwArg("x", coords.x);
		addKwArg("y", coords.y);
		addKwArg("z", coords.z);
	}
}
