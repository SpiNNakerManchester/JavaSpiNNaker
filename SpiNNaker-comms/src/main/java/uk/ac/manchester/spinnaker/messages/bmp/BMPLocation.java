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

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Like a {@linkplain CoreLocation core location}, but for BMPs. Note that board
 * numbers are <em>not</em> restricted in range like core numbers.
 *
 * @author Donal Fellows
 */
@UsedInJavadocOnly(CoreLocation.class)
public final class BMPLocation implements HasCoreLocation {
	private final int cabinet;

	private final int frame;

	private final int board;

	/**
	 * Create an instance with cabinet and frame both zero.
	 *
	 * @param board
	 *            The board.
	 */
	public BMPLocation(BMPBoard board) {
		cabinet = 0;
		frame = 0;
		this.board = board.board;
	}

	/**
	 * Create an instance with cabinet and frame both zero.
	 *
	 * @param board
	 *            The board number.
	 */
	public BMPLocation(int board) {
		this(0, 0, board);
	}

	/**
	 * Create an instance.
	 *
	 * @param bmp
	 *            The managing BMP.
	 * @param board
	 *            The board.
	 */
	public BMPLocation(BMPCoords bmp, BMPBoard board) {
		this.cabinet = bmp.getCabinet();
		this.frame = bmp.getFrame();
		this.board = board.board;
	}

	/**
	 * Create an instance.
	 *
	 * @param cabinet
	 *            The cabinet number.
	 * @param frame
	 *            The frame number.
	 * @param board
	 *            The board number.
	 */
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

	/**
	 * @return The coordinates of the BMP that manages the board.
	 */
	public BMPCoords getBMPCoords() {
		return new BMPCoords(cabinet, frame);
	}

	/**
	 * @return The board number of the board.
	 */
	public BMPBoard getBMPBoard() {
		return new BMPBoard(board);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BMPLocation) {
			BMPLocation bmp = (BMPLocation) other;
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
