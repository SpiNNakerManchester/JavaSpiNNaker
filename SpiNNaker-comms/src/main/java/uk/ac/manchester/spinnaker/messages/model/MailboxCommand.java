package uk.ac.manchester.spinnaker.messages.model;

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

	private MailboxCommand(int value) {
		this.value = value;
	}
}
