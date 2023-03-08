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
package uk.ac.manchester.spinnaker.alloc.bmp;

import uk.ac.manchester.spinnaker.alloc.model.Direction;

/**
 * Describes a part of a request that modifies the power of an FPGA-managed
 * inter-board link to be off.
 *
 * @author Donal Fellows
 * @param board
 *            The database ID of the board that the FPGA is located on.
 * @param link
 *            Which link (and hence which FPGA).
 */
public record Link(int board, Direction link) {
	@Override
	public String toString() {
		return "Link(" + board + "," + link + ":OFF)";
	}
}
