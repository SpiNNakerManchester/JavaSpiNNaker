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

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Encapsulates exceptions from processes which communicate with some core/chip.
 */
public class ProcessException extends SpinnmanException {
	private static final long serialVersionUID = 7759365416594564702L;

	private static final String S = "     "; // five spaces

	/** Where does the code believe this exception originated? */
	public final CoreLocation core;

	/**
	 * Create an exception.
	 *
	 * @param core
	 *            What core were we talking to.
	 * @param cause
	 *            What exception caused problems.
	 */
	ProcessException(HasCoreLocation core, Throwable cause) {
		super(format(
				"when sending to %d:%d:%d, received exception: %s\n" + S
						+ "with message: %s",
				core.getX(), core.getY(), core.getP(),
				cause.getClass().getName(), cause.getMessage()), cause);
		this.core = core.asCoreLocation();
	}
}
