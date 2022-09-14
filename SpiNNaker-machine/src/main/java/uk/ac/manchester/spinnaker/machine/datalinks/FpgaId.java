/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
