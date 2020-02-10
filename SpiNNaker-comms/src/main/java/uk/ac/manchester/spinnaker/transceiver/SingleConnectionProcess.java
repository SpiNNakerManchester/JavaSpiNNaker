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
package uk.ac.manchester.spinnaker.transceiver;

import static uk.ac.manchester.spinnaker.connections.SCPRequestPipeline.SCP_TIMEOUT;

import java.io.IOException;
import java.util.function.Consumer;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SCPRequestPipeline;
import uk.ac.manchester.spinnaker.messages.scp.CheckOKResponse;
import uk.ac.manchester.spinnaker.messages.scp.NoResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/**
 * A process that uses a single connection in communication.
 *
 * @param <T>
 *            The type of connection used by this process.
 */
abstract class SingleConnectionProcess<T extends SCPConnection>
		extends Process {
	private final ConnectionSelector<T> connectionSelector;
	private SCPRequestPipeline requestPipeline;
	private final int timeout;
	private final RetryTracker retryTracker;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	protected SingleConnectionProcess(ConnectionSelector<T> connectionSelector,
			RetryTracker retryTracker) {
		this(connectionSelector, SCP_TIMEOUT, retryTracker);
	}

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param timeout
	 *            How long to take sending the message, in milliseconds.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	protected SingleConnectionProcess(ConnectionSelector<T> connectionSelector,
			int timeout, RetryTracker retryTracker) {
		this.requestPipeline = null;
		this.timeout = timeout;
		this.connectionSelector = connectionSelector;
		this.retryTracker = retryTracker;
	}

	private SCPRequestPipeline getPipeline(SCPRequest<?> request) {
		/*
		 * If no pipe line built yet, build one on the connection selected for
		 * it
		 */
		if (requestPipeline == null) {
			requestPipeline = new SCPRequestPipeline(
					connectionSelector.getNextConnection(request), timeout,
					retryTracker);
		}
		return requestPipeline;
	}

	@Override
	protected final <R extends CheckOKResponse> void sendRequest(
			SCPRequest<R> request, Consumer<R> callback) throws IOException {
		getPipeline(request).sendRequest(request, callback, this::receiveError);
	}

	@Override
	protected void sendOneWayRequest(SCPRequest<? extends NoResponse> request)
			throws IOException {
		getPipeline(request).sendOneWayRequest(request);
	}

	@Override
	protected final void finish() throws IOException {
		requestPipeline.finish();
	}
}
