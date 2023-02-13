/*
 * Copyright (c) 2018-2023 The University of Manchester
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

import static java.lang.String.format;

/**
 * An exception which occurs when trying to execute an unimplemented command.
 */
public class UnimplementedDSECommandException
		extends UnsupportedOperationException {
	private static final long serialVersionUID = -2215740111501788733L;

	/**
	 * Create an instance.
	 *
	 * @param index
	 *            Where the command was located.
	 * @param command
	 *            The command that was unimplemented.
	 */
	UnimplementedDSECommandException(int index, Commands command) {
		super(format(
				"Command %s (at index %d) in the data specification "
						+ "executor has not yet been implemented",
				command, index));
	}

	/**
	 * Create an instance.
	 *
	 * @param index
	 *            Where the opcode was located.
	 * @param opcode
	 *            The opcode that couldn't be converted into a command.
	 */
	UnimplementedDSECommandException(int index, int opcode) {
		super(format("unknown opcocode (%d) at index %d", opcode, index));
	}
}
