package uk.ac.manchester.spinnaker.data_spec.exceptions;

import static java.lang.String.format;

import uk.ac.manchester.spinnaker.data_spec.Commands;

/**
 * An exception which occurs when trying to execute an unimplemented command.
 */
@SuppressWarnings("serial")
public class UnimplementedDSECommandException
		extends UnsupportedOperationException {
	/**
	 * Create an instance.
	 *
	 * @param index
	 *            Where the command was located.
	 * @param command
	 *            The command that was unimplemented.
	 */
	public UnimplementedDSECommandException(int index, Commands command) {
		super(format("Command %s (at index %d) in the data specification "
				+ "executor has not yet been implemented", command, index));
	}

	/**
	 * Create an instance.
	 *
	 * @param index
	 *            Where the opcode was located.
	 * @param opcode
	 *            The opcode that couldn't be converted into a command.
	 */
	public UnimplementedDSECommandException(int index, int opcode) {
		super(format("unknown opcocode (%d) at index %d", opcode, index));
	}
}
