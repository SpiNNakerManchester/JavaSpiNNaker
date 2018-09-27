package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request to not receive notifications about a machine.
 */
public class NoNotifyMachineCommand extends Command<String> {
	//
	/**
	 * Create a request to not be notified of changes in machine state.
	 *
	 * @param machineName
	 *            The machine to request about.
	 */
	public NoNotifyMachineCommand(String machineName) {
		super("no_notify_machine");
		addArg(machineName);
	}

	/**
	 * Create a request to not be notified of changes in all machines' state.
	 */
	public NoNotifyMachineCommand() {
		super("no_notify_machine");
	}
}
