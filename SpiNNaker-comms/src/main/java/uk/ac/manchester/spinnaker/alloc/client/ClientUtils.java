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

import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

/** Shared helper because we can't use a superclass. */
abstract class ClientUtils {
	private ClientUtils() {
	}

	/**
	 * Receive a message from a queue or time out.
	 *
	 * @param received
	 *            Where to receive from.
	 * @param timeout
	 *            Timeout, in milliseconds.
	 * @return The message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws SocketTimeoutException
	 *             If a timeout happens.
	 */
	static ByteBuffer receiveHelper(BlockingQueue<ByteBuffer> received,
		long timeout) throws SocketTimeoutException, InterruptedException {
		var msg = received.poll(timeout, MILLISECONDS);
		if (isNull(msg)) {
			throw new SocketTimeoutException();
		}
		return msg;
	}
}
