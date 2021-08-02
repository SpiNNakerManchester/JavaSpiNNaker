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

import java.sql.SQLException;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;

/**
 * Basic coordinates of a board.
 *
 * @author Donal Fellows
 */
public final class BoardCoords {
	/** Logical triad X coordinate. */
	private final int x;

	/** Logical triad Y coordinate. */
	private final int y;

	/** Logical triad Z coordinate. */
	private final int z;

	/** Physical cabinet number. */
	private final int cabinet;

	/** Physical frame number. */
	private final int frame;

	/**
	 * Physical board number. May be {@code null} if the board is dead (e.g.,
	 * because it is outright absent from the machine).
	 */
	private final Integer board;

	/**
	 * IP address of ethernet chip. May be {@code null} if the current user
	 * doesn't have permission to see the board address at this point, or the
	 * board is dead (e.g., because it is outright absent from the machine).
	 */
	private final String address;

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
			Integer board, String address) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cabinet = cabinet;
		this.frame = frame;
		this.board = board;
		this.address = address;
	}

	/**
	 * Construct a set of board coordinates from a database row that describes
	 * them.
	 *
	 * @param row
	 *            Database row
	 * @param shroudAddress
	 *            Whether the {@link #address} should be shrouded.
	 * @throws SQLException
	 *             If the row lacks the entry needed or this is otherwise
	 *             misused.
	 */
	public BoardCoords(Row row, boolean shroudAddress) throws SQLException {
		x = row.getInt("x");
		y = row.getInt("y");
		z = row.getInt("z");
		cabinet = row.getInt("cabinet");
		frame = row.getInt("frame");
		board = row.getInteger("board_num");
		address = shroudAddress ? null : row.getString("address");
	}

	/**
	 * @return Logical triad X coordinate.
	 */
	public int getX() {
		return x;
	}

	/**
	 * @return Logical triad Y coordinate.
	 */
	public int getY() {
		return y;
	}

	/**
	 * @return Logical triad Z coordinate.
	 */
	public int getZ() {
		return z;
	}

	/**
	 * @return Physical cabinet number.
	 */
	public int getCabinet() {
		return cabinet;
	}

	/**
	 * @return Physical frame number.
	 */
	public int getFrame() {
		return frame;
	}

	/**
	 * @return Physical board number. May be {@code null} if the board is dead
	 *         (e.g., because it is outright absent from the machine).
	 */
	public Integer getBoard() {
		return board;
	}

	/**
	 * @return IP address of ethernet chip. May be {@code null} if the current
	 *         user doesn't have permission to see the board address at this
	 *         point, or the board is dead (e.g., because it is outright absent
	 *         from the machine).
	 */
	public String getAddress() {
		return address;
	}
}
