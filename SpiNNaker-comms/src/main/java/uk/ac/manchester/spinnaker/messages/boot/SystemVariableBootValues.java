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
package uk.ac.manchester.spinnaker.messages.boot;

import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.hardware_version;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.led_0;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.variables;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.machine.MachineVersion;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.SerializableMessage;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;

/**
 * Default values of the system variables that get passed to SpiNNaker during
 * boot.
 */
public class SystemVariableBootValues implements SerializableMessage {
	/** The size of the boot variable block, in bytes. */
	static final int BOOT_VARIABLE_SIZE = 256;

	private final Map<SystemVariableDefinition, Object> values;

	private boolean unmodifiable;

	/** Create a set of boot values using all the defaults. */
	public SystemVariableBootValues() {
		values = new HashMap<>();
		for (var svd : SystemVariableDefinition.values()) {
			values.put(svd, svd.getDefault());
		}
	}

	/**
	 * Create a set of boot values that is a copy of an existing set of boot
	 * values. <em>This new set will be modifiable.</em>
	 *
	 * @param original
	 *            The set of boot values to copy from.
	 */
	public SystemVariableBootValues(SystemVariableBootValues original) {
		values = new HashMap<>(original.values);
	}

	/**
	 * Set a particular boot value.
	 *
	 * @param systemVariable
	 *            The variable to set.
	 * @param value
	 *            The value to set it to. The type depends on the type of the
	 *            variable being set.
	 * @throws UnsupportedOperationException
	 *             If the boot values are not writable.
	 * @throws IllegalArgumentException
	 *             If the variable type doesn't match.
	 */
	public void setValue(SystemVariableDefinition systemVariable,
			Object value) {
		if (unmodifiable) {
			throw new UnsupportedOperationException(
					"the standard defaults are not modifiable");
		}
		switch (systemVariable.type) {
		case BYTE_ARRAY:
			var defbytes = (byte[]) values.get(systemVariable);
			if (!(value instanceof byte[])) {
				throw new IllegalArgumentException("need a byte array");
			}
			var newbytes = (byte[]) value;
			if (newbytes.length != defbytes.length) {
				throw new IllegalArgumentException(
						"byte array length must be " + defbytes.length);
			}
			break;
		case ADDRESS:
			if (!(value instanceof MemoryLocation)) {
				throw new IllegalArgumentException("need a memory location");
			}
			break;
		default:
			if (!(value instanceof Number)) {
				throw new IllegalArgumentException("need an integer");
			}
			break;
		}
		values.put(systemVariable, value);
	}

	/**
	 * Get the default values of the system variables that get passed to
	 * SpiNNaker during boot for a particular version of SpiNNaker board. This
	 * set of defaults will be <em>unmodifiable</em> and must be copied (with
	 * the appropriate constructor) to be changed.
	 *
	 * @param boardVersion
	 *            Which sort of SpiNN board is being booted.
	 * @return The defaults for the specific board.
	 * @throws IllegalArgumentException
	 *             if an unsupported board version is used
	 */
	public static SystemVariableBootValues get(int boardVersion) {
		var bv = BootValues.get(boardVersion);
		if (nonNull(bv)) {
			return bv;
		}
		throw new IllegalArgumentException(
				"unknown SpiNNaker board version: " + boardVersion);
	}

	/**
	 * Get the default values of the system variables that get passed to
	 * SpiNNaker during boot for a particular version of SpiNNaker board. This
	 * set of defaults will be <em>unmodifiable</em> and must be copied (with
	 * the appropriate constructor) to be changed.
	 *
	 * @param boardVersion
	 *            Which sort of SpiNN board is being booted.
	 * @return The defaults for the specific board.
	 * @throws IllegalArgumentException
	 *             if an unsupported board version is used
	 */
	public static SystemVariableBootValues get(MachineVersion boardVersion) {
		var bv = BootValues.get(boardVersion.hardwareVersion());
		if (nonNull(bv)) {
			return bv;
		}
		throw new IllegalArgumentException(
				"unknown SpiNNaker board version: " + boardVersion);
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		int base = buffer.position();
		for (var svd : variables()) {
			buffer.position(base + svd.offset);
			svd.addToBuffer(values.get(svd), buffer);
		}
	}

	/** Mark this object as unmodifiable. */
	void unmodifiable() {
		unmodifiable = true;
	}

	private static class BootValues {
		private static final SystemVariableBootValues[] MAP;

		/**
		 * Deeply magical values, used to configure board LED states. Note that
		 * index 0 corresponds to board hardware version 1, index 1 to hardware
		 * version 2, etc.
		 */
		private static final int[] LED0 = {
			0x00076104, 0x00006103, 0x00000502, 0x00000001, 0x00000001
		};

		static {
			MAP = new SystemVariableBootValues[LED0.length + 1];
			int hwver = 1;
			for (int led0 : LED0) {
				var bv = new SystemVariableBootValues();
				bv.setValue(hardware_version, hwver);
				bv.setValue(led_0, led0);
				bv.unmodifiable();
				MAP[hwver] = bv;
				hwver++;
			}
		}

		/**
		 * Look up the defaults for a particular version of board.
		 *
		 * @param boardVersion
		 *            The board version.
		 * @return The defaults. Note that this should be treated as
		 *         <i>unmodifiable</i>.
		 */
		static SystemVariableBootValues get(int boardVersion) {
			if (boardVersion > 0 && boardVersion < MAP.length) {
				return MAP[boardVersion];
			}
			return null;
		}
	}
}
