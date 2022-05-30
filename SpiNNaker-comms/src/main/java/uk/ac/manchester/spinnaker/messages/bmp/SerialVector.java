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

import uk.ac.manchester.spinnaker.messages.scp.SCPCommand;

/**
 * The data in the serial vector. The result of a {@link SCPCommand#CMD_BMP_INFO
 * BMP_INFO} call with argument {@link BMPInfo#SERIAL SERIAL}.
 * <p>
 * See {@code cmd_bmp_info()} in {@code bmp_cmd.c}.
 */
public class SerialVector {
	/** Hardware version. */
	public final int hardwareVersion;

	/** LPC1768 serial number. */
	public final int[] serialNumber;

	/** Flash buffer address. */
	public final int flashBuffer;

	/** {@code board_stat} address (this board). */
	public final int boardStat;

	/** Cortex boot vector address. */
	public final int cortexBootVector;

	private static final int SERIAL_NUMBER_LENGTH = 4;

	/**
	 * @param buffer
	 *            Where to deserialize the vector from.
	 */
	SerialVector(ByteBuffer buffer) {
		IntBuffer b = buffer.asIntBuffer();
		hardwareVersion = b.get();
		serialNumber = new int[SERIAL_NUMBER_LENGTH];
		b.get(serialNumber);
		flashBuffer = b.get();
		boardStat = b.get();
		cortexBootVector = b.get();
	}

	static {
		SCPCommand.class.getClass();
	}
}
