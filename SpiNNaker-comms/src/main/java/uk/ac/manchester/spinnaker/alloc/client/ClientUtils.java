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

import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
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

	/**
	 * Add a {@code /} to the end of the path part of a URI.
	 *
	 * @param uri
	 *            The URI to amend. Assumed to be HTTP or HTTPS.
	 * @return The amended URI.
	 */
	static URI asDir(URI uri) {
		var path = uri.getPath();
		if (!path.endsWith("/")) {
			path += "/";
			uri = uri.resolve(path);
		}
		return uri;
	}

	/**
	 * Make a read-only shallow copy of a list.
	 *
	 * @param <T>
	 *            The type of elements in the list. These are
	 *            <em>recommended</em> to be immutable, but this is not
	 *            enforced.
	 * @param list
	 *            The list to make a read-only copy of. The elements in the list
	 *            are not themselves copied.
	 * @return The read-only copy.
	 */
	static <T> List<T> readOnlyCopy(List<T> list) {
		return Objects.isNull(list) ? List.of() : List.copyOf(list);
	}
}
