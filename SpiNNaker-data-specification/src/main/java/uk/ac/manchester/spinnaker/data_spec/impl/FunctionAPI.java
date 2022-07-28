/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.data_spec.impl;

import static uk.ac.manchester.spinnaker.data_spec.impl.Commands.BREAK;
import static uk.ac.manchester.spinnaker.data_spec.impl.Commands.NOP;
import static uk.ac.manchester.spinnaker.data_spec.impl.Functions.OPCODE;
import static uk.ac.manchester.spinnaker.data_spec.impl.OperationMapper.getOperationImpl;

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

	/**
	 * Get the implementation of an operation.
	 *
	 * @param cmdOpcode
	 *            The <i>encoded</i> opcode value.
	 * @param index
	 *            The location in the data specification data stream where the
	 *            command was read from, for reporting errors to the user.
	 * @return The operation implementation. Never {@code null}.
	 * @throws UnimplementedDSECommandException
	 *             If the opcode used is not implemented.
	 */
	default Callable getOperation(int cmdOpcode, int index)
			throws UnimplementedDSECommandException {
		int opcode = OPCODE.getValue(cmdOpcode);
		var c = Commands.get(opcode);
		if (c == null) {
			throw new UnimplementedDSECommandException(index, opcode);
		}
		var opImpl = getOperationImpl(this, c);
		if (opImpl == null) {
			throw new UnimplementedDSECommandException(index, c);
		}
		return opImpl;
	}

	/**
	 * This command executes no operation.
	 */
	@Operation(NOP)
	default void doNOP() {
		// Does nothing
	}

	/**
	 * This command raises an exception to stop the execution of the data
	 * specification executor (DSE).
	 *
	 * @throws ExecuteBreakInstruction
	 *             Always
	 */
	@Operation(BREAK)
	default void doBreak() throws ExecuteBreakInstruction {
		throw new ExecuteBreakInstruction();
	}
}
