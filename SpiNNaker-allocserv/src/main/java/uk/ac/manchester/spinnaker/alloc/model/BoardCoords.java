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

import static java.lang.String.format;

import java.util.Objects;

import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.machine.board.ValidBoardNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidCabinetNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidFrameNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * Basic coordinates of a board. The result of a DB query.
 *
 * @author Donal Fellows
 */
@Immutable
public final class BoardCoords {
	/** Logical triad X coordinate. */
	@ValidTriadX
	private final int x;

	/** Logical triad Y coordinate. */
	@ValidTriadY
	private final int y;

	/** Logical triad Z coordinate. */
	@ValidTriadZ
	private final int z;

	/** Physical cabinet number. */
	@ValidCabinetNumber
	private final int cabinet;

	/** Physical frame number. */
	@ValidFrameNumber
	private final int frame;

	/**
	 * Physical board number. May be {@code null} if the board is dead (e.g.,
	 * because it is outright absent from the machine).
	 */
	@ValidBoardNumber
	private final Integer board;

	/**
	 * IP address of ethernet chip. May be {@code null} if the current user
	 * doesn't have permission to see the board address at this point, or the
	 * board is dead (e.g., because it is outright absent from the machine).
	 */
	@IPAddress(nullOK = true)
	private final String address;

	/**
	 * Make an instance from raw results. Uncommon.
	 *
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
	 *            Physical board number, or {@code null}
	 * @param address
	 *            IP address of ethernet chip, or {@code null}
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
	 * them. The common constructor.
	 *
	 * @param row
	 *            Database row
	 * @param shroudAddress
	 *            Whether the {@link #address} should be shrouded.
	 */
	public BoardCoords(Row row, boolean shroudAddress) {
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

	@Override
	public boolean equals(Object o) {
		return (o instanceof BoardCoords other) && (x == other.x)
				&& (y == other.y) && (z == other.z)
				&& (cabinet == other.cabinet) && (frame == other.frame)
				&& Objects.equals(board, other.board)
				&& Objects.equals(address, other.address);
	}

	@Override
	public int hashCode() {
		// Just uses the triad coordinates
		return x << 16 | y << 8 | z;
	}

	@Override
	public String toString() {
		return format("xyz:(%d,%d,%d);cfb:(%d,%d,%s);ip:%s", x, y, z, cabinet,
				frame, board, address);
	}
}
