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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static uk.ac.manchester.spinnaker.messages.model.FPGALinkRegisters.BANK_OFFSET_MULTIPLIER;
import static uk.ac.manchester.spinnaker.messages.model.FPGALinkRegisters.STOP;

/**
 * Represents link directions of a board.
 *
 * <pre>
 *        __
 *       /  |
 *    __/   |__
 *   /  | a /  |
 *  /   |__/   |
 *  | f /  | b /
 *  |__/   |__/
 *  /  | x /  |
 * /   |__/   |
 * | e /  | c /
 * |__/   |__/
 *    | d /
 *    |__/
 * </pre>
 *
 * Note that this is tilted over with respect to reality; to <em>actually</em>
 * go "true vertically north", you have to go first {@link #N} and then
 * {@link #NW}, taking two boards to actually go straight north (by an offset of
 * 12 chips); {@code a} is vertically north of {@code c}, and {@code f} is
 * vertically north of {@code d}.
 *
 * @see DirInfo
 * @author Donal Fellows
 */
// TODO Use an image instead of ASCII art; that'll let us get the angle right
public enum Direction {
	// Order must match that in database
	/** Northward, from {@code x} to {@code a}. */
	N(2, 0, "fpga_n"),
	/** Eastward, from {@code x} to {@code b}. */
	E(2, 1, "fpga_e"),
	/** Southeast, from {@code x} to {@code c}. */
	SE(0, 0, "fpga_se"),
	/** Southward, from {@code x} to {@code d}. */
	S(0, 1, "fpga_s"),
	/** Westward, from {@code x} to {@code e}. */
	W(1, 0, "fpga_w"),
	/** Northwest, from {@code x} to {@code f}. */
	NW(1, 1, "fpga_nw");

	/**
	 * The number of the FPGA that manages the link in this direction.
	 */
	public final int fpga;

	/**
	 * The address of the FPGA register bank that refers to the link.
	 */
	public final int addr;

	/**
	 * The name of the column in the {@code pending_changes} table
	 *         that holds information pertaining to this link.
	 */
	public final String columnName;

	Direction(int fpga, int bankSelect, String columnName) {
		this.fpga = fpga;
		this.addr = bankSelect * BANK_OFFSET_MULTIPLIER + STOP.offset;
		this.columnName = columnName;
	}

	/**
	 * Gets the direction of the link in the opposite direction. Only really
	 * valid for SpiNN-5 boards.
	 *
	 * @return The opposite direction.
	 */
	public Direction opposite() {
		return values()[(ordinal() + values().length) % values().length];
	}
}
