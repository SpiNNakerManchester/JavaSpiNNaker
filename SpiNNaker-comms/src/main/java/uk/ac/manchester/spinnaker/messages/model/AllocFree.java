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

/** The SCP Allocation and Free codes. */
public enum AllocFree {
	/** Allocate SDRAM. */
	ALLOC_SDRAM(0),
	/** Free SDRAM using a Pointer. */
	FREE_SDRAM_BY_POINTER(1),
	/** Free SDRAM using an APP ID. */
	FREE_SDRAM_BY_APP_ID(2),
	/** Allocate Routing Entries. */
	ALLOC_ROUTING(3),
	/** Free Routing Entries by Pointer. */
	FREE_ROUTING_BY_POINTER(4),
	/** Free Routing Entries by APP ID. */
	FREE_ROUTING_BY_APP_ID(5);

	/** The SARK operation value. */
	public final byte value;

	AllocFree(int value) {
		this.value = (byte) value;
	}
}
