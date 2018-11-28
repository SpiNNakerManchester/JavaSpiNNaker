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
package uk.ac.manchester.spinnaker.messages.model;

/**
 * The registers on an ARM. Or rather the conventional ones.
 */
enum ARMRegisters {
	/** Register r0. */
	r0,
	/** Register r1. */
	r1,
	/** Register r2. */
	r2,
	/** Register r3. */
	r3,
	/** Register r4. */
	r4,
	/** Register r5. */
	r5,
	/** Register r6. */
	r6,
	/** Register r7. */
	r7,
	/** Register r8. */
	r8,
	/** Register r9. */
	r9,
	/** Register r10. */
	r10,
	/** Register r11. */
	r11,
	/** Register r12. */
	r12,
	/** Stack pointer. */
	sp,
	/** Link register. */
	lr,
	/** Program counter. */
	pc,
	/** Status register. */
	apsr;
	int get(int[] registers) {
		return registers[ordinal()];
	}
}
