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

/**
 * A special exception that indicates that a
 * {@link uk.ac.manchester.spinnaker.data_spec.Commands#BREAK BREAK} was
 * encountered.
 */
public class ExecuteBreakInstruction extends DataSpecificationException {
	private static final long serialVersionUID = -4902287652556707319L;

	/** Create an instance. */
	ExecuteBreakInstruction() {
		super("BREAK instruction reached");
	}
}
