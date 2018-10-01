package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request to get notifications about a machine.
 */
public class NotifyMachineCommand extends Command<String> {
	/**
	 * Create a request to be notified of changes in machine state.
	 *
	 * @param machineName
	 *            The machine to request about.
	 */
	public NotifyMachineCommand(String machineName) {
		super("notify_machine");
		addArg(machineName);
	}

	/**
	 * Create a request to be notified of changes in all machines' state.
	 */
	public NotifyMachineCommand() {
		super("notify_machine");
	}
}
