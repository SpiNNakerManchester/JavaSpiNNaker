/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.model;

import static java.lang.String.format;

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
 * @param x
 *            Logical triad X coordinate.
 * @param y
 *            Logical triad Y coordinate.
 * @param z
 *            Logical triad Z coordinate.
 * @param cabinet
 *            Physical cabinet number.
 * @param frame
 *            Physical frame number.
 * @param board
 *            Physical board number. May be {@code null} if the board is dead
 *            (e.g., because it is outright absent from the machine).
 * @param address
 *            IP address of ethernet chip. May be {@code null} if the current
 *            user doesn't have permission to see the board address at this
 *            point, or the board is dead (e.g., because it is outright absent
 *            from the machine).
 */
@Immutable
public record BoardCoords(@ValidTriadX int x, @ValidTriadY int y,
		@ValidTriadZ int z, @ValidCabinetNumber int cabinet,
		@ValidFrameNumber int frame, @ValidBoardNumber Integer board,
		@IPAddress(nullOK = true) String address) {
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
		this(row.getInt("x"), row.getInt("y"), row.getInt("z"),
				row.getInt("cabinet"), row.getInt("frame"),
				row.getInteger("board_num"),
				shroudAddress ? null : row.getString("address"));
	}

	@Override
	public String toString() {
		return format("xyz:(%d,%d,%d);cfb:(%d,%d,%s);ip:%s", x, y, z, cabinet,
				frame, board, address);
	}
}
