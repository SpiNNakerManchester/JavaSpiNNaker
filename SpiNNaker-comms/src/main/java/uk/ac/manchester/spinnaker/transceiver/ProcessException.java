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
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.String.format;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Encapsulates exceptions from processes which communicate with some core/chip.
 */
public class ProcessException extends Exception {
	private static final long serialVersionUID = 7759365416594564702L;
	private static final String S = "     "; // five spaces

	/**
	 * Create an exception.
	 *
	 * @param core
	 *            What core were we talking to.
	 * @param cause
	 *            What exception caused problems.
	 */
	public ProcessException(HasCoreLocation core, Throwable cause) {
		super(format("\n" + S + "Received exception class: %s\n" + S
				+ "With message: %s\n" + S + "When sending to %d:%d:%d\n",
				cause.getClass().getName(), cause.getMessage(), core.getX(),
				core.getY(), core.getP()), cause);
	}
}
