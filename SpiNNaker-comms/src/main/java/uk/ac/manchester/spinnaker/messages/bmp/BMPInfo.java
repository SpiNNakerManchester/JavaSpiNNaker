/*
 * Copyright (c) 2018-2022 The University of Manchester
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

/** The SCP BMP Information Types. */
public enum BMPInfo {
	/** Serial flash information. */
	SERIAL(0),
	/** Data read from EEPROM. {@code ee_data_t} contents. */
	EE_BUF(1),
	/** CAN status information. */
	CAN_STATUS(2),
	/** ADC information. */
	ADC(3),
	/** IP Address. */
	IP_ADDR(4),
	/** Uninitialised vector. 8 words, from {@code uni_vec}. */
	UNINIT_VEC(5);

	/** The raw BMP value. */
	public final byte value;

	BMPInfo(int value) {
		this.value = (byte) value;
	}
}
