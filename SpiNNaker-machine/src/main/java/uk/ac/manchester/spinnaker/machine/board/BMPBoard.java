/*
 * Copyright (c) 2022 The University of Manchester
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
 * Wrapper for a board number so that it can't get mixed up with other integers.
 *
 * @author Donal Fellows
 * @param board
 *            The board number.
 */
public record BMPBoard(@ValidBoardNumber int board) {
	/**
	 * The maximum board number. There can be only up to 24 boards per frame.
	 */
	public static final int MAX_BOARD_NUMBER = 23;

	@Override
	public String toString() {
		return "board=" + board;
	}
}
