/*
 * Copyright (c) 2020 The University of Manchester
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
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Request to report a problem.
 */
public class ReportProblemCommand extends Command<String> {
	/**
	 * Make a request to report a problem with a failing board.
	 *
	 * @param boardAddress
	 *            The IP address of the board.
	 */
	public ReportProblemCommand(String boardAddress) {
		super("report_problem");
		addArg(boardAddress);
	}

	/**
	 * Make a request to report a problem with a failing chip.
	 *
	 * @param boardAddress
	 *            The IP address of the board with the chip.
	 * @param problemChip
	 *            The address of the chip with the problem on the board.
	 *            Board-relative.
	 */
	public ReportProblemCommand(String boardAddress,
			HasChipLocation problemChip) {
		super("report_problem");
		addArg(boardAddress);
		addKwArg("x", problemChip.getX());
		addKwArg("y", problemChip.getY());
	}

	/**
	 * Make a request to report a problem with a failing core.
	 *
	 * @param boardAddress
	 *            The IP address of the board with the core.
	 * @param problemCore
	 *            The address of the core with the problem on the board.
	 *            Board-relative.
	 */
	public ReportProblemCommand(String boardAddress,
			HasCoreLocation problemCore) {
		super("report_problem");
		addArg(boardAddress);
		addKwArg("x", problemCore.getX());
		addKwArg("y", problemCore.getY());
		addKwArg("p", problemCore.getP());
	}
}
