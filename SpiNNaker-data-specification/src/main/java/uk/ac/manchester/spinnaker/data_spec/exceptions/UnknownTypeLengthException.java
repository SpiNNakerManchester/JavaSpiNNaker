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
package uk.ac.manchester.spinnaker.data_spec.exceptions;

import uk.ac.manchester.spinnaker.data_spec.Commands;

/**
 * An exception that indicates that the value of the requested type is unknown.
 */
public class UnknownTypeLengthException extends DataSpecificationException {
	private static final long serialVersionUID = 8012093021275095441L;

	/**
	 * Create an instance.
	 *
	 * @param dataLen
	 *            How long a set of data was requested.
	 * @param command
	 *            What command was being executed.
	 */
	public UnknownTypeLengthException(int dataLen, Commands command) {
		super("Unknown data length " + dataLen + " during command " + command);
	}
}
