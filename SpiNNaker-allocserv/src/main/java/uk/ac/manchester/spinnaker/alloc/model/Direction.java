/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.model;

import static uk.ac.manchester.spinnaker.messages.model.FPGA.FPGA_E_S;
import static uk.ac.manchester.spinnaker.messages.model.FPGA.FPGA_N_NE;
import static uk.ac.manchester.spinnaker.messages.model.FPGA.FPGA_SW_W;

import uk.ac.manchester.spinnaker.messages.model.FPGA;

/**
 * Represents link directions of a board.
 * <p>
 * <img src="doc-files/Directions.png" width="300"
 *		alt="SpiNNaker board neighbourhood">
 * <p>
 * Note how this is tilted over with respect to classical compass directions; to
 * <em>actually</em> go "true vertically north", you have to go first {@link #N}
 * and then {@link #NW} (or <em>vice versa</em>), taking two boards to actually
 * go straight north (by an offset of 12 chips); {@code a} is vertically north
 * of {@code c}, and {@code f} is vertically north of {@code d}.
 *
 * @see uk.ac.manchester.spinnaker.alloc.admin.DirInfo DirInfo
 * @author Donal Fellows
 */
public enum Direction {
	// Order must match that in database
	/** Northward, from {@code x} to {@code a}. */
	N(FPGA_N_NE, 0, "fpga_n", 3),
	/** Eastward, from {@code x} to {@code b}. */
	E(FPGA_N_NE, 1, "fpga_e", 4),
	/** Southeast, from {@code x} to {@code c}. */
	SE(FPGA_E_S, 0, "fpga_se", 5),
	/** Southward, from {@code x} to {@code d}. */
	S(FPGA_E_S, 1, "fpga_s", 0),
	/** Westward, from {@code x} to {@code e}. */
	W(FPGA_SW_W, 0, "fpga_w", 1),
	/** Northwest, from {@code x} to {@code f}. */
	NW(FPGA_SW_W, 1, "fpga_nw", 2);

	/**
	 * The FPGA that manages the link in this direction. Note that the names of
	 * FPGA identifiers don't exactly match up with the direction names in this
	 * class. This <em>mostly</em> doesn't matter; the only real confusion is
	 * right here in the definitions of these directions.
	 */
	public final FPGA fpga;

	/**
	 * The register bank that manages the link in this direction.
	 */
	public final int bank;

	/**
	 * The name of the column in the {@code pending_changes} table that holds
	 * information pertaining to this link.
	 */
	public final String columnName;

	/** The number of the opposite of the link. */
	private final int oppo;

	Direction(FPGA fpga, int bankSelect, String columnName, int opposite) {
		this.fpga = fpga;
		this.bank = bankSelect;
		this.columnName = columnName;
		this.oppo = opposite;
	}

	/**
	 * Gets the direction of the link in the opposite direction. Only really
	 * valid for SpiNN-5 boards.
	 *
	 * @return The opposite direction.
	 */
	public Direction opposite() {
		return values()[oppo];
	}
}
