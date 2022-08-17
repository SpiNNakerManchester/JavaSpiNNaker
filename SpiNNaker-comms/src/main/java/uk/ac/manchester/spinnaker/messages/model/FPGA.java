/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.model;

/**
 * Identifiers for the FPGAs on a SpiNNaker board, as managed by BMP.
 * <p>
 * SpiNNaker FPGA identifiers, taken from the
 * <a href="http://spinnakermanchester.github.io/docs/spin5-links.pdf">SpiNN-5
 * FPGA SATA Links</a> datasheet.
 */
public enum FPGA {
	/** The first FPGA. Handles east and south. */
	FPGA_E_S("0", 0, 1),
	/** The second FPGA. Handles south-west and west. */
	FPGA_SW_W("1", 1, 2),
	/** The third FPGA. Handles north and north-east. */
	FPGA_N_NE("2", 2, 4),
	/**
	 * All three FPGAs. Note that only a subset of APIs that handle FPGAs will
	 * accept this.
	 */
	FPGA_ALL("0-2", 3, 7);

	/** The "name" of the FPGA. */
	public final String name;

	/** The FPGA identifier in protocol terms. */
	public final int value;

	/** The bit encoding for read and write requests. */
	public final int bits;

	FPGA(String name, int value, int bits) {
		this.name = name;
		this.value = value;
		this.bits = bits;
	}

	@Override
	public String toString() {
		return name;
	}

	/** @return Whether this identifies a single FPGA. */
	public boolean isSingleFPGA() {
		return this != FPGA_ALL;
	}
}
