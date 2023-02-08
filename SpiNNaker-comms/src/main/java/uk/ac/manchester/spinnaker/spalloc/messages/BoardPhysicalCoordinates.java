/*
 * Copyright (c) 2018 The University of Manchester
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
 *
 * @param cabinet
 *            the cabinet ID
 * @param frame
 *            the frame ID within the cabinet
 * @param board
 *            the board ID within the frame
 */
@JsonPropertyOrder({ "cabinet", "frame", "board" })
@JsonFormat(shape = ARRAY)
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
@Immutable
public record BoardPhysicalCoordinates(
		@JsonProperty(value = "cabinet", defaultValue = "0") //
		@ValidCabinetNumber int cabinet,
		@JsonProperty(value = "frame", defaultValue = "0") //
		@ValidFrameNumber int frame,
		@JsonProperty(value = "board", defaultValue = "0") //
		@ValidBoardNumber Integer board) {
	/**
	 * Create with given coordinates.
	 *
	 * @param coords
	 *            the coordinates in standard form.
	 */
	public BoardPhysicalCoordinates(PhysicalCoords coords) {
		this(coords.c(), coords.f(), coords.b());
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
