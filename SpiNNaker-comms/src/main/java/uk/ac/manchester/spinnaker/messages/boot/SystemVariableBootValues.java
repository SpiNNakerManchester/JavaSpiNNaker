package uk.ac.manchester.spinnaker.messages.boot;

import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.hardware_version;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.led_0;

import java.nio.ByteBuffer;
import java.util.Collections;
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

	@SuppressWarnings("checkstyle:MagicNumber")
	private static class BootValues {
		private static final Map<Integer, SystemVariableBootValues> MAP;
		static {
			HashMap<Integer, SystemVariableBootValues> bootValues =
					new HashMap<>();
			bootValues.put(1, new SystemVariableBootValues(1, 0x00076104));
			bootValues.put(2, new SystemVariableBootValues(2, 0x00006103));
			bootValues.put(3, new SystemVariableBootValues(3, 0x00000502));
			bootValues.put(4, new SystemVariableBootValues(4, 0x00000001));
			bootValues.put(5, new SystemVariableBootValues(5, 0x00000001));
			MAP = Collections.unmodifiableMap(bootValues);
		};
	}

	private final Map<SystemVariableDefinition, Object> values;

	/** Create a set of boot values using all the defaults. */
	public SystemVariableBootValues() {
		values = new HashMap<>();
		for (SystemVariableDefinition svd : SystemVariableDefinition.values()) {
			values.put(svd, svd.getDefault());
		}
	}

	private SystemVariableBootValues(int hardwareVersion, int led0) {
		this();
		values.put(hardware_version, hardwareVersion);
		values.put(led_0, led0);
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
		SystemVariableBootValues bv = BootValues.MAP.get(boardVersion);
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
}
