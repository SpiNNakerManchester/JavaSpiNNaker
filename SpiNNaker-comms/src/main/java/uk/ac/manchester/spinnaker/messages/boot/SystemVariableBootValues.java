package uk.ac.manchester.spinnaker.messages.boot;

import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.hardware_version;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.led_0;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

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
		for (SystemVariableDefinition svd : SystemVariableDefinition.values()) {
			values.put(svd, svd.getDefault());
		}
	}

	/**
	 * Create a set of boot values that is a copy of an existing set of boot
	 * values.
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
	 */
	public void setValue(SystemVariableDefinition systemVariable,
			Object value) {
		if (unmodifiable) {
			throw new UnsupportedOperationException(
					"the standard defaults are not modifiable");
		}
		switch (systemVariable.type) {
		case BYTE_ARRAY:
			byte[] defbytes = (byte[]) values.get(systemVariable);
			if (!(value instanceof byte[])) {
				throw new IllegalArgumentException("need a byte array");
			}
			byte[] newbytes = (byte[]) value;
			if (newbytes.length != defbytes.length) {
				throw new IllegalArgumentException(
						"byte array length must be " + defbytes.length);
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
	 * SpiNNaker during boot for a particular version of SpiNNaker board.
	 *
	 * @param boardVersion
	 *            Which sort of SpiNN board is being booted.
	 * @return The defaults for the specific board.
	 */
	public static SystemVariableBootValues get(int boardVersion) {
		SystemVariableBootValues bv = BootValues.get(boardVersion);
		if (bv != null) {
			return bv;
		}
		throw new IllegalArgumentException(
				"unknown SpiNNaker board version: " + boardVersion);
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		for (SystemVariableDefinition svd : SystemVariableDefinition.values()) {
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
				SystemVariableBootValues bv = new SystemVariableBootValues();
				bv.setValue(hardware_version, hwver);
				bv.setValue(led_0, led0);
				bv.unmodifiable();
				MAP[hwver] = bv;
				hwver++;
			}
		};

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
