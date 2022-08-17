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
package uk.ac.manchester.spinnaker.messages.bmp;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Like a core location, but for BMPs. Note that board numbers are
 * <em>not</em> restricted in range like core numbers.
 *
 * @author Donal Fellows
 */
public class BMPLocation implements HasCoreLocation {
	private final int cabinet;

	private final int frame;

	private final int board;

	public BMPLocation(int board) {
		cabinet = 0;
		frame = 0;
		this.board = board;
	}

	public BMPLocation(int cabinet, int frame, int board) {
		this.cabinet = cabinet;
		this.frame = frame;
		this.board = board;
	}

	/**
	 * @return The cabinet number of the board. Not actually a processor
	 *         coordinate.
	 */
	@Override
	public int getX() {
		return cabinet;
	}

	/**
	 * @return The frame number of the board. Not actually a processor
	 *         coordinate.
	 */
	@Override
	public int getY() {
		return frame;
	}

	/**
	 * @return The board number of the board. Not actually a processor ID.
	 */
	@Override
	public int getP() {
		return board;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BMPLocation) {
			var bmp = (BMPLocation) other;
			return bmp.cabinet == cabinet && bmp.frame == frame
					&& bmp.board == board;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return board;
	}
}
