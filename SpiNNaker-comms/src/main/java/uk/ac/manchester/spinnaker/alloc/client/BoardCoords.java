/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.client;

import static java.lang.String.format;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.board.ValidBoardNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidCabinetNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidFrameNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/** Generalised coordinates of a board. */
@Immutable
public class BoardCoords {
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

	/** Physical board number. */
	@ValidBoardNumber
	private final Integer board;

	/**
	 * IP address of ethernet chip. May be {@code null} if the current user
	 * doesn't have permission to see the board address at this point.
	 */
	@IPAddress(nullOK = true)
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
	BoardCoords(@JsonProperty("x") int x, @JsonProperty("y") int y,
			@JsonProperty("z") int z, @JsonProperty("cabinet") int cabinet,
			@JsonProperty("frame") int frame,
			@JsonProperty("board") Integer board,
			@JsonProperty("address") String address) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cabinet = cabinet;
		this.frame = frame;
		this.board = board;
		this.address = address;
	}

	/**
	 * Get the triad X coordinate. Range: 0-255.
	 *
	 * @return Logical triad X coordinate.
	 */
	public int getX() {
		return x;
	}

	/**
	 * Get the triad Y coordinate. Range: 0-255.
	 *
	 * @return Logical triad Y coordinate.
	 */
	public int getY() {
		return y;
	}

	/**
	 * Get the triad Z coordinate. Range: 0-2.
	 *
	 * @return Logical triad Z coordinate.
	 */
	public int getZ() {
		return z;
	}

	/**
	 * Get the number of the cabinet containing the frame containing the board.
	 *
	 * @return Physical cabinet number.
	 */
	public int getCabinet() {
		return cabinet;
	}

	/**
	 * Get the number of the frame (within the cabinet) containing the board.
	 *
	 * @return Physical frame number.
	 */
	public int getFrame() {
		return frame;
	}

	/**
	 * Get the number of the board within its frame.
	 *
	 * @return Physical board number.
	 */
	public int getBoard() {
		return board;
	}

	/**
	 * Get the IP address of the Ethernet chip of the board, if available.
	 *
	 * @return IP address of ethernet chip. May be {@code null} if the
	 *         current user doesn't have permission to see the board address
	 *         at this point.
	 */
	public String getAddress() {
		return address;
	}

	@Override
	public String toString() {
		return format("Board(%d,%d,%d|%d:%d:%d|%s)", x, y, z,
				cabinet, frame, board, address);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof BoardCoords)) {
			return false;
		}
		var o = (BoardCoords) other;
		return x == o.x && y == o.y && z == o.z && cabinet == o.cabinet
				&& frame == o.frame && Objects.equals(board, o.board)
				&& Objects.equals(address, o.address);
	}

	@Override
	public int hashCode() {
		return (x << 16) | (y << 8) | z;
	}
}
