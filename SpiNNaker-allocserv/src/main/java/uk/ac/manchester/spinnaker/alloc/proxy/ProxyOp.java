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
	/**
	 * A message going to or from a board. Connection must be open already.
	 * When going to a board, the connection must have been opened with
	 * {@link #OPEN}, and thus be already bound.
	 */
	MESSAGE,
	/**
	 * Ask for a bidirectional connection from all boards to be opened. Also
	 * the response to such a request. The difference is that this reports the
	 * real listening IP address and port in the response message. (This is
	 * closed with a {@link #CLOSE} message.) Sending is only possible on this
	 * channel with {@link #MESSAGE_TO} (because no target address is bound by
	 * default).
	 */
	OPEN_UNBOUND,
	/**
	 * A message going to a board on a channel which does not have a SpiNNaker
	 * board target address bound already ({@link #OPEN_UNBOUND}).
	 */
	MESSAGE_TO
}
