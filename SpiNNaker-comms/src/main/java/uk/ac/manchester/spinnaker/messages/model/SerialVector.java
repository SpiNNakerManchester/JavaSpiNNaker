/*
 * Copyright (c) 2022 The University of Manchester
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

import java.nio.IntBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.bmp.ReadADC;
import uk.ac.manchester.spinnaker.messages.bmp.ReadSerialVector;

/**
 * The data in the BMP serial vector.
 * <p>
 * See {@code cmd_bmp_info()} in {@code bmp_cmd.c}.
 *
 * @param hardwareVersion
 *            Hardware version.
 * @param serialNumber
 *            LPC1768 serial number. Length {@value #SERIAL_LENGTH}.
 * @param flashBuffer
 *            Flash buffer address.
 * @param boardStat
 *            {@code board_stat} address for the board. See {@link ReadADC} for
 *            an operation that reads this.
 * @param cortexBoot
 *            Cortex boot vector address. Can be used to determine which copy of
 *            the BMP code was successfully booted from.
 * @see ReadSerialVector
 */
public record SerialVector(int hardwareVersion, IntBuffer serialNumber,
		MemoryLocation flashBuffer, MemoryLocation boardStat,
		MemoryLocation cortexBoot) {
	/** The number of words in the {@link #serialNumber() serial_number}. */
	public static final int SERIAL_LENGTH = 4;

	/**
	 * @return The device serial number, as a read-only buffer. Length
	 *         {@value #SERIAL_LENGTH}.
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
	public IntBuffer serialNumber() {
		// Make a new instance so positions aren't shared
		return serialNumber.asReadOnlyBuffer();
	}
}
