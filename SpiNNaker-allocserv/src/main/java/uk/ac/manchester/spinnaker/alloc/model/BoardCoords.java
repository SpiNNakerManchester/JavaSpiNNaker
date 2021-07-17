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
package uk.ac.manchester.spinnaker.alloc.model;

/**
 * Basic coordinates of a board.
 *
 * @author Donal Fellows
 */
public final class BoardCoords {
	/** Logical triad X coordinate. */
	public final int x;

	/** Logical triad Y coordinate. */
	public final int y;

	/** Logical triad Z coordinate. */
	public final int z;

	/** Physical cabinet number. */
	public final int cabinet;

	/** Physical frame number. */
	public final int frame;

	/** Physical board number. */
	public final int board;

	/** IP address of ethernet chip. */
	public final String address;

	/**
	 * @param x
	 *            Logical triad X coordinate
	 * @param y
	 *            Logical triad Y coordinate
	 * @param z
	 *            Logical triad Z coordinate
	 * @param cabinet
	 *            Physical cabinet number
	 * @param frame
	 *            Physical frame number
	 * @param board
	 *            Physical board number
	 * @param address
	 *            IP address of ethernet chip
	 */
	public BoardCoords(int x, int y, int z, int cabinet, int frame,
			int board, String address) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cabinet = cabinet;
		this.frame = frame;
		this.board = board;
		this.address = address;
	}
}
