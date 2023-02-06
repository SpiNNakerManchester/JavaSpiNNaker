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
package uk.ac.manchester.spinnaker.messages.bmp;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPCommand;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * The data in the serial vector. The result of a {@link SCPCommand#CMD_BMP_INFO
 * BMP_INFO} call with argument {@link BMPInfo#SERIAL SERIAL}.
 * <p>
 * See {@code cmd_bmp_info()} in {@code bmp_cmd.c}.
 *
 * @see ReadSerialVector
 */
@UsedInJavadocOnly(SCPCommand.class)
public class SerialVector {
	/** The number of words in the {@link #getSerialNumber() serial_number}. */
	public static final int SERIAL_LENGTH = 4;

	/** Hardware version. */
	private final int hardwareVersion;

	/** LPC1768 serial number. Length 4. */
	private final IntBuffer serialNumber;

	/** Flash buffer address. */
	private final MemoryLocation flashBuffer;

	/** {@code board_stat} address for the board. */
	private final MemoryLocation boardStat;

	/** Cortex boot vector address. */
	private final MemoryLocation cortexBoot;

	/**
	 * @param buffer
	 *            Where to deserialize the vector from.
	 */
	SerialVector(ByteBuffer buffer) {
		var b = buffer.asIntBuffer();
		hardwareVersion = b.get();
		var sn = new int[SERIAL_LENGTH];
		b.get(sn);
		serialNumber = IntBuffer.wrap(sn);
		flashBuffer = new MemoryLocation(b.get());
		boardStat = new MemoryLocation(b.get());
		cortexBoot = new MemoryLocation(b.get());
	}

	/** @return The hardware version. */
	public int getHardwareVersion() {
		return hardwareVersion;
	}

	/**
	 * @return The device serial number, as a read-only buffer.
	 */
	// @formatter:off
	/* Obtained from LPC17xx In Application Programming function 58. The API
	 * descriptions for these things are fairly well buried in the LPC17xx User
	 * Manual. This is the relevant part:
	 *
	 * Command      Read device serial number
	 * Input        Command code: 58<sub>10</sub>
	 *              Parameters:   None
	 * Return Code  CMD_SUCCESS |
	 * Result       Result0: First 32-bit word of Device Identification Number
	 *                       (at the lowest address)
	 *              Result1: Second 32-bit word of Device Identification Number
	 *              Result2: Third 32-bit word of Device Identification Number
	 *              Result3: Fourth 32-bit word of Device Identification Number
	 * Description  This command is used to read the device identification
	 *              number. The serial number may be used to uniquely identify
	 *              a single unit among all LPC17xx devices.
	 *
	 * The four words of the result form the four words provided below, in the
	 * order described above (not that that typically matters). */
	// @formatter:on
	public IntBuffer getSerialNumber() {
		// Make a new instance so positions aren't shared
		return serialNumber.asReadOnlyBuffer();
	}

	/** @return The location of the flash buffer. */
	public MemoryLocation getFlashBuffer() {
		return flashBuffer;
	}

	/**
	 * @return The board status block location.
	 * @see ReadADC
	 */
	public MemoryLocation getBoardStatusLocation() {
		return boardStat;
	}

	/** @return The location of the Cortex boot vector. */
	public MemoryLocation getCortexVector() {
		return cortexBoot;
	}
}
