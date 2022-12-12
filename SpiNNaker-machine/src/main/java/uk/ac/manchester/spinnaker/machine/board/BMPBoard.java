/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.board;

/**
 * Wrapper for a board number so that it can't get mixed up with other integers.
 *
 * @author Donal Fellows
 * @param board
 *            The board number.
 */
public record BMPBoard(@ValidBoardNumber int board) {
	/**
	 * The maximum board number. There can be only up to 24 boards per frame.
	 */
	public static final int MAX_BOARD_NUMBER = 23;

	@Override
	public String toString() {
		return "board=" + board;
	}
}
