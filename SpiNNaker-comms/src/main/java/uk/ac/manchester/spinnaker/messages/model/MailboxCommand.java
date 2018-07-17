package uk.ac.manchester.spinnaker.messages.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Commands sent between an application and the monitor processor
 */
public enum MailboxCommand {
	/** The mailbox is idle */
	SHM_IDLE(0),
	/** The mailbox contains an SDP message */
	SHM_MSG(1),
	/** The mailbox contains a non-operation */
	SHM_NOP(2),
	/** The mailbox contains a signal */
	SHM_SIGNAL(3),
	/** The mailbox contains a command */
	SHM_CMD(4);
	public final int value;
	private static final Map<Integer, MailboxCommand> map = new HashMap<>();
	static {
		for (MailboxCommand v : values()) {
			map.put(v.value, v);
		}
	}

	private MailboxCommand(int value) {
		this.value = value;
	}

	public static MailboxCommand get(int value) {
		return map.get(value);
	}
}
