package uk.ac.manchester.spinnaker.messages.eieio;

/**
 * Describes a class that can be a command identifier for an EIEIO command
 * message.
 *
 * @author Donal Fellows
 */
public interface EIEIOCommand {
	/**
	 * Get the encoded ID number of the command.
	 *
	 * @return The encoded form.
	 */
	int getValue();
}
