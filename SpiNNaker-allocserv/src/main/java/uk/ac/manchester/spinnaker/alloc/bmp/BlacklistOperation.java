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
package uk.ac.manchester.spinnaker.alloc.bmp;

/**
 * Non-boot operations that may be performed on a BMP.
 *
 * @author Donal Fellows
 */
public enum BlacklistOperation {
	// Careful: values must match SQL (CHECK constraints and queries)
	/** Read a blacklist from a board's BMP's flash. */
	READ,
	/** Write a blacklist to a board's BMP's flash. */
	WRITE,
	/** Read the serial numbers from a board's BMP. */
	GET_SERIAL
}
