/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.proxy;

/**
 * Message codes used in proxy operations.
 *
 * @author Donal Fellows
 */
public enum ProxyOp {
	/**
	 * Ask for a bidirectional channel to a board to be opened. Also the
	 * response to such a request.
	 */
	OPEN,
	/**
	 * Ask for a channel (created with {@link #OPEN} or
	 * {@link #OPEN_UNCONNECTED}) to be closed. Also the response to such a
	 * request.
	 */
	CLOSE,
	/**
	 * A message going to or from a board. Channel must be open already.
	 * When going to a board, the channel must have been opened with
	 * {@link #OPEN}, and thus be already bound.
	 */
	MESSAGE,
	/**
	 * Ask for a bidirectional channel from all boards to be opened. Also
	 * the response to such a request. The difference is that this reports the
	 * real listening IP address and port in the response message. (This is
	 * closed with a {@link #CLOSE} message.) Sending is only possible on this
	 * channel with {@link #MESSAGE_TO} (because no target address is set by
	 * default).
	 */
	OPEN_UNCONNECTED,
	/**
	 * A message going to a board on a channel which does not have a SpiNNaker
	 * board target address set up already ({@link #OPEN_UNCONNECTED}).
	 */
	MESSAGE_TO
}
