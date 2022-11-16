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
package uk.ac.manchester.spinnaker.alloc.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * Our interface that specifies what top-level operations can be done by the UDP
 * socket proxying system. Used to hide the details of the websocket from code
 * that doesn't need to care and shouldn't care.
 */
interface ProxyProtocolClient extends AutoCloseable {
	/**
	 * Open a connected channel to a SpiNNaker board in the current job.
	 *
	 * @param chip
	 *            The coordinates of the ethernet chip of the board to connect
	 *            to.
	 * @param port
	 *            The UDP port to connect to.
	 * @param receiveQueue
	 *            Where to enqueue received messages.
	 * @return The connected channel.
	 * @throws InterruptedException
	 *             If interrupted while waiting for a reply.
	 */
	ConnectedChannel openChannel(ChipLocation chip, int port,
			BlockingQueue<ByteBuffer> receiveQueue) throws InterruptedException;

	/**
	 * Open an unconnected channel to any SpiNNaker board in the current job.
	 *
	 * @param receiveQueue
	 *            Where to enqueue received messages.
	 * @return The unconnected channel.
	 * @throws InterruptedException
	 *             If interrupted while waiting for a reply.
	 */
	UnconnectedChannel openUnconnectedChannel(
			BlockingQueue<ByteBuffer> receiveQueue) throws InterruptedException;

	/**
	 * Is the underlying websocket in the state OPEN.
	 *
	 * @return state equals ReadyState.OPEN
	 */
	boolean isOpen();

	/**
	 * {@inheritDoc}
	 * <p>
	 * Note that this may process the close asynchronously.
	 */
	@Override
	void close();

	interface ConnectedChannel extends Closeable {
		/**
		 * Send a message to the board that the channel is connected to.
		 *
		 * @param msg
		 *            The payload of the message to send. Might be a serialized
		 *            SDP message, for example.
		 * @throws IOException
		 *             If the message cannot be sent.
		 */
		void send(ByteBuffer msg) throws IOException;
	}

	interface UnconnectedChannel extends Closeable {
		/**
		 * @return The "local" address for this channel.
		 */
		Inet4Address getAddress();

		/**
		 * @return The "local" port for this channel.
		 */
		int getPort();

		/**
		 * Send a message to a board in the allocation of the current job.
		 *
		 * @param chip
		 *            Which ethernet chip to send to.
		 * @param port
		 *            Which UDP port to send to.
		 * @param msg
		 *            The payload of the message to send. Might be a serialized
		 *            SDP message, for example.
		 * @throws IOException
		 *             If the message cannot be sent.
		 */
		void send(ChipLocation chip, int port, ByteBuffer msg)
				throws IOException;
	}
}
