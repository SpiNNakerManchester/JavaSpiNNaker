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
package uk.ac.manchester.spinnaker.allocator;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Physical coordinates of a board. SpiNNaker boards are arranged in frames
 * (multi-unit racks that share a management layer) and frames are arranged in
 * cabinets (full 19" server cabinets).
 */
@JsonFormat(shape = ARRAY)
public class Physical {
	private int cabinet;

	private int frame;

	private Integer board;

	public Physical() {
	}

	/**
	 * @param cabinet
	 *            The cabinet number.
	 * @param frame
	 *            The frame number.
	 * @param board
	 *            The board number.
	 */
	public Physical(int cabinet, int frame, int board) {
		this.cabinet = cabinet;
		this.frame = frame;
		this.board = board;
	}

	/** @return The cabinet number. */
	public int getCabinet() {
		return cabinet;
	}

	public void setCabinet(int cabinet) {
		this.cabinet = cabinet;
	}

	/** @return The frame number. */
	public int getFrame() {
		return frame;
	}

	public void setFrame(int frame) {
		this.frame = frame;
	}

	/** @return The board number. */
	public Integer getBoard() {
		// TODO document when this can be null
		return board;
	}

	public void setBoard(Integer board) {
		this.board = board;
	}

	@Override
	public String toString() {
		return format("[%d:%d:%s]", cabinet, frame, board);
	}
}
