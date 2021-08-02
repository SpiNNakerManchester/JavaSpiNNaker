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

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * The physical coordinates of a board.
 */
@JsonPropertyOrder({
	"cabinet", "frame", "board"
})
@JsonFormat(shape = ARRAY)
public class BoardPhysicalCoordinates {
	private int cabinet;

	private int frame;

	private Integer board;

	/**
	 * Create with default coordinates.
	 */
	public BoardPhysicalCoordinates() {
	}

	/**
	 * Create with given coordinates.
	 *
	 * @param cabinet
	 *            the cabinet ID
	 * @param frame
	 *            the frame ID within the cabinet
	 * @param board
	 *            the board ID within the frame
	 */
	public BoardPhysicalCoordinates(int cabinet, int frame, Integer board) {
		this.cabinet = cabinet;
		this.frame = frame;
		this.board = board;
	}

	public int getCabinet() {
		return cabinet;
	}

	public void setCabinet(int cabinet) {
		this.cabinet = cabinet;
	}

	public int getFrame() {
		return frame;
	}

	public void setFrame(int frame) {
		this.frame = frame;
	}

	public Integer getBoard() {
		return board;
	}

	public void setBoard(Integer board) {
		this.board = board;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BoardPhysicalCoordinates) {
			BoardPhysicalCoordinates other = (BoardPhysicalCoordinates) o;
			return cabinet == other.cabinet && frame == other.frame
					&& board == other.board;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 9 * (cabinet * 1234567 + frame * 56789 + board);
	}

	@Override
	public String toString() {
		return "Board{" + cabinet + "," + frame + "," + board + "}";
	}
}
