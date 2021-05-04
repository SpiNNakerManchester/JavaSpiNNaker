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
