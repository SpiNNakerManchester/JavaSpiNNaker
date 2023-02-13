/*
 * Copyright (c) 2018-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static java.util.Objects.isNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.ValidBoardNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidCabinetNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidFrameNumber;

/**
 * The physical coordinates of a board. This would be {@link PhysicalCoords}
 * except it has a different serialization form for backward-compatibility.
 */
@JsonPropertyOrder({
	"cabinet", "frame", "board"
})
@JsonFormat(shape = ARRAY)
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
@Immutable
public class BoardPhysicalCoordinates {
	@ValidCabinetNumber
	private final int cabinet;

	@ValidFrameNumber
	private final int frame;

	@ValidBoardNumber
	private final Integer board;

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
	public BoardPhysicalCoordinates(
			@JsonProperty(value = "cabinet", defaultValue = "0") int cabinet,
			@JsonProperty(value = "frame", defaultValue = "0") int frame,
			@JsonProperty(value = "board", defaultValue = "0") Integer board) {
		this.cabinet = cabinet;
		this.frame = frame;
		this.board = board;
	}

	/**
	 * Create with given coordinates.
	 *
	 * @param coords
	 *            the coordinates in standard form.
	 */
	public BoardPhysicalCoordinates(PhysicalCoords coords) {
		this.cabinet = coords.c;
		this.frame = coords.f;
		this.board = coords.b;
	}

	/** @return the cabinet ID */
	public int getCabinet() {
		return cabinet;
	}

	/** @return the frame ID within the cabinet */
	public int getFrame() {
		return frame;
	}

	/** @return the board ID within the frame */
	public Integer getBoard() {
		return board;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BoardPhysicalCoordinates) {
			var other = (BoardPhysicalCoordinates) o;
			return cabinet == other.cabinet && frame == other.frame
					&& Objects.equals(board, other.board);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 9 * (cabinet * 1234567 + frame * 56789
				+ (isNull(board) ? 0 : board));
	}

	@Override
	public String toString() {
		return "Board{" + cabinet + "," + frame + "," + board + "}";
	}

	/**
	 * Convert to the standard coordinate scheme. Assumes that the {@code board}
	 * field is defined.
	 *
	 * @return the coordinates
	 */
	public PhysicalCoords toStandardCoords() {
		return new PhysicalCoords(cabinet, frame, board);
	}
}
