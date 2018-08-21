package uk.ac.manchester.spinnaker.messages.model;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Commands sent between an application and the monitor processor.
 */
public enum MailboxCommand {
	/** The mailbox is idle. */
	SHM_IDLE(0),
	/** The mailbox contains an SDP message. */
	SHM_MSG(1),
	/** The mailbox contains a non-operation. */
	SHM_NOP(2),
	/** The mailbox contains a signal. */
	SHM_SIGNAL(3),
	/** The mailbox contains a command. */
	SHM_CMD(4);

	/** The SARK value. */
	public final int value;
	private static final Map<Integer, MailboxCommand> MAP = new HashMap<>();
	static {
		for (MailboxCommand v : values()) {
			MAP.put(v.value, v);
		}
	}

	MailboxCommand(int value) {
		this.value = value;
	}

	/**
	 * Convert SARK value to enum.
	 *
	 * @param value
	 *            The value to convert.
	 * @return The enum member
	 */
	public static MailboxCommand get(int value) {
		return requireNonNull(MAP.get(value),
				"unknown mailbox command: " + value);
	}
}
