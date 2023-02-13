/*
 * Copyright (c) 2021-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.model;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 * FPGA registers within a register bank.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/spio/tree/master/designs/spinnaker_fpgas">
 *      Spio documentation</a>
 * @author Donal Fellows
 */
@SARKStruct("spio")
public enum FPGALinkRegisters {
	/** Version. {top 24: Module, bottom 8: Protocol} */
	VERS(0x00, 32),
	/** CRC error counter. */
	CRCE(0x04, 32),
	/** Frame error counter. */
	FRME(0x08, 32),
	/** Packet dispatcher busy counter. */
	BUSY(0x0C, 32),
	/** Local nack'd frame counter. */
	LNAK(0x10, 32),
	/** Remote nack counter. */
	RNAK(0x14, 32),
	/** Local ack'd frame counter. */
	LACK(0x18, 32),
	/** Remote ack counter. */
	RACK(0x1C, 32),
	/** Local out-of-credit counter. */
	LOOC(0x20, 32),
	/** Remote out-of-credit counter. */
	ROOC(0x24, 32),
	/** Credit. */
	CRDT(0x28, 3),
	/** Frame assembler valid sent frame counter. */
	SFRM(0x2C, 32),
	/** Frame transmitter frame counter. */
	TFRM(0x30, 32),
	/** Frame disassembler valid frame counter. */
	DFRM(0x34, 32),
	/** Packet dispatcher valid received frame counter. */
	RFRM(0x38, 32),
	/** Empty frame assembler queues. */
	EMPT(0x3C, 8),
	/** Full frame assembler queues. */
	FULL(0x40, 8),
	/** Local channel flow control status. */
	CFCL(0x44, 8),
	/** Remote channel flow control status. */
	CFCR(0x48, 8),
	/** IDle Sentinel Output value. (Sent in idle frames.) */
	IDSO(0x4C, true, 16),
	/** IDle Sentinel Input value. (Latest received) */
	IDSI(0x50, 16),
	/** Handshake. {bit 1: version err, bit 0: complete} */
	HAND(0x54, 2),
	/** Link reconnection (re-handshake) counter. */
	RECO(0x58, 32),
	/** 1 = Stop sending data frames. (NB: Will still receive them) */
	STOP(0x5C, true, 1);

	/** The base addresses of register banks are all multiples of this. */
	public static final int BANK_OFFSET_MULTIPLIER = 0x00010000;

	/** The offset of the register within the bank of registers. */
	public final int offset;

	/** Whether this is a writable register. */
	public final boolean writable;

	/** Size of register, in bits. */
	public final int size;

	FPGALinkRegisters(int offset, int size) {
		this(offset, false, size);
	}

	FPGALinkRegisters(int offset, boolean writable, int size) {
		this.offset = offset;
		this.writable = writable;
		this.size = size;
	}

	/**
	 * Compute the address of the register in a particular register bank.
	 *
	 * @param registerBank
	 *            Which register bank. Must be 0, 1, or 2.
	 * @return The address in the FPGA's address space.
	 * @throws IllegalArgumentException
	 *             If a bad register bank is given.
	 */
	public MemoryLocation address(int registerBank) {
		if (registerBank < 0 || registerBank > 2) {
			throw new IllegalArgumentException(
					"registerBank must be 0, 1, or 2");
		}
		return new MemoryLocation(
				BANK_OFFSET_MULTIPLIER * registerBank + offset);
	}
}
