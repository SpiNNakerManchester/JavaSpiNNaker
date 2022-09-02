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
package uk.ac.manchester.spinnaker.spalloc.exceptions;

import java.io.IOException;

/** Thrown when a network-level problem occurs during protocol handling. */
public class SpallocProtocolException extends IOException {
	private static final long serialVersionUID = -1591596793445886688L;

	/** @param cause The cause of this exception. */
	public SpallocProtocolException(Throwable cause) {
		super(cause);
	}

	/** @param msg The message of this exception. */
	public SpallocProtocolException(String msg) {
		super(msg);
	}
}
