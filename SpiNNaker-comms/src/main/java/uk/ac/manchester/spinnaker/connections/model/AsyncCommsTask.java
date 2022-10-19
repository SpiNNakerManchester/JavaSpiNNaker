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
package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import uk.ac.manchester.spinnaker.messages.scp.CheckOKResponse;
import uk.ac.manchester.spinnaker.messages.scp.NoResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/**
 * A task for managing the sending of messages to SpiNNaker and receiving the
 * responses. This will handle most of the details of the fact that UDP allows
 * packet reordering and discarding, and these are situations that have been
 * seen for real.
 */
public interface AsyncCommsTask {
	/**
	 * Add an SCP request to the set to be sent.
	 *
	 * @param <T>
	 *            The type of response expected to the request.
	 * @param request
	 *            The SCP request to be sent
	 * @param callback
	 *            A callback function to call when the response has been
	 *            received; takes an SCPResponse as a parameter, or a
	 *            {@code null} if the response doesn't need to be processed.
	 * @param errorCallback
	 *            A callback function to call when an error is found when
	 *            processing the message; takes the original {@link SCPRequest},
	 *            and the exception caught while sending it.
	 * @throws IOException
	 *             If things go really wrong.
	 * @throws InterruptedException
	 *             If communications are interrupted (prior to sending).
	 */
	<T extends CheckOKResponse> void sendRequest(SCPRequest<T> request,
			Consumer<T> callback,
			BiConsumer<SCPRequest<?>, Throwable> errorCallback)
			throws IOException, InterruptedException;

	/**
	 * Indicate the end of the packets to be sent. This must be called to ensure
	 * that all responses are received and handled.
	 *
	 * @throws IOException
	 *             If anything goes wrong with communications.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	void finish() throws IOException, InterruptedException;

	/**
	 * Send a one-way request.
	 *
	 * @param <T>
	 *            The type of response, which must be {@link NoResponse} or a
	 *            subclass.
	 * @param request
	 *            The one-way SCP request to be sent.
	 * @throws IOException
	 *             If things go really wrong.
	 * @throws InterruptedException
	 *             If communications are interrupted (prior to sending).
	 */
	<T extends NoResponse> void sendOneWayRequest(SCPRequest<T> request)
			throws IOException, InterruptedException;
}
