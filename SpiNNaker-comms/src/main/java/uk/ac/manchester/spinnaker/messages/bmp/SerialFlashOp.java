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
package uk.ac.manchester.spinnaker.messages.bmp;

import uk.ac.manchester.spinnaker.messages.scp.SCPCommand;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * The serial flash operations for argument three of
 * {@link SCPCommand#CMD_BMP_SF}.
 */
@UsedInJavadocOnly(SCPCommand.class)
enum SerialFlashOp {
	/** Read from the Serial Flash. */
	READ(0),
	/** write to the Serial Flash. */
	WRITE(1),
	/** Get the CRC of Serial Flash. */
	CRC(2);

	/** The raw BMP value. */
	public final byte value;

	SerialFlashOp(int value) {
		this.value = (byte) value;
	}
}
