/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.data_spec;

import static uk.ac.manchester.spinnaker.data_spec.Commands.BREAK;
import static uk.ac.manchester.spinnaker.data_spec.Commands.NOP;
import static uk.ac.manchester.spinnaker.data_spec.Functions.OPCODE;
import static uk.ac.manchester.spinnaker.data_spec.OperationMapper.getOperationImpl;

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
