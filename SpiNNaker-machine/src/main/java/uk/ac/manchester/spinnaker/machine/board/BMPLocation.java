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
package uk.ac.manchester.spinnaker.machine.board;

import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Like a {@linkplain CoreLocation core location}, but for BMPs.
 *
 * @param cabinet
 *            The cabinet number.
 * @param frame
 *            The frame number.
 * @param board
 *            The board number.
 * @author Donal Fellows
 */
@Immutable
@UsedInJavadocOnly(CoreLocation.class)
public record BMPLocation(@ValidCabinetNumber int cabinet,
		@ValidFrameNumber int frame, @ValidBoardNumber int board)
		implements HasBMPLocation {
	/**
	 * Create an instance with cabinet and frame both zero.
	 *
	 * @param board
	 *            The board number.
	 */
	public BMPLocation(int board) {
		this(0, 0, board);
	}

	@Override
	public int getCabinet() {
		return cabinet;
	}

	@Override
	public int getFrame() {
		return frame;
	}

	@Override
	public int getBoard() {
		return board;
	}

	@Override
	public BMPLocation asBMPLocation() {
		return this;
	}
}
