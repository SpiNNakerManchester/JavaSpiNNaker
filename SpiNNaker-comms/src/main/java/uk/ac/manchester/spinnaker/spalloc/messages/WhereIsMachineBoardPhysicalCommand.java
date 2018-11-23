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
 * Request to get the location of a board in a machine by physical location.
 */
public class WhereIsMachineBoardPhysicalCommand extends Command<Integer> {
	/**
	 * Create a request to locate a board on a machine.
	 *
	 * @param machine
	 *            The machine to request about.
	 * @param cabinet
	 *            The cabinet containing the board to ask about.
	 * @param frame
	 *            The frame containing the board to ask about.
	 * @param board
	 *            The board (within the cabinet and frame) to ask about.
	 */
	public WhereIsMachineBoardPhysicalCommand(String machine, int cabinet,
			int frame, int board) {
		super("where_is");
		addKwArg("machine", machine);
		addKwArg("cabinet", cabinet);
		addKwArg("frame", frame);
		addKwArg("board", board);
	}

	/**
	 * Create a request to locate a board on a machine.
	 *
	 * @param machine
	 *            The machine to request about.
	 * @param coords
	 *            The physical coordinates of the board to ask about.
	 */
	public WhereIsMachineBoardPhysicalCommand(String machine,
			BoardPhysicalCoordinates coords) {
		super("where_is");
		addKwArg("machine", machine);
		addKwArg("cabinet", coords.getCabinet());
		addKwArg("frame", coords.getFrame());
		addKwArg("board", coords.getBoard());
	}
}
