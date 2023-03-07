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
package uk.ac.manchester.spinnaker.machine.datalinks;

/**
 * The FPGAs that manage inter-board links.
 *
 * @author Christian-B
 * @see FpgaEnum
 */
public enum FpgaId {
	/** The FGPA link that connects to the bottom and bottom right chips. */
	BOTTOM(0),
	/** The FGPA link that connects to the left and top left chips. */
	LEFT(1),
	/** The FGPA link that connects to the top and right chips. */
	TOP_RIGHT(2);

	/** The physical ID for this link. */
	public final int id;

	/**
	 * Converts an ID into an enum.
	 */
	private static final FpgaId[] BY_ID = {
		BOTTOM, LEFT, TOP_RIGHT
	};

	FpgaId(int id) {
		this.id = id;
	}

	/**
	 * Obtain the enum from the ID.
	 *
	 * @param id
	 *            The physical ID for the FPGA link.
	 * @return The ID as an enum
	 * @throws ArrayIndexOutOfBoundsException
	 *             Thrown if the ID is outside the known range.
	 */
	public static FpgaId byId(int id) throws ArrayIndexOutOfBoundsException {
		return BY_ID[id];
	}
}
