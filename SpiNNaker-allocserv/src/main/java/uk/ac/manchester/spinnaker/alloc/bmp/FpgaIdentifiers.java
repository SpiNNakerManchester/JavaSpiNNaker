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
package uk.ac.manchester.spinnaker.alloc.bmp;

/**
 * SpiNNaker FPGA identifiers.
 *
 * @see <a href=
 *      "http://spinnakermanchester.github.io/docs/spin5-links.pdf">SpiNN-5 FPGA
 *      SATA Links</a> datasheet.
 */
// No idea if this terminology matches up elsewhere in Spalloc
public enum FpgaIdentifiers {
	/** Handles east and south. */
	FPGA_E_S,
	/** Handles south-west and west. */
	FPGA_SW_W,
	/** Handles north and north-east. */
	FPGA_N_NE
}
