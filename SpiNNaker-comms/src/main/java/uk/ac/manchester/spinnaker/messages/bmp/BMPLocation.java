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
package uk.ac.manchester.spinnaker.messages.bmp;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.board.ValidBoardNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidCabinetNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidFrameNumber;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Like a {@linkplain CoreLocation core location}, but for BMPs. Note that board
 * numbers are <em>not</em> restricted in range like core numbers.
 *
 * @author Donal Fellows
 */
@UsedInJavadocOnly(CoreLocation.class)
public final class BMPLocation implements HasCoreLocation {
	@ValidCabinetNumber
	private final int cabinet;

	@ValidFrameNumber
	private final int frame;

	@ValidBoardNumber
	private final int board;

	/**
	 * Create an instance with cabinet and frame both zero.
	 *
	 * @param board
	 *            The board number.
	 */
	public BMPLocation(int board) {
		this(0, 0, board);
	}

	/**
	 * Create an instance.
	 *
	 * @param cabinet
	 *            The cabinet number.
	 * @param frame
	 *            The frame number.
	 * @param board
	 *            The board number.
	 */
	public BMPLocation(int cabinet, int frame, int board) {
		this.cabinet = cabinet;
		this.frame = frame;
		this.board = board;
	}

	/**
	 * @return The cabinet number of the board. Not actually a processor
	 *         coordinate.
	 */
	@Override
	public int getX() {
		return cabinet;
	}

	/**
	 * @return The frame number of the board. Not actually a processor
	 *         coordinate.
	 */
	@Override
	public int getY() {
		return frame;
	}

	/**
	 * @return The board number of the board. Not actually a processor ID.
	 */
	@Override
	public int getP() {
		return board;
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof BMPLocation bmp) && (bmp.cabinet == cabinet)
				&& (bmp.frame == frame) && (bmp.board == board);
	}

	@Override
	public int hashCode() {
		return board;
	}
}
