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
package uk.ac.manchester.spinnaker.messages.scp;

/** Standard bit shifts. */
abstract class Bits {
	private Bits() {
	}

	/** The top bit of the word. */
	static final int TOP_BIT = 31;
	/** Bits 31&ndash;24. */
	static final int BYTE3 = 24;
	/** Bits 23&ndash;16. */
	static final int BYTE2 = 16;
	/** Bits 15&ndash;8. */
	static final int BYTE1 = 8;
	/** Bits 7&ndash;0. */
	static final int BYTE0 = 0;
	/** Bits 31&ndash;16. */
	static final int HALF1 = 16;
	/** Bits 15&ndash;0. */
	static final int HALF0 = 0;
}
