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
package uk.ac.manchester.spinnaker.data_spec.impl;

/** Constants used by DSG command encoding. */
public interface EncodingConstants {
	// Constants used by DSG command encoding: payload length field

	/** DSG command is one word long. */
	int LEN1 = 0;

	/** DSG command is two words long. */
	int LEN2 = 1;

	/** DSG command is three words long. */
	int LEN3 = 2;

	/** DSG command is four words long. */
	int LEN4 = 3;

	// Constants used by DSG command encoding: register usage field

	/** DSG command uses no registers. */
	int NO_REGS = 0;

	/** DSG command uses just the src2 register. */
	int SRC2_ONLY = 1;

	/** DSG command uses just the src1 register. */
	int SRC1_ONLY = 2;

	/** DSG command uses just the src1 and src2 registers. */
	int SRC1_AND_SRC2 = 3;

	/** DSG command uses just the dest register. */
	int DEST_ONLY = 4;

	/** DSG command uses just the src2 and dest registers. */
	int DEST_AND_SRC2 = 5;

	/** DSG command uses just the src1 and dest registers. */
	int DEST_AND_SRC1 = 6;

	/** DSG command uses the src1, src2 and dest registers. */
	int ALL_REGS = 7;

	/** return values from functions of the data spec executor. */
	int END_SPEC_EXECUTOR = -1;
}
