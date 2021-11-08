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
package uk.ac.manchester.spinnaker.messages.model;

/**
 * Main FPGA registers.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/spio/tree/master/designs/spinnaker_fpgas">
 *      Spio documentation</a>
 * @author Donal Fellows
 */
@SARKStruct("spio")
public enum FPGAMainRegisters {
	/** Top-level design version. */
	VERS(0x00, 32),
	/**
	 * Compile flags.
	 * <pre>
	 * {   5: chip scope,
	 *     4: peripheral support,
	 *     3: ring support,
	 *     2: north/south on front,
	 *   1-0: FPGA ID }
	 * </pre>
	 */
	FLAG(0x04, 6),
	/** Peripheral MC route key. (default: {@code 0xFFFFFFFF}) */
	PKEY(0x08, true, 32),
	/** Peripheral MC route mask. (default: {@code 0x00000000}) */
	PMSK(0x0C, true, 32),
	/**
	 * Scrambler on. (default: 0xF)
	 *
	 * <pre>
	 * { 3: ring link,
	 *   2: peripheral link,
	 *   1: board-to-board link1,
	 *   0: board-to-board link0 }
	 * </pre>
	 */
	SCRM(0x10, true, 4),
	/**
	 * Enable SpiNNaker chip (2-of-7) link. (Default: {@code 0x00000000})
	 *
	 * <pre>
	 * { 0: Link 0 SpiNN-&gt;FPGA enable,
	 *   1: Link 0 FPGA-&gt;SpiNN enable,
	 *   2: Link 1 SpiNN-&gt;FPGA enable,
	 *   3: Link 1 FPGA-&gt;SpiNN enable,
	 *   ... }
	 * </pre>
	 */
	SLEN(0x14, true, 32),
	/**
	 * Override status LED. (default: {@code 0x0F})
	 *
	 * <pre>
	 * { 7: DIM_RING,
	 *   6: DIM_PERIPH,
	 *   5: DIM_B2B1,
	 *   4: DIM_B2B0,
	 *   3: FORCE_ERROR_RING,
	 *   2: FORCE_ERROR_PERIPH,
	 *   1: FORCE_ERROR_B2B1,
	 *   0: FORCE_ERROR_B2B0 }
	 * </pre>
	 */
	LEDO(0x18, true, 8),
	/**
	 * Receive equalization. (default: {@code 0x0A})
	 *
	 * <pre>
	 * { 7-6: RING_RXEQMIX,
	 *   5-4: PERIPH_RXEQMIX,
	 *   3-2: B2B1_RXEQMIX,
	 *   1-0: B2B0_RXEQMIX }
	 * </pre>
	 */
	RXEQ(0x1C, true, 8),
	/**
	 * Transmit driver swing. (default: {@code 0x0066})
	 *
	 * <pre>
	 * { 15-12: RING_TXDIFFCTRL,
	 *    11-8: PERIPH_TXDIFFCTRL,
	 *     7-4: B2B1_TXDIFFCTRL,
	 *     3-0: B2B0_TXDIFFCTRL }
	 * </pre>
	 */
	TXDS(0x20, true, 16),
	/**
	 * Transmit pre-emphasis. (default: {@code 0x012})
	 *
	 * <pre>
	 * { 11-9: RING_TXPREEMPHASIS,
	 *    8-6: PERIPH_TXPREEMPHASIS,
	 *    5-3: B2B1_TXPREEMPHASIS,
	 *    2-0: B2B0_TXPREEMPHASIS }
	 * </pre>
	 */
	TXPE(0x24, true, 12);

	/**
	 * Base address of the main registers. Fixed.
	 */
	public static final int BASE_ADDRESS = 0x00040000;

	/** The offset of the register within the bank of registers. */
	public final int offset;

	/** Whether this is a writable register. */
	public final boolean writable;

	/** Size of register, in bits. */
	public final int size;

	FPGAMainRegisters(int offset, int size) {
		this(offset, false, size);
	}

	FPGAMainRegisters(int offset, boolean writable, int size) {
		this.offset = offset;
		this.writable = writable;
		this.size = size;
	}

	/**
	 * @return The address of the register in the FPGA's address space.
	 */
	public int getAddress() {
		return BASE_ADDRESS + offset;
	}
}
