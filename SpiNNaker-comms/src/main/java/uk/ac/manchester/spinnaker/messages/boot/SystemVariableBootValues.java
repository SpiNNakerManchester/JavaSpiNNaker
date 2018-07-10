package uk.ac.manchester.spinnaker.messages.boot;

import static uk.ac.manchester.spinnaker.messages.boot.SystemVariableDefinition.hardware_version;
import static uk.ac.manchester.spinnaker.messages.boot.SystemVariableDefinition.led_0;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/**
 * Default values of the system variables that get passed to SpiNNaker during
 * boot.
 */
public class SystemVariableBootValues implements SerializableMessage {
	/** The size of the boot variable block, in bytes. */
	static final int BOOT_VARIABLE_SIZE = 256;
	private static final SystemVariableBootValues[] BOOT_VALUES = { null,
			new SystemVariableBootValues(1, 0x00076104),
			new SystemVariableBootValues(2, 0x00006103),
			new SystemVariableBootValues(3, 0x00000502),
			new SystemVariableBootValues(4, 0x00000001),
			new SystemVariableBootValues(5, 0x00000001) };
	private final Map<SystemVariableDefinition, Object> values;

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
	 */
	public static SystemVariableBootValues get(int boardVersion) {
		switch (boardVersion) {
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
			return BOOT_VALUES[boardVersion];
		default:
			throw new IllegalArgumentException(
					"unknown SpiNNaker board version: " + boardVersion);
		}
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		for (SystemVariableDefinition svd : SystemVariableDefinition.values()) {
			svd.type.addToBuffer(values.get(svd), buffer);
		}
	}
}
