/*
 * Copyright (c) 2018 The University of Manchester
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

/**
 * The interface supported by any object that is associated with a board
 * location.
 */
public interface HasBMPLocation {
	/**
	 * @return The cabinet number of the board. Not actually a processor
	 *         coordinate.
	 */
	@ValidCabinetNumber
	public int getCabinet();

	/**
	 * @return The frame number of the board. Not actually a processor
	 *         coordinate.
	 */
	@ValidFrameNumber
	public int getFrame();

	/**
	 * @return The board number of the board. Not actually a processor ID.
	 */
	@ValidBoardNumber
	public int getBoard();

	/**
	 * Converts (if required) this to a simple X, Y, P tuple.
	 *
	 * @return A CoreLocation representation of the X, Y, P tuple
	 */
	default BMPLocation asBMPLocation() {
		return new BMPLocation(getCabinet(), getFrame(), getBoard());
	}
}
