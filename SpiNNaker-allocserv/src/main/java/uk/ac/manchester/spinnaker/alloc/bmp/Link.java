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
package uk.ac.manchester.spinnaker.alloc.bmp;

import uk.ac.manchester.spinnaker.alloc.model.Direction;

/**
 * Describes a part of a request that modifies the power of an FPGA-managed
 * inter-board link to be off.
 *
 * @author Donal Fellows
 */
public final class Link {
	/** The database ID of the board that the FPGA is located on. */
	private final int board;

	/** Which link (and hence which FPGA). */
	private final Direction link;

	/**
	 * Create a request.
	 *
	 * @param board
	 *            The DB ID of the board that the FPGA is located on.
	 * @param link
	 *            Which link (and hence which FPGA).
	 */
	Link(int board, Direction link) {
		this.board = board;
		this.link = link;
	}

	@Override
	public String toString() {
		return "Link(" + board + "," + link + ":OFF)";
	}

	/** @return The database ID of the board that the FPGA is located on. */
	public int getBoard() {
		return board;
	}

	/** @return Which link (and hence which FPGA). */
	public Direction getLink() {
		return link;
	}
}
