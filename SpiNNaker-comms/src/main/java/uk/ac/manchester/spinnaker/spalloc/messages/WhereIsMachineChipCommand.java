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

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * Request to get the location of a chip in a machine.
 *
 * @see <a href=
 *      "https://spalloc-server.readthedocs.io/en/stable/protocol/#commands.where_is"
 *      >Spalloc Server documentation</a>
 * @see WhereIs The basic result type associated with the request
 */
public class WhereIsMachineChipCommand extends Command<Integer> {
	/**
	 * Create a request to locate a chip on a machine.
	 *
	 * @param machine
	 *            The machine to request about.
	 * @param chip
	 *            The coordinates of the chip to ask about.
	 */
	public WhereIsMachineChipCommand(String machine, HasChipLocation chip) {
		super("where_is");
		addKwArg("machine", machine);
		addKwArg("chip_x", chip.getX());
		addKwArg("chip_y", chip.getY());
	}
}
