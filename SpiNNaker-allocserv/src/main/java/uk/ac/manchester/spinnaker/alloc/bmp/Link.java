/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import uk.ac.manchester.spinnaker.alloc.model.Direction;

/**
 * Describes a part of a request that modifies the power of an FPGA-managed
 * inter-board link to be off.
 *
 * @author Donal Fellows
 */
final class Link {
	/** The physical ID of the board that the FPGA is located on. */
	final int board;

	/** Which link (and hence which FPGA). */
	final Direction link;

	/**
	 * Create a request.
	 *
	 * @param board
	 *            The physical ID of the board that the FPGA is located on.
	 * @param link
	 *            Which link (and hence which FPGA).
	 */
	Link(int board, Direction link) {
		this.board = board;
		this.link = link;
	}

	@Override
	public String toString() {
		return "Link(" + board + "," + link + ":OFF)";
	}
}
