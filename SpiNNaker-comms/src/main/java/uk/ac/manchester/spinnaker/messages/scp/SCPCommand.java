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
package uk.ac.manchester.spinnaker.messages.scp;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

/** The SCP Command codes. */
public enum SCPCommand implements CommandCode {
	/** Get SCAMP Version. */
	CMD_VER(0),
	/**
	 * Run at PC.
	 *
	 * @deprecated see {@link #CMD_AS}
	 */
	@Deprecated
	CMD_RUN(1),
	/** Read SDRAM. */
	CMD_READ(2),
	/** Write SDRAM. */
	CMD_WRITE(3),
	/**
	 * Run via APLX.
	 *
	 * @deprecated see {@link #CMD_AS}
	 */
	@Deprecated
	CMD_APLX(4),
	/** Fill memory. */
	CMD_FILL(5),
	/** Remap application core. */
	CMD_REMAP(16),
	/** Read neighbouring chip's memory. */
	CMD_LINK_READ(17),
	/** Write neighbouring chip's memory. */
	CMD_LINK_WRITE(18),
	/** Application core reset. */
	CMD_AR(19),
	/** Send a broadcast Nearest-Neighbour packet. */
	CMD_NNP(20),
	/** Send a Signal. */
	CMD_SIG(22),
	/** Send Flood-Fill Data. */
	CMD_FFD(23),
	/** Application core APLX start. */
	CMD_AS(24),
	/** Control the LEDs. */
	CMD_LED(25),
	/** Set an IP tag. */
	CMD_IPTAG(26),
	/** Read/write/erase serial ROM. */
	CMD_SROM(27),
	/** Allocate or Free SDRAM or Routing entries. */
	CMD_ALLOC(28),
	/** Initialise the router. */
	CMD_RTR(29),
	/** Dropped Packet Reinjection setup. */
	CMD_DPRI(30),
	/** Get Chip Summary Information. */
	CMD_INFO(31),
	/** Get BMP info structures. */
	CMD_BMP_INFO(48),
	/** */
	CMD_FLASH_COPY(49),
	/** */
	CMD_FLASH_ERASE(50),
	/** */
	CMD_FLASH_WRITE(51),
	/** Serial flash access. */
	CMD_BMP_SF(53),
	/** EEPROM access? */
	@Deprecated
	CMD_BMP_EE(54),
	/** */
	CMD_RESET(55),
	/** FPGA control. BMP-only operation. */
	CMD_XILINX(56),
	/** Turns on or off the machine via BMP. */
	CMD_BMP_POWER(57),
	/** Access I2C bus. */
	@Deprecated
	CMD_BMP_I2C(61),
	/** Configure pulse-width modulation hardware. */
	@Deprecated
	CMD_BMP_PWM(62),
	/** */
	@Deprecated
	CMD_BMP_TEST(63),
	/** Tube output. */
	CMD_TUBE(64);

	/** The SCAMP encoding. */
	public final short value;

	private static final Map<Short, SCPCommand> MAP = new HashMap<>();

	SCPCommand(int value) {
		this.value = (short) value;
	}

	static {
		for (var r : values()) {
			MAP.put(r.value, r);
		}
	}

	/**
	 * Convert an encoded value into an enum element.
	 *
	 * @param value
	 *            The value to convert
	 * @return The enum element
	 */
	public static SCPCommand get(short value) {
		return requireNonNull(MAP.get(value),
				"unrecognised command value: " + value);
	}

	@Override
	public short getValue() {
		return value;
	}
}
