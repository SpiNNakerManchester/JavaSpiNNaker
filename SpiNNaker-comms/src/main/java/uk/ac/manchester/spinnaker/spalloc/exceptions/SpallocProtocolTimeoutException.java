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

/** Thrown upon a protocol-level timeout. */
public class SpallocProtocolTimeoutException extends IOException {
	private static final long serialVersionUID = -3573271239107837119L;

	public SpallocProtocolTimeoutException(String string, Throwable e) {
		super(string, e);
	}

	public SpallocProtocolTimeoutException(String string) {
		super(string);
	}
}
