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
