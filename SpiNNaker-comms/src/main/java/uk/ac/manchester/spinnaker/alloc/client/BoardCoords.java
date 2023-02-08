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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.machine.board.ValidBoardNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidCabinetNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidFrameNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * Generalised coordinates of a board.
 *
 * @param x
 *            Logical triad X coordinate. Range: 0&ndash;255.
 * @param y
 *            Logical triad Y coordinate. Range: 0&ndash;255.
 * @param z
 *            Logical triad Z coordinate. Range: 0&ndash;2.
 * @param cabinet
 *            Number of the cabinet containing the frame containing the board.
 *            Range: 0&ndash;31.
 * @param frame
 *            Number of the frame (within the cabinet) containing the board.
 *            Range: 0&ndash;31.
 * @param board
 *            Number of the board within its frame. Range: 0&ndash;23. May be
 *            {@code null} under some circumstances.
 * @param address
 *            IP address of ethernet chip on board. May be {@code null} if the
 *            current user doesn't have permission to see the board address at
 *            this point (because it isn't allocated or booted).
 */
@Immutable
public record BoardCoords(//
		@JsonProperty("x") @ValidTriadX int x,
		@JsonProperty("y") @ValidTriadY int y,
		@JsonProperty("z") @ValidTriadZ int z,
		@JsonProperty("cabinet") @ValidCabinetNumber int cabinet,
		@JsonProperty("frame") @ValidFrameNumber int frame,
		@JsonProperty("board") @ValidBoardNumber Integer board,
		@JsonProperty("address") @IPAddress(nullOK = true) String address) {
	/**
	 * The triad coordinate triple of the board.
	 *
	 * @return Logical triad coordinates.
	 */
	@JsonIgnore
	public TriadCoords triad() {
		return new TriadCoords(x, y, z);
	}

	/**
	 * The physical coordinate triple of the board.
	 *
	 * @return Physical board coordinates, or {@code null} if the {@link #board}
	 *         is null.
	 */
	@JsonIgnore
	public PhysicalCoords physicalCoords() {
		if (board == null) {
			return null;
		}
		return new PhysicalCoords(cabinet, frame, board);
	}

	@Override
	public String toString() {
		return format("Board(%d,%d,%d|%d:%d:%d|%s)", x, y, z, cabinet, frame,
				board, address);
	}
}
