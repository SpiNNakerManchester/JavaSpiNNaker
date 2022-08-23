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
package uk.ac.manchester.spinnaker.transceiver;

import static uk.ac.manchester.spinnaker.messages.Constants.CPU_INFO_OFFSET;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_REGISTER_P2P_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.SYSTEM_VARIABLE_BASE_ADDRESS;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 * Common memory locations. Note that many locations really come in both
 * buffered and unbuffered varieties, but this class doesn't hold all standard
 * locations and versions:
 * <blockquote> Buffered SDRAM means that writes go through a write buffer.
 * Unbuffered means that they go directly to SDRAM. Reads are unaffected in
 * general. If you are writing lots of data, it is unlikely to matter much since
 * the write buffer is limited in size. Here you probably want to use Unbuffered
 * anyway, as it will then block until the write is definitely done. Using
 * Buffered writing means that the write may or may not have happened at the
 * time of the response. </blockquote>
 *
 * @author Donal Fellows
 */
public abstract class CommonMemoryLocations {
	private CommonMemoryLocations() {
	}

	/**
	 * Start of unbuffered access to SDRAM. Writes will block until they have
	 * completed.
	 */
	public static final MemoryLocation UNBUFFERED_SDRAM_START =
			new MemoryLocation(0x60000000);

	/** Location of routing table data in transit. Reserved by SCAMP. */
	public static final MemoryLocation ROUTING_TABLE_DATA =
			new MemoryLocation(0x67800000);

	/**
	 * Where executables are written to prior to launching them. Reserved by
	 * SCAMP.
	 */
	public static final MemoryLocation EXECUTABLE_ADDRESS =
			new MemoryLocation(0x67800000);

	/**
	 * Start of buffered access to SDRAM. Writes will finish rapidly, but data
	 * may take some cycles to appear in memory.
	 * <p>
	 * It doesn't matter too much when working from host; the time to do the
	 * network communications is rather longer than the normal buffering time
	 * for SDRAM.
	 */
	public static final MemoryLocation BUFFERED_SDRAM_START =
			new MemoryLocation(0x70000000);

	/** Location of the bank of memory-mapped router registers. Buffered. */
	public static final MemoryLocation ROUTER_BASE =
			new MemoryLocation(0xe1000000);

	/** Location of the memory-mapped router control register (r0). */
	public static final MemoryLocation ROUTER_CONTROL =
			new MemoryLocation(0xe1000000);

	/** Location of the memory-mapped router error register (r5). */
	public static final MemoryLocation ROUTER_ERROR =
			new MemoryLocation(0xe1000014);

	/**
	 * Where to write router diagnostic counters control data to (r11).
	 * Unbuffered.
	 */
	public static final MemoryLocation ROUTER_DIAGNOSTIC_COUNTER =
			new MemoryLocation(0xf100002c);

	/** Location of the memory-mapped router filtering registers (rFN). */
	public static final MemoryLocation ROUTER_FILTERS =
			new MemoryLocation(0xe1000200);

	/** Location of the memory-mapped router diagnostics registers (rCN). */
	public static final MemoryLocation ROUTER_DIAGNOSTICS =
			new MemoryLocation(0xe1000300);

	/** The base address of a router's P2P routing table, 3 bits per route. */
	public static final MemoryLocation ROUTER_P2P =
			new MemoryLocation(ROUTER_REGISTER_P2P_ADDRESS);

	/** Where the CPU information structure is located. Buffered system RAM. */
	public static final MemoryLocation CPU_INFO =
			new MemoryLocation(CPU_INFO_OFFSET);

	/** Where the system variables are located. Unbuffered system RAM. */
	public static final MemoryLocation SYS_VARS =
			new MemoryLocation(SYSTEM_VARIABLE_BASE_ADDRESS);
}
