/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.proxy;

/**
 * Message codes used in proxy operations.
 *
 * @author Donal Fellows
 */
public enum ProxyOp {
	/**
	 * Ask for a bidirectional connection to a board to be opened. Also the
	 * response to such a request.
	 */
	OPEN,
	/**
	 * Ask for a bidirectional connection to a board to be closed. Also the
	 * response to such a request.
	 */
	CLOSE,
	/** A message going to or from a board. Connection must be open already. */
	MESSAGE
}
