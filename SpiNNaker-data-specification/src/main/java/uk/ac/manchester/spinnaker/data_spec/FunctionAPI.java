package uk.ac.manchester.spinnaker.data_spec;

import static uk.ac.manchester.spinnaker.data_spec.Commands.BREAK;
import static uk.ac.manchester.spinnaker.data_spec.Commands.NOP;
import static uk.ac.manchester.spinnaker.data_spec.Functions.OPCODE_FIELD;
import static uk.ac.manchester.spinnaker.data_spec.Functions.OPCODE_MASK;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.data_spec.exceptions.ExecuteBreakInstruction;
import uk.ac.manchester.spinnaker.data_spec.exceptions.UnimplementedDSECommandException;

/**
 * The interface supported by the {@link Functions} class. This makes generating
 * an {@link Callable} much easier.
 *
 * @author Donal Fellows
 */
public interface FunctionAPI {
	/**
	 * Decode the command, storing the fields in variables in this object.
	 *
	 * @param cmd
	 *            The encoded command
	 */
	void unpack(int cmd);

	default Callable getOperation(int cmd, int index)
			throws DataSpecificationException {
		int opcode = (cmd >> OPCODE_FIELD) & OPCODE_MASK;
		Commands c = Commands.get(opcode);
		if (c == null) {
			throw new DataSpecificationException(
					"unknown opcocode at index " + index + ": " + opcode);
		}
		Callable opImpl = OperationMapper.getOperationImpl(this, c);
		if (opImpl == null) {
			throw new UnimplementedDSECommandException(c);
		}
		return opImpl;
	}

	/**
	 * This command executes no operation.
	 */
	@Operation(NOP)
	default void execute_nop() {
		// Does nothing
	}

	/**
	 * This command raises an exception to stop the execution of the data
	 * specification executor (DSE).
	 */
	@Operation(BREAK)
	default void execute_break() throws DataSpecificationException {
		throw new ExecuteBreakInstruction();
	}
}
