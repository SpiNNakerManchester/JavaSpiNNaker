package uk.ac.manchester.spinnaker.messages.eieio;

/**
 * A non-standard EIEIO command. Note that its constructor is not exposed.
 *
 * @see EIEIOCommandID#get(int)
 * @author Donal Fellows
 */
public class CustomEIEIOCommand implements EIEIOCommand {
	// Must be power of 2 (minus 1)
	private static final int MAX_COMMAND = 0x3FFF;
	private final int command;

	/**
	 * @param command
	 *            The ID value of the command.
	 */
	CustomEIEIOCommand(int command) {
		if (command < 0 || command > MAX_COMMAND) {
			throw new IllegalArgumentException(
					"parameter command is outside the allowed range (0 to "
							+ MAX_COMMAND + ")");
		}
		this.command = command;
	}

	@Override
	public int getValue() {
		return command;
	}
}
